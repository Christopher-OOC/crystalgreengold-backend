package com.topnivo.backend.controller;

import com.topnivo.backend.model.entity.Transaction;
import com.topnivo.backend.model.response.ApiResponse;
import com.topnivo.backend.model.response.ResponseStatus;
import com.topnivo.backend.model.response.TransactionResponse;
import com.topnivo.backend.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(value = "/api/v1/transactions")
@RequiredArgsConstructor
@Slf4j
public class TransactionController {

    private final TransactionService transactionService;
    private final ModelMapper modelMapper;

    @GetMapping(value = "/members/{memberId}")
    public ResponseEntity<?> getTransactionByPage(
            @PathVariable(value = "memberId", required = true) String memberId,
            @RequestParam(name = "page", required = false, defaultValue = "1") int page,
            @RequestParam(name = "size", required = false, defaultValue = "10") int size,
            @RequestParam(name = "type", required = false, defaultValue = "ALL") String type
    ) {
        Page<Transaction> transactionPage = transactionService.getTransactionByPage(memberId, page, size, type);

        Map<String, Integer> metadata = new LinkedHashMap<>();
        metadata.put("page", page);
        metadata.put("size", size);
        metadata.put("numberOfElements", transactionPage.getNumberOfElements());
        metadata.put("totalNumberOfElements", (int) transactionPage.getTotalElements());
        metadata.put("totalPages", transactionPage.getTotalPages());

        ApiResponse<List<Transaction>> response = new ApiResponse<>(
                ResponseStatus.SUCCESS.name(),
                "Transactions retrieved successfully!",
                transactionPage.getContent(),
                metadata
        );

        return ResponseEntity.ok(response);
    }
}
