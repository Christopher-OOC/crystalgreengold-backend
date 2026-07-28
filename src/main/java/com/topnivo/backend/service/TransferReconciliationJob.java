package com.topnivo.backend.service;

import com.topnivo.backend.model.constant.TransferStatus;
import com.topnivo.backend.model.entity.TransferRecord;
import com.topnivo.backend.repository.TransferRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransferReconciliationJob {

    private static final String AFRICA_LAGOS_TIMEZONE = "Africa/Lagos";

    private final TransferRecordRepository transferRecordRepository;
    private final FlutterwavePaymentService flutterwavePaymentService;

    @Async
    @Scheduled(cron = "0 * * * * ?", zone = AFRICA_LAGOS_TIMEZONE)
    public void reconcilePendingTransfers() {
        List<TransferRecord> pendingRecords = transferRecordRepository.findByStatus(TransferStatus.PENDING);

        if (pendingRecords.isEmpty()) {
            return;
        }

        log.info("Transfer reconciliation job found {} pending transfer records", pendingRecords.size());

        for (TransferRecord transferRecord : pendingRecords) {
            if (flutterwavePaymentService.isTransferSuccessful(transferRecord)) {
                transferRecord.setStatus(TransferStatus.COMPLETED);

                transferRecordRepository.save(transferRecord);
            }
        }
    }
}
