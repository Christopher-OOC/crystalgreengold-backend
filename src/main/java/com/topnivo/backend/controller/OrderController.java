package com.topnivo.backend.controller;

import com.topnivo.backend.mapper.MemberMapper;
import com.topnivo.backend.model.constant.OrderType;
import com.topnivo.backend.model.entity.Member;
import com.topnivo.backend.model.entity.Order;
import com.topnivo.backend.model.entity.OrderItem;
import com.topnivo.backend.model.entity.ProductOrderItem;
import com.topnivo.backend.model.request.OrderRequest;
import com.topnivo.backend.model.response.*;
import com.topnivo.backend.model.response.ResponseStatus;
import com.topnivo.backend.service.FlutterwavePaymentService;
import com.topnivo.backend.service.MemberService;
import com.topnivo.backend.service.OrderService;
import jakarta.mail.MessagingException;
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
@RequestMapping(value = "/api/v1/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;
    private final FlutterwavePaymentService paymentService;
    private final ModelMapper modelMapper;
    private final MemberService memberService;
    private final MemberMapper memberMapper;

    @PostMapping(value = "/validate/{memberId}")
    public ResponseEntity<?> validateOrder(
            @PathVariable("memberId") String memberId,
            @RequestBody OrderRequest request
    ) {
        boolean isValidated = orderService.validateOrder(memberId, request);

        ApiResponse<Boolean> response = new ApiResponse<>(
                ResponseStatus.VALIDATED.name(),
                "Your Order has been validated successfully!",
                isValidated,
                null
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/create-order/{memberId}")
    public ResponseEntity<?> makeOrder(
            @PathVariable("memberId") String memberId,
            @RequestBody OrderRequest request
    ) throws MessagingException {
        String paymentReference = request.getPaymentReference();
        Order order;
        if (paymentReference == null) {
            order = orderService.makeOrder(memberId, request);
        } else {
            double amountPaid = paymentService.checkPaymentValidity(paymentReference);
            order = orderService.makePaidOrder(memberId, request, paymentReference, amountPaid);
        }
        OrderResponse orderResponse = modelMapper.map(order, OrderResponse.class);
        Member member = memberService.confirmOrderById(memberId, order.getOrderId(), "CONFIRMED");

        ApiResponse<OrderResponse> response = new ApiResponse<>(
                ResponseStatus.CREATED.name(),
                "Order created successfully!",
                orderResponse,
                null
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/members/{memberId}")
    public ResponseEntity<?> findOrdersPageByMemberId(
            @PathVariable(value = "memberId") String memberId,
            @RequestParam(value = "status", required = false, defaultValue = "ALL") String status,
            @RequestParam(value = "from", required = false, defaultValue = "") String from,
            @RequestParam(value = "to", required = false, defaultValue = "") String to,
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @RequestParam(value = "size", required = false, defaultValue = "5") int size
    ) {
        Page<Order> orderPage = orderService.findOrdersPageByMemberId(memberId, status, from, to, page, size);

        Map<String, Integer> metadata = new LinkedHashMap<>();
        metadata.put("page", page);
        metadata.put("size", size);
        metadata.put("number", orderPage.getNumberOfElements());
        metadata.put("totalPages", orderPage.getTotalPages());

        List<OrderResponse> responseList = new ArrayList<>();
        orderPage.getContent().forEach(order -> responseList.add(modelMapper.map(order, OrderResponse.class)));

        ApiResponse<List<OrderResponse>> response = new ApiResponse<>(
                ResponseStatus.SUCCESS.name(),
                "Orders retrieved successfully!",
                responseList,
                metadata
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/{orderId}/members/{memberId}")
    public ResponseEntity<?> findByOrderId(
            @PathVariable("memberId") String memberId,
            @PathVariable("orderId") String orderId
    ) {
        Order order = orderService.findOrderById(memberId, orderId);
        OrderResponse orderResponse = modelMapper.map(order, OrderResponse.class);

        Member store = order.getStore();
        StoreResponse storeResponse = new StoreResponse();
        if (store == null) {
            storeResponse.setBusinessName("Topnivo");
            storeResponse.setAddress("12, eyterur, Lagos");
            storeResponse.setImage(null);
            storeResponse.setPhoneNumber("0901237367");
        } else {
            storeResponse.setBusinessName(store.getBusinessName());
            storeResponse.setAddress(storeResponse.getAddress());
            storeResponse.setImage(null);
            storeResponse.setPhoneNumber(store.getPhoneNumber());
        }

        orderResponse.getOrderItems().forEach(orderItemResponse -> orderItemResponse.setStore(storeResponse));

        ApiResponse<OrderResponse> response = new ApiResponse<>(
                ResponseStatus.SUCCESS.name(),
                "Orders retrieved successfully!",
                orderResponse,
                null
        );

        return ResponseEntity.ok(response);
    }

    @PutMapping(value = "/{orderId}/members/{memberId}")
    public ResponseEntity<?> changeOrderStatus(
            @PathVariable(value = "memberId") String memberId,
            @PathVariable(value = "orderId") String orderId,
            @RequestParam(value = "status") String status
    ) {
        Order order = orderService.changeOrderStatus(memberId, orderId, status);
        OrderResponse orderResponse = modelMapper.map(order, OrderResponse.class);
        ApiResponse<OrderResponse> response = new ApiResponse<>(
                ResponseStatus.SUCCESS.name(),
                "Order status updated successfully!",
                orderResponse,
                null
        );

        return ResponseEntity.ok(response);
    }

    @PutMapping(value = "/{orderId}/confirm-orders/{memberId}")
    public ResponseEntity<?> confirmOrderById(
            @PathVariable("memberId") String memberId,
            @PathVariable(value = "orderId", required = true) String orderId,
            @RequestParam(value = "status", required = true) String status
    ) {
        Member member = memberService.confirmOrderById(memberId, orderId, status);
        MemberResponse memberResponse = memberMapper.memberToResponse(member);
        ApiResponse<MemberResponse> response = new ApiResponse<>(
                ResponseStatus.SUCCESS.name(),
                "Order confirmed successfully!",
                memberResponse,
                null
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/manage-orders/members/{storeId}")
    public ResponseEntity<?> findManageOrdersPageByStoreId(
            @PathVariable(value = "storeId") String storeId,
            @RequestParam(value = "status", required = false, defaultValue = "ALL") String status,
            @RequestParam(value = "from", required = false, defaultValue = "") String from,
            @RequestParam(value = "to", required = false, defaultValue = "") String to,
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @RequestParam(value = "size", required = false, defaultValue = "5") int size
    ) {
        Page<Order> orderPage = orderService.findManageOrdersPageByStoreId(storeId, status, from, to, page, size);

        Map<String, Integer> metadata = new LinkedHashMap<>();
        metadata.put("page", page);
        metadata.put("size", size);
        metadata.put("numberOfElements", orderPage.getNumberOfElements());
        metadata.put("totalNumberOfElements", (int) orderPage.getTotalElements());
        metadata.put("totalPages", orderPage.getTotalPages());

        List<OrderResponse> responseList = new ArrayList<>();
        orderPage.getContent().forEach(order -> responseList.add(modelMapper.map(order, OrderResponse.class)));

        ApiResponse<List<OrderResponse>> response = new ApiResponse<>(
                ResponseStatus.SUCCESS.name(),
                "Orders retrieved successfully!",
                responseList,
                metadata
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<?> findAllOrders(
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @RequestParam(value = "size", required = false, defaultValue = "5") int size
    ) {
        Page<Order> orderPage = orderService.findAllOrderByPage(page, size);

        Map<String, Integer> metadata = new LinkedHashMap<>();
        metadata.put("page", page);
        metadata.put("size", size);
        metadata.put("number", orderPage.getNumberOfElements());
        metadata.put("totalPages", orderPage.getTotalPages());

        List<OrderResponse> responseList = new ArrayList<>();
        orderPage.getContent().forEach(order -> responseList.add(modelMapper.map(order, OrderResponse.class)));

        ApiResponse<List<OrderResponse>> response = new ApiResponse<>(
                ResponseStatus.SUCCESS.name(),
                "Orders retrieved successfully!",
                responseList,
                metadata
        );

        return ResponseEntity.ok(response);
    }
}
