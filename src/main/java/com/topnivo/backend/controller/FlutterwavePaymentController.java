package com.topnivo.backend.controller;

import com.topnivo.backend.mapper.MemberMapper;
import com.topnivo.backend.model.entity.Order;
import com.topnivo.backend.model.entity.TransferRecord;
import com.topnivo.backend.model.request.OrderRequest;
import com.topnivo.backend.model.request.PaymentOrderRequest;
import com.topnivo.backend.model.response.ApiResponse;
import com.topnivo.backend.model.response.OrderResponse;
import com.topnivo.backend.model.response.ResponseStatus;
import com.topnivo.backend.model.response.TransferRecordResponse;
import com.topnivo.backend.service.FlutterwavePaymentService;
import com.topnivo.backend.service.OrderService;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(value = "/api/v1/flutterwave/payments")
@RequiredArgsConstructor
@Slf4j
public class FlutterwavePaymentController {

    private final FlutterwavePaymentService flutterwavePaymentService;
    private final OrderService orderService;
    private final MemberMapper memberMapper;
    private final ModelMapper modelMapper;

    @GetMapping(value = "")
    public ResponseEntity<?> getAllBanks() {
        List<Map<String, String>> allBanks = flutterwavePaymentService.findAllBanks();
        ApiResponse<List<Map<String, String>>> response = new ApiResponse<>(
                ResponseStatus.SUCCESS.name(),
                "Banks retrieved successfully!",
                allBanks,
                null
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping(value = {"/orders/{memberId}/verify", "/verify-order/{memberId}", "/verify/{memberId}"})
    public ResponseEntity<?> verifyOrderPayment(
            @PathVariable("memberId") String memberId,
            @RequestBody PaymentOrderRequest paymentRequest
    ) throws MessagingException {
        String paymentReference = paymentRequest.getPaymentReference();
        double amountPaid = flutterwavePaymentService.checkPaymentValidity(paymentReference);
        OrderRequest orderRequest = OrderRequest.builder()
                .memberType(paymentRequest.getMemberType())
                .address(paymentRequest.getAddress())
                .phoneNumber(paymentRequest.getPhoneNumber())
                .build();

        Order order = orderService.makePaidOrder(memberId, orderRequest, paymentReference, amountPaid);
        OrderResponse orderResponse = modelMapper.map(order, OrderResponse.class);
        ApiResponse<OrderResponse> response = new ApiResponse<>(
                ResponseStatus.CREATED.name(),
                "Flutterwave payment verified and order created successfully!",
                orderResponse,
                null
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/get-payroll")
    public ResponseEntity<?> getPayroll() {
        List<TransferRecord> transferRecords = flutterwavePaymentService.getPayroll();
        List<TransferRecordResponse> recordResponseList = new ArrayList<>();
        for (TransferRecord transferRecord : transferRecords) {
            TransferRecordResponse recordResponse = new TransferRecordResponse();
            recordResponse.setId(transferRecord.getId());
            recordResponse.setReason(transferRecord.getReason());
            recordResponse.setReference(transferRecord.getReference());
            recordResponse.setAmount(transferRecord.getAmount());
            recordResponse.setMember(memberMapper.memberToResponse(transferRecord.getMember(), true));
            recordResponse.setStatus(transferRecord.getStatus());

            recordResponseList.add(recordResponse);
        }

        ApiResponse<List<TransferRecordResponse>> response = new ApiResponse<>(
                ResponseStatus.SUCCESS.name(),
                "Flutterwave initialized transfer records retrieved successfully!",
                recordResponseList,
                null
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/prepare-payroll")
    public ResponseEntity<?> preparePayroll() {
        List<TransferRecord> transferRecords = flutterwavePaymentService.preparePayroll();
        List<TransferRecordResponse> recordResponseList = new ArrayList<>();
        for (TransferRecord transferRecord : transferRecords) {
            TransferRecordResponse recordResponse = new TransferRecordResponse();
            recordResponse.setId(transferRecord.getId());
            recordResponse.setReason(transferRecord.getReason());
            recordResponse.setReference(transferRecord.getReference());
            recordResponse.setAmount(transferRecord.getAmount());
            recordResponse.setMember(memberMapper.memberToResponse(transferRecord.getMember()));
            recordResponse.setStatus(transferRecord.getStatus());

            recordResponseList.add(recordResponse);
        }

        ApiResponse<List<TransferRecordResponse>> response = new ApiResponse<>(
                ResponseStatus.SUCCESS.name(),
                "Flutterwave initialized transfer records retrieved successfully!",
                recordResponseList,
                null
        );

        return ResponseEntity.ok(response);
    }

    @DeleteMapping(value = "/payroll-entry/{id}")
    public ResponseEntity<?> deleteAPayrollEntry(@PathVariable("id") int id) {
        flutterwavePaymentService.deleteAPayrollEntry(id);

        ApiResponse<String> response = new ApiResponse<>(
                ResponseStatus.DELETED.name(),
                "Flutterwave payroll entry deleted successfully!",
                "The payroll has been deleted!",
                null
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/send-payroll")
    public ResponseEntity<?> sendPayroll() {
        flutterwavePaymentService.sendPayroll();

        ApiResponse<String> response = new ApiResponse<>(
                ResponseStatus.SUCCESS.name(),
                "Flutterwave payroll sent but you might need to wait for some moment to confirm all payments are sent successfully!",
                "The payroll has been sent!",
                null
        );

        return ResponseEntity.ok(response);
    }
}
