package com.topnivo.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.topnivo.backend.exception.exception.*;
import com.topnivo.backend.model.constant.AdminSettings;
import com.topnivo.backend.model.constant.TransactionStatus;
import com.topnivo.backend.model.constant.TransactionType;
import com.topnivo.backend.model.constant.TransferStatus;
import com.topnivo.backend.model.entity.AdminSetting;
import com.topnivo.backend.model.entity.Member;
import com.topnivo.backend.model.entity.Transaction;
import com.topnivo.backend.model.entity.TransferRecord;
import com.topnivo.backend.repository.AdminSettingRepository;
import com.topnivo.backend.repository.MemberRepository;
import com.topnivo.backend.repository.TransactionRepository;
import com.topnivo.backend.repository.TransferRecordRepository;
import com.topnivo.backend.util.TransactionUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    @Value("${paystack.url.banks}")
    private String paystackBanksUrl;
    @Value("${paystack.url.balance}")
    private String paystackBalanceUrl;
    @Value("${paystack.url.transactionVerify:https://api.paystack.co/transaction/verify}")
    private String paystackTransactionVerifyUrl;
    @Value("${paystack.url.transferVerify:https://api.paystack.co/transfer/verify}")
    private String paystackTransferVerifyUrl;
    @Value("${paystack.url.transferBulk}")
    private String paystackTransferBulkUrl;
    @Value("${paystack.url.transferRecipient}")
    private String paystackTransferRecipientUrl;
    @Value("${paystack.privateKey}")
    private String paystackPrivateKey;
    @Value("${paystack.verifyPayment:false}")
    private boolean paystackVerifyPayment;


    private final RestTemplate restTemplate;
    private final MemberRepository memberRepository;
    private final AdminSettingRepository adminSettingRepository;
    private final ObjectMapper objectMapper;
    private final TransferRecordRepository transferRecordRepository;
    private final TransactionRepository transactionRepository;

    public double checkPaymentValidity(String transactionReference) {
        if (transactionReference == null || transactionReference.isBlank()) {
            throw new InvalidPaymentException(ErrorMessages.INVALID_PAYMENT);
        }
        if (!paystackVerifyPayment) {
            log.warn("Paystack remote verification is disabled. Accepting payment reference {} for local/dev order creation.",
                    transactionReference);
            return Double.NaN;
        }
        if (paystackPrivateKey == null || paystackPrivateKey.isBlank() || !paystackPrivateKey.startsWith("sk_")) {
            throw new InvalidPaymentException(ErrorMessages.PAYSTACK_SECRET_KEY_NOT_CONFIGURED);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + paystackPrivateKey);
        HttpEntity<String> httpEntity = new HttpEntity<>(headers);

        String url = paystackTransactionVerifyUrl + "/" + transactionReference;
        double amount = 0;
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, httpEntity, String.class);
            Map<String, Object> responseMapTransferRecipient = objectMapper.readValue(response.getBody(), Map.class);
            Map<String, Object> dataPayment = (Map<String, Object>) responseMapTransferRecipient.get("data");
            boolean responseTransferRecipientStatus = (boolean) responseMapTransferRecipient.get("status");
            String paymentStatus = (String) dataPayment.get("status");
            String verifiedReference = (String) dataPayment.get("reference");
            Number amountPaid = (Number) dataPayment.get("amount");

            if (responseTransferRecipientStatus && paymentStatus.equals("success") && transactionReference.equals(verifiedReference)) {
                amount = amountPaid.doubleValue() / 100.0;
            }
            else {
                throw new InvalidPaymentException(ErrorMessages.INVALID_PAYMENT);
            }
        }
        catch (InvalidPaymentException exception) {
            throw exception;
        }
        catch (RestClientResponseException exception) {
            log.warn("Paystack rejected transaction reference {} with status {} and body {}",
                    transactionReference,
                    exception.getStatusCode(),
                    exception.getResponseBodyAsString());
            throw new InvalidPaymentException(ErrorMessages.INVALID_PAYMENT);
        }
        catch (Exception exception) {
            log.warn("Could not verify Paystack transaction reference {}", transactionReference, exception);
            throw new InvalidPaymentException(ErrorMessages.INVALID_PAYMENT);
        }

        return amount;
    }

    public List<Map<String, String>> findAllBanks() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + paystackPrivateKey);
        HttpEntity<String> httpEntity = new HttpEntity<>(headers);
        String url = paystackBanksUrl;

        List<Map<String, String>> returnValue = null;
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, httpEntity, String.class);
            returnValue = extractBankData(response.getBody());
        } catch (Exception exception) {
            throw new NoSuchResourceException(ErrorMessages.UNKNOWN_ERROR);
        }

        return returnValue;
    }

    public List<TransferRecord> getPayroll() {
        validatePendingPayroll();

        return transferRecordRepository.findByStatusIn(List.of(TransferStatus.PENDING, TransferStatus.INITIALIZED));
    }

    public void validatePendingPayroll() {
        List<TransferRecord> transferRecords = transferRecordRepository.findByStatus(TransferStatus.PENDING);

        transferRecords.forEach(record -> {
            isTransferSuccessful(record);
        });
    }

    public void deleteInitializedPayroll() {
        List<TransferRecord> transferRecords = transferRecordRepository.findByStatus(TransferStatus.INITIALIZED);
        transferRecordRepository.deleteAll(transferRecords);

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

                HttpHeaders headers = new HttpHeaders();
                headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + paystackPrivateKey);

                Map<String, String> recipientPayload = new HashMap<>();
                recipientPayload.put("type", member.getAccountDetails().getBankType());
                recipientPayload.put("name", member.getLastName() + " " + member.getFirstName());
                recipientPayload.put("account_number", member.getAccountDetails().getAccountNumber());
                recipientPayload.put("bank_code", member.getAccountDetails().getBankCode());
                recipientPayload.put("currency", member.getAccountDetails().getCurrency());

                HttpEntity<Map<String, String>> httpEntity = new HttpEntity<>(recipientPayload, headers);

                // create transfer recipient
                try {
                    ResponseEntity<String> responseTransferRecipient =
                            restTemplate.exchange(paystackTransferRecipientUrl, HttpMethod.POST, httpEntity, String.class);
                    Map<String, Object> responseMapTransferRecipient = objectMapper.readValue(responseTransferRecipient.getBody(), Map.class);
                    Map<String, Object> dataTransferRecipient = (Map<String, Object>) responseMapTransferRecipient.get("data");
                    boolean responseTransferRecipientStatus = (boolean) responseMapTransferRecipient.get("status");
                    String recipientCode = (String) dataTransferRecipient.get("recipient_code");
                    boolean responseActive = (boolean) dataTransferRecipient.get("active");

                    if (responseTransferRecipientStatus && responseActive && !Objects.isNull(recipientCode)) {
                        transferRecord.setRecipientCode(recipientCode);
                        transferRecords.add(transferRecord);
                    }
                }
                catch (Exception ignored) {
                    log.info("Cound not send money for: " + member.getUsername());
                }
            }
        }

        return transferRecordRepository.saveAll(transferRecords);
    }

    public void sendPayroll() {
        checkIfAdminIsValid();

        List<TransferRecord> transferRecords = transferRecordRepository.findByStatus(TransferStatus.INITIALIZED);
        double totalPayout = transferRecords.stream().mapToDouble(t -> t.getMember().getAvailableBalance()).sum();

        HttpHeaders headersBalance = new HttpHeaders();
        headersBalance.set(HttpHeaders.AUTHORIZATION, "Bearer " + paystackPrivateKey);
        HttpEntity<Map<String, String>> httpEntityBalance = new HttpEntity<>(headersBalance);
        try {
            ResponseEntity<String> responseBalance =
                    restTemplate.exchange(paystackBalanceUrl, HttpMethod.GET, httpEntityBalance, String.class);
            Map<String, Object> responseMapBalance = objectMapper.readValue(responseBalance.getBody(), Map.class);
            List<Map<String, Object>> dataBalance = (List<Map<String, Object>>) responseMapBalance.get("data");
            double balance = ((int) dataBalance.get(0).get("balance") * 100.0) / 100;

            if (balance > totalPayout * 100) {
                /*
                 * PayStack bulk transfer (batch) must not be greater than 100 units at once and in every 5s
                 */
                int totalRecords = transferRecords.size();
                int batchSize = 100;

                for (int batchStart = 0; batchStart < totalRecords; batchStart += batchSize) {
                    int batchEnd = Math.min(batchStart + batchSize, totalRecords);
                    List<Map<String, Object>> payrollList = new ArrayList<>();

                    for (int i = batchStart; i < batchEnd; i++) {
                        // 100NGN means 100 * 100
                        TransferRecord transferRecord = transferRecords.get(i);
                        Map<String, Object> singleTransferPayload = new HashMap<>();
                        singleTransferPayload.put("amount", transferRecord.getAmount() * 100);
                        singleTransferPayload.put("reference", transferRecord.getReference());
                        singleTransferPayload.put("reason", transferRecord.getReason());
                        singleTransferPayload.put("recipient", transferRecord.getRecipientCode());

                        // check if the Recipient Code is not null
                        if (!Objects.isNull(transferRecord.getRecipientCode())) {
                            payrollList.add(singleTransferPayload);
                        }
                        else {
                            transferRecordRepository.deleteById(transferRecord.getId());
                        }
                    }

                    Map<String, Object> transfersPayload = new HashMap<>();
                    transfersPayload.put("currency", "NGN");
                    transfersPayload.put("source", "balance");
                    transfersPayload.put("transfers", payrollList);

                    // Send request to batch transfer
                    HttpHeaders headers = new HttpHeaders();
                    headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + paystackPrivateKey);
                    HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(transfersPayload, headers);

                    try {
                        restTemplate.exchange(paystackTransferBulkUrl, HttpMethod.POST, httpEntity, String.class);

                    } catch (Exception ignored) {

                    }

                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            transferRecords.forEach(record -> {
                record.setStatus(TransferStatus.PENDING);
            });

            transferRecordRepository.saveAll(transferRecords);
        }
        catch (Exception ex) {
            throw new BadRequestException(ErrorMessages.INSUFFICIENT_FUNDS_FOR_PAYROLL);
        }
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
                stringMap.put("type", (String) bank.get("type"));
                stringMap.put("currency", (String) bank.get("currency"));

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

    private void isTransferSuccessful(TransferRecord transferRecord) {
        if (transferRecord == null) {
            return;
        }

        HttpHeaders headersVerify = new HttpHeaders();
        headersVerify.set(HttpHeaders.AUTHORIZATION, "Bearer " + paystackPrivateKey);
        HttpEntity<Map<String, String>> httpEntityVerify = new HttpEntity<>(headersVerify);

        try {
            ResponseEntity<String> responseVerify =
                    restTemplate.exchange(paystackTransferVerifyUrl + "/" + transferRecord.getReference(), HttpMethod.GET, httpEntityVerify, String.class);
            Map<String, Object> responseMapVerify = objectMapper.readValue(responseVerify.getBody(), Map.class);
            boolean responseStatus = (boolean) responseMapVerify.get("status");
            List<Map<String, Object>> dataMap = (List<Map<String, Object>>) responseMapVerify.get("data");
            Map<String, Object> dataVerify =  dataMap.get(0);
            String successVerify = (String) dataVerify.get("status");

            Member member = transferRecord.getMember();

            Transaction transaction = new Transaction();
            transaction.setReferenceId(transferRecord.getReference());
            transaction.setMemberId(member.getMemberId());
            transaction.setAmount(transferRecord.getAmount());
            transaction.setMessage(transferRecord.getReason());
            transaction.setTransactionDate(LocalDateTime.now());
            transaction.setType(TransactionType.WITHDRAWAL);

            if (responseStatus && successVerify.equals("success")) {
                // Reset availableBalance = 0
                member.setAvailableBalance(0.0);
                transferRecord.setStatus(TransferStatus.COMPLETED);
                transferRecordRepository.save(transferRecord);


                transaction.setStatus(TransactionStatus.COMPLETED);

                transactionRepository.save(transaction);
                memberRepository.save(member);
            }
            else {
                transferRecord.setStatus(TransferStatus.DECLINED);
                transaction.setStatus(TransactionStatus.DECLINED);

                transactionRepository.save(transaction);
                transferRecordRepository.save(transferRecord);
            }

        } catch (Exception ignored) {

        }
    }
}
