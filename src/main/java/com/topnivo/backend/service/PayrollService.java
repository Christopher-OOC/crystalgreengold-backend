package com.topnivo.backend.service;

import com.topnivo.backend.model.constant.TransferStatus;
import com.topnivo.backend.model.entity.AccountDetails;
import com.topnivo.backend.model.entity.Member;
import com.topnivo.backend.model.entity.TransferRecord;
import com.topnivo.backend.repository.TransferRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PayrollService {

    private final TransferRecordRepository transferRecordRepository;

    public ByteArrayResource generatePayrollReportCsv() throws IOException {

        List<TransferRecord> transferRecords =
                transferRecordRepository.findByStatus(TransferStatus.INITIALIZED);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(outputStream, StandardCharsets.UTF_8))) {

            writer.write("S/N,Username,Amount,Account Name,Bank,Rank,Package");
            writer.newLine();

            int serialNumber = 1;

            for (TransferRecord transferRecord : transferRecords) {

                Member member = transferRecord.getMember();

                if (member == null) {
                    continue;
                }

                AccountDetails accountDetails = member.getAccountDetails();

                writer.write(String.join(",",
                        String.valueOf(serialNumber++),
                        escapeCsv(member.getUsername()),
                        String.valueOf(transferRecord.getAmount()),
                        escapeCsv(accountDetails != null ? accountDetails.getAccountName() : ""),
                        escapeCsv(accountDetails != null ? accountDetails.getBankName() : ""),
                        escapeCsv(member.getRank() != null ? member.getRank().getName() : ""),
                        escapeCsv(member.getCurrentPackage() != null
                                ? member.getCurrentPackage().getName()
                                : "")
                ));

                writer.newLine();
            }

            writer.flush();
        }

        return new ByteArrayResource(outputStream.toByteArray());
    }

    public ByteArrayResource generateFlutterwavePayrollCsv() throws IOException {

        List<TransferRecord> transferRecords =
                transferRecordRepository.findByStatus(TransferStatus.INITIALIZED);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(outputStream, StandardCharsets.UTF_8))) {

            writer.write("Bank Name,Account Number,Amount,Narration");
            writer.newLine();

            for (TransferRecord transferRecord : transferRecords) {

                Member member = transferRecord.getMember();

                if (member == null
                        || member.getAccountDetails() == null
                        || member.getAccountDetails().getAccountNumber() == null) {
                    continue;
                }

                String bankName = escapeCsv(member.getAccountDetails().getBankName());
                String accountNumber = escapeCsv(member.getAccountDetails().getAccountNumber());
                String amount = String.valueOf(transferRecord.getAmount());
                String narration = escapeCsv(transferRecord.getReason());

                writer.write(String.join(",",
                        bankName,
                        accountNumber,
                        amount,
                        narration));

                writer.newLine();
            }

            writer.flush();
        }

        return new ByteArrayResource(outputStream.toByteArray());
    }

    private String escapeCsv(String value) {

        if (value == null) {
            return "";
        }

        value = value.replace("\"", "\"\"");

        if (value.contains(",")
                || value.contains("\"")
                || value.contains("\n")) {

            return "\"" + value + "\"";
        }

        return value;
    }
}