package com.topnivo.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.topnivo.backend.exception.exception.*;
import com.topnivo.backend.model.constant.*;
import com.topnivo.backend.model.entity.AdminSetting;
import com.topnivo.backend.model.entity.Member;
import com.topnivo.backend.model.entity.Transaction;
import com.topnivo.backend.model.entity.TransferRecord;
import com.topnivo.backend.repository.AdminSettingRepository;
import com.topnivo.backend.repository.MemberRepository;
import com.topnivo.backend.repository.TransactionRepository;
import com.topnivo.backend.repository.TransferRecordRepository;
import com.topnivo.backend.util.PasswordUtils;
import com.topnivo.backend.util.TransactionUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FlutterwavePaymentService {

    @Value("${flutterwave.clientId:}")
    private String flwClientId;
    @Value("${flutterwave.clientSecret:}")
    private String flwClientSecret;
    @Value("${flutterwave.v3SecretKey:}")
    private String flwV3SecretKey;
    @Value("${flutterwave.verifyPayment:false}")
    private boolean flwVerifyPayment;

    @Value("${flutterwave.url.token:https://idp.flutterwave.com/realms/flutterwave/protocol/openid-connect/token}")
    private String flwTokenUrl;
    @Value("${flutterwave.url.banks:https://f4bexperience.flutterwave.com/banks}")
    private String flwBanksUrl;
    @Value("${flutterwave.url.transactionVerifyV3:https://api.flutterwave.com/v3/transactions/{refId}/verify}")
    private String flwTransactionVerifyUrl;
    @Value("${flutterwave.url.walletBalance:https://f4bexperience.flutterwave.com/wallets/balances}")
    private String flwWalletBalanceUrl;
    @Value("${flutterwave.url.directTransfer:https://f4bexperience.flutterwave.com/direct-transfers}")
    private String flwDirectTransferUrl;
    @Value("${flutterwave.url.transferGet:https://f4bexperience.flutterwave.com/transfers}")
    private String flwTransferGetUrl;

    private final RestTemplate restTemplate;
    private final MemberRepository memberRepository;
    private final AdminSettingRepository adminSettingRepository;
    private final ObjectMapper objectMapper;
    private final TransferRecordRepository transferRecordRepository;
    private final TransactionRepository transactionRepository;

    private volatile String cachedAccessToken;
    private volatile long tokenExpiryTime = 0;

    private synchronized String getAccessToken() {
        long now = System.currentTimeMillis();
        if (cachedAccessToken != null && now < tokenExpiryTime) {
            return cachedAccessToken;
        }

        if (flwClientId == null || flwClientId.isBlank() || flwClientSecret == null || flwClientSecret.isBlank()) {
            throw new InvalidPaymentException(ErrorMessages.FLW_CREDENTIALS_NOT_CONFIGURED);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", flwClientId);
        body.add("client_secret", flwClientSecret);
        body.add("grant_type", "client_credentials");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(flwTokenUrl, HttpMethod.POST, request, String.class);
            Map<String, Object> tokenResponse = objectMapper.readValue(response.getBody(), Map.class);

            cachedAccessToken = (String) tokenResponse.get("access_token");
            int expiresIn = (int) tokenResponse.getOrDefault("expires_in", 600);
            tokenExpiryTime = now + ((expiresIn - 60) * 1000L);

            log.info("Flutterwave OAuth2 access token obtained successfully, expires in {}s", expiresIn);
            return cachedAccessToken;
        } catch (Exception e) {
            log.error("Failed to obtain Flutterwave OAuth2 access token", e);
            throw new InvalidPaymentException(ErrorMessages.FLW_OAUTH_TOKEN_FAILED);
        }
    }

    public double checkPaymentValidity(String transactionReference) {
        log.info("Checking Flutterwave Payment Validity for reference {}...", transactionReference);
        if (transactionReference == null || transactionReference.isBlank()) {
            throw new InvalidPaymentException(ErrorMessages.FLW_INVALID_PAYMENT);
        }
        if (!flwVerifyPayment) {
            log.warn("Flutterwave remote verification is disabled. Accepting payment reference {} for local/dev order creation.",
                    transactionReference);
            return Double.NaN;
        }
        if (flwV3SecretKey == null || flwV3SecretKey.isBlank()) {
            throw new InvalidPaymentException(ErrorMessages.FLW_CREDENTIALS_NOT_CONFIGURED);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + flwV3SecretKey);
        HttpEntity<String> httpEntity = new HttpEntity<>(headers);

        String url = flwTransactionVerifyUrl.replace("{refId}", transactionReference);
        double amount = 0;
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, httpEntity, String.class);
            Map<String, Object> responseMap = objectMapper.readValue(response.getBody(), Map.class);
            String responseStatus = (String) responseMap.get("status");

            if (!"success".equalsIgnoreCase(responseStatus)) {
                throw new InvalidPaymentException(ErrorMessages.FLW_INVALID_PAYMENT);
            }

            Map<String, Object> data = (Map<String, Object>) responseMap.get("data");
            String verifiedStatus = (String) data.get("status");
            String verifiedReference = (String) data.get("tx_ref");
            Number amountPaid = (Number) data.get("amount");

            if ("successful".equalsIgnoreCase(verifiedStatus) && transactionReference.equals(verifiedReference)) {
                amount = amountPaid.doubleValue();
            } else {
                throw new InvalidPaymentException(ErrorMessages.FLW_INVALID_PAYMENT);
            }
        } catch (InvalidPaymentException exception) {
            throw exception;
        } catch (RestClientResponseException exception) {
            log.warn("Flutterwave rejected transaction reference {} with status {} and body {}",
                    transactionReference,
                    exception.getStatusCode(),
                    exception.getResponseBodyAsString());
            throw new InvalidPaymentException(ErrorMessages.FLW_INVALID_PAYMENT);
        } catch (Exception exception) {
            log.warn("Could not verify Flutterwave transaction reference {}", transactionReference, exception);
            throw new InvalidPaymentException(ErrorMessages.FLW_INVALID_PAYMENT);
        }

        log.info("Confirmed Flutterwave amount of {}...", amount);
        return amount;
    }

    public List<Map<String, String>> findAllBanks() {
        String accessToken = getAccessToken();

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
        HttpEntity<String> httpEntity = new HttpEntity<>(headers);
        String url = flwBanksUrl + "?country=NG";

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, httpEntity, String.class);
            return extractBankData(response.getBody());
        } catch (Exception exception) {
            throw new NoSuchResourceException(ErrorMessages.UNKNOWN_ERROR);
        }
    }

    public List<TransferRecord> getPayroll() {
        return transferRecordRepository.findByStatusIn(List.of(TransferStatus.INITIALIZED, TransferStatus.PENDING));
    }

    public List<TransferRecord> getPendingPayroll() {
        return transferRecordRepository.findByStatusIn(List.of(TransferStatus.PENDING));
    }

    public void deleteAPayrollEntry(int id) {
        checkIfAdminIsValid();
        transferRecordRepository.deleteById(id);
    }

    public List<TransferRecord> preparePayroll() {
        checkIfAdminIsValid();

        List<TransferRecord> records = transferRecordRepository.findByStatus(TransferStatus.INITIALIZED);
        transferRecordRepository.deleteAll(records);

        AdminSetting setting = adminSettingRepository.findByName(AdminSettings.MINIMUM_WITHDRAWAL.name());
        List<Member> eligibleMembers = memberRepository.findByAvailableBalanceIsGreaterThanEqual(setting.getValue());
        List<TransferRecord> transferRecords = new ArrayList<>();

        for (Member member : eligibleMembers) {
            if (member.isEnabled() && member.getAccountDetails() != null && member.getCurrentPackage() != null && member.isCanReceivePayment()) {
                TransferRecord transferRecord = new TransferRecord();
                transferRecord.setReason("Topnivo has credited you " + member.getAvailableBalance());
                transferRecord.setReference(TransactionUtils.generateReferenceId());
                transferRecord.setMember(member);
                transferRecord.setAmount(member.getAvailableBalance());
                transferRecord.setStatus(TransferStatus.INITIALIZED);
                transferRecord.setType(TransferType.PAYOUT);
                transferRecords.add(transferRecord);
            }
        }

        return transferRecordRepository.saveAll(transferRecords);
    }

    public void sendPayroll() {
        List<TransferRecord> transferRecords = transferRecordRepository.findByStatus(TransferStatus.INITIALIZED);
        transferRecords.forEach((record) -> record.getMember().setAvailableBalance(0.0));
        List<Member> members = transferRecords.stream().map(TransferRecord::getMember).collect(Collectors.toList());
        memberRepository.saveAll(members);

        double totalPayout = transferRecords.stream().mapToDouble(TransferRecord::getAmount).sum();

        String accessToken = getAccessToken();

        HttpHeaders headersBalance = new HttpHeaders();
        headersBalance.set(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
        headersBalance.set("X-Trace-Id", PasswordUtils.generateNumbers(16));
        HttpEntity<String> httpEntityBalance = new HttpEntity<>(headersBalance);

        try {
            ResponseEntity<String> responseBalance =
                    restTemplate.exchange(flwWalletBalanceUrl + "/NGN", HttpMethod.GET, httpEntityBalance, String.class);
            Map<String, Object> responseMapBalance = objectMapper.readValue(responseBalance.getBody(), Map.class);
            Map<String, Object> balanceData = (Map<String, Object>) responseMapBalance.get("data");
            double balance = ((Number) balanceData.get("available_balance")).doubleValue();
            String balanceStatus = (String) responseMapBalance.get("status");

            if (!"success".equalsIgnoreCase(balanceStatus)) {
                throw new BadRequestException(ErrorMessages.FLW_UNKNOWN_ERROR);
            }
            if (totalPayout > balance) {
                throw new BadRequestException(ErrorMessages.FLW_INSUFFICIENT_FUNDS_FOR_PAYROLL);
            }

            log.info("About to make the payouts...");

            ExecutorService executorService = Executors.newFixedThreadPool(20);
            try {
                List<Future<?>> futures = new ArrayList<>();

                for (TransferRecord transferRecord : transferRecords) {
                    futures.add(executorService.submit(() -> {
                        try {
                            HttpHeaders headers = new HttpHeaders();
                            headers.setBearerAuth(accessToken);
                            headers.setContentType(MediaType.APPLICATION_JSON);
                            headers.set("X-Trace-Id", PasswordUtils.generateNumbers(16));
                            headers.set("X-Idempotency-Key", PasswordUtils.generateNumbers(16));

                            String referenceId = UUID.randomUUID().toString();

                            HttpEntity<Map<String, Object>> entity =
                                    new HttpEntity<>(buildTransferPayload(transferRecord, referenceId), headers);

                            ResponseEntity<String> response = restTemplate.exchange(
                                    flwDirectTransferUrl,
                                    HttpMethod.POST,
                                    entity,
                                    String.class
                            );

                            transferRecord.setStatus(TransferStatus.PENDING);
                            transferRecord.setReference(referenceId);

                            Map<String, Object> responseMap = objectMapper.readValue(response.getBody(), Map.class);
                            String responseStatus = (String) responseMap.get("status");

                            if ("success".equalsIgnoreCase(responseStatus)) {
                                Map<String, Object> data = (Map<String, Object>) responseMap.get("data");
                                String transferStatus = (String) data.get("status");
                                String transferId = (String) data.get("id");

                                log.info("Flutterwave transfer ID gotten: {}", transferId);

                                if (transferStatus.equals("SUCCESSFUL")) {
                                    transferRecord.setStatus(TransferStatus.COMPLETED);
                                }
                                else if (transferStatus.equals("PENDING") || transferStatus.equals("NEW") || transferStatus.equals("INITIATED")) {
                                    transferRecord.setStatus(TransferStatus.PENDING);
                                } else {
                                    transferRecord.getMember().setAvailableBalance(transferRecord.getMember().getAvailableBalance() + transferRecord.getAmount());
                                    transferRecord.setStatus(TransferStatus.FAILED);
                                }

                                transferRecord.setTransactionId(transferId);
                            }
                            else {
                                transferRecord.getMember().setAvailableBalance(transferRecord.getMember().getAvailableBalance() + transferRecord.getAmount());
                                transferRecord.setStatus(TransferStatus.FAILED);
                            }
                        } catch (Exception ex) {
                            log.error("Transfer failed for {}", transferRecord.getMember().getAccountDetails().getAccountName(), ex);
                            transferRecord.getMember().setAvailableBalance(transferRecord.getMember().getAvailableBalance() + transferRecord.getAmount());
                            transferRecord.setStatus(TransferStatus.FAILED);
                        }
                        finally {
                            transferRecordRepository.save(transferRecord);
                            memberRepository.save(transferRecord.getMember());
                        }
                    }));
                }
            }
            finally {
                executorService.shutdown();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Map<String, Object> buildTransferPayload(TransferRecord transferRecord, String referenceId) {

        Member member = transferRecord.getMember();

        Map<String, Object> payload = new HashMap<>();
        payload.put("action", "instant");
        payload.put("type", "bank");
        payload.put("reference", referenceId);
        payload.put("narration", transferRecord.getReason());

        Map<String, Object> paymentInstruction = new HashMap<>();
        paymentInstruction.put("source_currency", "NGN");
        paymentInstruction.put("destination_currency", "NGN");

        Map<String, Object> amount = new HashMap<>();
        amount.put("value", transferRecord.getAmount());
        amount.put("applies_to", "destination_currency");

        paymentInstruction.put("amount", amount);

        Map<String, Object> recipient = new HashMap<>();

        Map<String, Object> bank = new HashMap<>();
        bank.put("code", member.getAccountDetails().getBankCode());
        bank.put("account_number", member.getAccountDetails().getAccountNumber());

        recipient.put("bank", bank);

        Map<String, Object> name = new HashMap<>();
        name.put("first", member.getFirstName());
        name.put("last", member.getLastName());

        recipient.put("name", name);

        if (member.getEmail() != null) {
            recipient.put("email", member.getEmail());
        }

        paymentInstruction.put("recipient", recipient);

        payload.put("payment_instruction", paymentInstruction);

        return payload;
    }

    @Transactional
    public void sendMoneyToStoreOwner(Member store, double amount) {
        if (store == null) {
            Member admin = memberRepository.findByUsernameIgnoreCase("admin");
            if (admin.getAccountDetails() != null) {
                log.info("About to send Flutterwave money to admin details: {}", admin.getAccountDetails());
                transferToStoreOwner(admin, amount);
                log.info("Successful Flutterwave transfer to admin details: {}", admin.getAccountDetails());
            }
        } else {
            if (store.getAccountDetails() != null) {
                try {
                    log.info("About to send Flutterwave money to store details: {}", store.getAccountDetails());
                    transferToStoreOwner(store, amount);
                    log.info("Successful Flutterwave transfer to store details: {}", store.getAccountDetails());
                } catch (Exception ex) {
                    store.setAvailableBalance(store.getAvailableBalance() + amount);
                    memberRepository.save(store);
                }
            } else {
                store.setAvailableBalance(store.getAvailableBalance() + amount);
                memberRepository.save(store);
            }
        }
    }

    private void transferToStoreOwner(Member store, double amount) {
        if (flwVerifyPayment) {
            String accessToken = getAccessToken();

            TransferRecord transferRecord = new TransferRecord();
            transferRecord.setReason("You have a Topnivo purchase of " + amount);
            transferRecord.setReference(TransactionUtils.generateReferenceId());
            transferRecord.setMember(store);
            transferRecord.setAmount(amount);
            transferRecord.setStatus(TransferStatus.INITIALIZED);
            transferRecord.setType(TransferType.PURCHASE);

            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Trace-Id", PasswordUtils.generateNumbers(16));
            headers.set("X-Idempotency-Key", PasswordUtils.generateNumbers(16));

            String referenceId = UUID.randomUUID().toString();

            HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(buildTransferPayload(transferRecord, referenceId), headers);

            try {
                ResponseEntity<String> responseEntity =
                        restTemplate.exchange(flwDirectTransferUrl, HttpMethod.POST, httpEntity, String.class);
                Map<String, Object> responseMap = objectMapper.readValue(responseEntity.getBody(), Map.class);
                String responseStatus = (String) responseMap.get("status");

                if ("success".equalsIgnoreCase(responseStatus)) {
                    Map<String, Object> data = (Map<String, Object>) responseMap.get("data");
                    String transferStatus = (String) data.get("status");
                    String transferId = (String) data.get("id");

                    log.info("Flutterwave transfer ID gotten: {}", transferId);

                    if (transferStatus.equals("SUCCESSFUL")) {
                        transferRecord.setStatus(TransferStatus.COMPLETED);
                    }
                    else if (transferStatus.equals("PENDING") || transferStatus.equals("NEW") || transferStatus.equals("INITIATED")) {
                        transferRecord.setStatus(TransferStatus.PENDING);
                    } else {
                        transferRecord.getMember().setAvailableBalance(transferRecord.getMember().getAvailableBalance() + transferRecord.getAmount());
                        transferRecord.setStatus(TransferStatus.FAILED);

                        createTransactionIfPurchaseTransferNotSuccessful(transferRecord, TransactionStatus.DECLINED);
                    }

                    transferRecord.setTransactionId(transferId);
                }
                else {
                    log.info("Could not send Flutterwave money for: {}", store.getUsername());
                    transferRecord.getMember().setAvailableBalance(transferRecord.getMember().getAvailableBalance() + transferRecord.getAmount());
                    transferRecord.setStatus(TransferStatus.FAILED);

                    createTransactionIfPurchaseTransferNotSuccessful(transferRecord, TransactionStatus.DECLINED);
                }
            } catch (Exception ex) {
                log.info("Could not send Flutterwave money for: {}", store.getUsername());
                transferRecord.getMember().setAvailableBalance(transferRecord.getMember().getAvailableBalance() + transferRecord.getAmount());
                transferRecord.setStatus(TransferStatus.FAILED);

                createTransactionIfPurchaseTransferNotSuccessful(transferRecord, TransactionStatus.DECLINED);
            }
            finally {
                transferRecordRepository.save(transferRecord);
                memberRepository.save(transferRecord.getMember());
            }
        }
    }

    @Async
    private void createTransactionIfPurchaseTransferNotSuccessful(TransferRecord transferRecord, TransactionStatus status) {
        Transaction transaction = new Transaction();

        if (transferRecord.getType().equals(TransferType.PAYOUT)) {
            transaction.setType(TransactionType.WITHDRAWAL);
        }
        else if (transferRecord.getType().equals(TransferType.PURCHASE)) {
            transaction.setType(TransactionType.INCOMING_PURCHASE);
        }
        else {
            transaction.setType(TransactionType.INTERNAL_TRANSFER);
        }

        if (status.equals(TransactionStatus.COMPLETED)) {
            transaction.setMessage(String.format("You received this because the transfer of %s naira was successful.",  transferRecord.getAmount()));
        }
        else if (status.equals(TransactionStatus.DECLINED)) {
            transaction.setMessage(String.format("You received this because the purchase transfer of %s was not successful.", transferRecord.getAmount()));
        }

        transaction.setAmount(transferRecord.getAmount());
        transaction.setMemberId(transferRecord.getMember().getMemberId());
        transaction.setStatus(status);
        transaction.setTransactionDate(LocalDateTime.now());
        transaction.setReferenceId(transferRecord.getReference());

        transactionRepository.save(transaction);
    }

    private List<Map<String, String>> extractBankData(String jsonResponse) {
        List<Map<String, String>> result = new ArrayList<>();

        try {
            Map<String, Object> responseMap = objectMapper.readValue(jsonResponse, Map.class);
            List<Map<String, Object>> banks = (List<Map<String, Object>>) responseMap.get("data");

            for (Map<String, Object> bank : banks) {
                Map<String, String> stringMap = new HashMap<>();
                stringMap.put("name", (String) bank.get("name"));
                stringMap.put("code", (String) bank.get("code"));
                result.add(stringMap);
            }
        } catch (Exception e) {
            throw new NoSuchResourceException(ErrorMessages.UNKNOWN_ERROR);
        }

        return result;
    }

    private void checkIfAdminIsValid() {
        var authorities = SecurityContextHolder.getContext().getAuthentication().getAuthorities();
        boolean isAdminSuperAdmin = authorities.stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN"));
        String adminUserName = SecurityContextHolder.getContext().getAuthentication().getName();
        Member admin = memberRepository.findByUsername(adminUserName);
        if (admin == null) {
            throw new NoSuchResourceException(ErrorMessages.NO_SUCH_MEMBER);
        }

        if (!isAdminSuperAdmin && admin.getSponsor() == null && admin.getPlacer() == null) {
            throw new BadRequestException(ErrorMessages.NO_SPONSOR_AND_PLACER);
        }
    }

    public TransferStatus getTransferStatus(TransferRecord transferRecord) {
        TransferStatus status = transferRecord.getStatus();

        if (transferRecord.getTransactionId() == null) {
            return status;
        }

        String accessToken = getAccessToken();

        HttpHeaders headersVerify = new HttpHeaders();
        headersVerify.set(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
        headersVerify.set("X-Trace-Id", PasswordUtils.generateNumbers(16));
        HttpEntity<String> httpEntityVerify = new HttpEntity<>(headersVerify);

        try {
            ResponseEntity<String> responseVerify =
                    restTemplate.exchange(flwTransferGetUrl + "/" + transferRecord.getTransactionId(),
                            HttpMethod.GET, httpEntityVerify, String.class);
            Map<String, Object> responseMapVerify = objectMapper.readValue(responseVerify.getBody(), Map.class);
            String responseStatus = (String) responseMapVerify.get("status");

            if ("success".equalsIgnoreCase(responseStatus)) {
                Map<String, Object> data = (Map<String, Object>) responseMapVerify.get("data");
                String transferStatus = (String) data.get("status");

                if ("SUCCESSFUL".equalsIgnoreCase(transferStatus)) {
                    status = TransferStatus.COMPLETED;
                }
                else if  ("FAILED".equalsIgnoreCase(transferStatus)) {
                    status = TransferStatus.FAILED;
                }
            }
        } catch (Exception ignored) {
            status = TransferStatus.PENDING;
        }

        return status;
    }
}
