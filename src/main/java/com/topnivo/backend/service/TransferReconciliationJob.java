package com.topnivo.backend.service;

import com.topnivo.backend.model.constant.TransactionStatus;
import com.topnivo.backend.model.constant.TransactionType;
import com.topnivo.backend.model.constant.TransferStatus;
import com.topnivo.backend.model.constant.TransferType;
import com.topnivo.backend.model.entity.Transaction;
import com.topnivo.backend.model.entity.TransferRecord;
import com.topnivo.backend.repository.MemberRepository;
import com.topnivo.backend.repository.TransactionRepository;
import com.topnivo.backend.repository.TransferRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransferReconciliationJob {

    private static final String AFRICA_LAGOS_TIMEZONE = "Africa/Lagos";

    private final TransferRecordRepository transferRecordRepository;
    private final FlutterwavePaymentService flutterwavePaymentService;
    private final MemberRepository memberRepository;
    private final TransactionRepository transactionRepository;

    @Async
    @Scheduled(cron = "0 */1 * * * ?", zone = AFRICA_LAGOS_TIMEZONE)
    public void reconcilePendingTransfers() {
        log.info("About to Run reconciliation job for pending transfer records...");

        List<TransferRecord> pendingRecords = transferRecordRepository.findByStatus(TransferStatus.PENDING);

        if (pendingRecords.isEmpty()) {
            return;
        }

        log.info("Transfer reconciliation job found {} pending transfer records", pendingRecords.size());

        for (TransferRecord transferRecord : pendingRecords) {
            TransferStatus status = flutterwavePaymentService.getTransferStatus(transferRecord);
            if (TransferStatus.COMPLETED.equals(status)) {
                transferRecord.setStatus(TransferStatus.COMPLETED);

                transferRecordRepository.save(transferRecord);

                createTransactionIfPurchaseTransferNotSuccessful(transferRecord, TransactionStatus.COMPLETED);
            } else if (TransferStatus.FAILED.equals(status)) {
                transferRecord.setStatus(TransferStatus.FAILED);
                transferRecord.getMember().setAvailableBalance(transferRecord.getMember().getAvailableBalance() + transferRecord.getAmount());

                transferRecordRepository.save(transferRecord);
                memberRepository.save(transferRecord.getMember());

                createTransactionIfPurchaseTransferNotSuccessful(transferRecord, TransactionStatus.DECLINED);
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
}
