package com.topnivo.backend.service;

import com.topnivo.backend.model.constant.TransferStatus;
import com.topnivo.backend.model.entity.TransferRecord;
import com.topnivo.backend.repository.TransferRecordRepository;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransferReconciliationJob {

    private static final String AFRICA_LAGOS_TIMEZONE = "Africa/Lagos";

    private final TransferRecordRepository transferRecordRepository;
    private final PaymentService paymentService;
    private final FlutterwavePaymentService flutterwavePaymentService;

    private final ExecutorService executorService = Executors.newFixedThreadPool(2);

    @Async
    @Scheduled(cron = "0 * * * * ?", zone = AFRICA_LAGOS_TIMEZONE)
    public void reconcilePendingTransfers() {
        List<TransferRecord> pendingRecords = transferRecordRepository.findByStatus(TransferStatus.PENDING);

        if (pendingRecords.isEmpty()) {
            return;
        }

        log.info("Transfer reconciliation job found {} pending transfer records", pendingRecords.size());

        executorService.submit(() -> {
            try {
                paymentService.validatePendingPayroll();
            } catch (Exception e) {
                log.warn("Paystack reconciliation failed: {}", e.getMessage());
            }
        });

        executorService.submit(() -> {
            try {
                flutterwavePaymentService.validatePendingPayroll();
            } catch (Exception e) {
                log.warn("Flutterwave reconciliation failed: {}", e.getMessage());
            }
        });
    }

    @PreDestroy
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
