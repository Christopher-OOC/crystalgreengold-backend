package com.topnivo.backend.service;

import com.topnivo.backend.exception.exception.*;
import com.topnivo.backend.model.constant.*;
import com.topnivo.backend.model.entity.*;
import com.topnivo.backend.model.request.OrderRequest;
import com.topnivo.backend.repository.*;
import com.topnivo.backend.util.OrderUtils;
import com.topnivo.backend.util.TransactionUtils;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final MemberRepository memberRepository;
    private final ProductRepository productRepository;
    private final StoreProductRepository storeProductRepository;
    private final CartItemRepository cartItemRepository;
    private final FlutterwavePaymentService paymentService;
    private final CommissionService commissionService;
    private final EmailService emailService;
    private final TransactionRepository transactionRepository;

    public boolean validateOrder(String memberId, OrderRequest request) {
        String errorMessage = "";
        int errorCount = 0;
        Member member = memberRepository.findByMemberId(memberId);
        if (Objects.isNull(member)) {
            throw new NoSuchResourceException(ErrorMessages.NO_SUCH_MEMBER);
        }
        if (Objects.isNull(member.getCurrentPackage())) {
            throw new BadRequestException(ErrorMessages.NOT_ON_PACKAGE);
        }
        if (Objects.isNull(member.getCart())) {
            throw new BadRequestException(ErrorMessages.NO_PRODUCT_ORDER);
        }

        List<CartItem> cartItems = cartItemRepository.findByCartId(member.getCart().getId());
        cartItems.sort(Comparator.comparing(a -> a.getProduct().getId()));

        if (request.getMemberType().equals(MemberType.REGULAR_MEMBER) ||
                request.getMemberType().equals(MemberType.SERVICE_CENTER)) {
            for (CartItem cartItem : cartItems) {
                if (!cartItem.getProduct().isDiscounted()) {
                    StoreProduct storeProduct = storeProductRepository.findProductFromStore(
                            cartItem.getStore().getMemberId(),
                            cartItem.getProduct().getId());

                    if (Objects.isNull(storeProduct)) {
                        errorMessage = errorMessage + "Reason" + ++errorCount + ". " +
                                "The store no longer have product with ID: (" +
                                cartItem.getProduct().getId() + "). Remove the product from the cart! ";
                    }
                    if (storeProduct.getBoughtQuantity() < cartItem.getQuantity()) {
                        errorMessage = errorMessage + "Reason" + ++errorCount + ". " +
                                "The available quantity of product with name ( " + storeProduct.getProduct().getName() + " )" +
                                " is " + storeProduct.getBoughtQuantity() + " in the store. " + "Reduce the quantity to " +
                                storeProduct.getBoughtQuantity() + "! ";
                    }
                }
            }
        } else if (request.getMemberType().equals(MemberType.PREMIUM_STORE)) {
            List<String> productRequestIds = new ArrayList<>(cartItems.stream()
                    .map(cartItem -> cartItem.getProduct().getId())
                    .toList());
            List<Product> productsFromCompany = productRepository.findAllById(productRequestIds);
            productsFromCompany.sort(Comparator.comparing(Product::getId));
            if (productRequestIds.isEmpty()) {
                errorMessage = errorMessage + "Reason" + ++errorCount + ". There is no product in order!";
            }
            // Check if all the products request quantity are available
            for (int i = 0; i < productsFromCompany.size(); i++) {
                CartItem cartItem = cartItems.get(i);
                Product product = productsFromCompany.get(i);
                if (product.getAvailableQuantity() < cartItem.getQuantity()) {
                    errorMessage = errorMessage + "Reason" + ++errorCount + ". " +
                            "The available quantity of product with name ( " + product.getName() + " )" +
                            " is " + product.getAvailableQuantity() + " in the company inventory. " + "Reduce the quantity to " +
                            product.getAvailableQuantity() + "! ";
                }
            }
        }

        if (!errorMessage.isEmpty()) {
            throw new BadRequestException(errorMessage);
        }

        return true;
    }

    public Order makeOrder(String memberId, OrderRequest request) throws MessagingException {
        return makeOrder(memberId, request, TransactionUtils.generateTransactionId(), false);
    }

    public Order makePaidOrder(String memberId, OrderRequest request, String paymentReference, double amountPaid) throws MessagingException {
        List<Transaction> existingTransactions = transactionRepository.findByReferenceId(paymentReference);
        if (!existingTransactions.isEmpty()) {
            return existingTransactions.stream()
                    .map(Transaction::getOrderId)
                    .filter(Objects::nonNull)
                    .map(orderRepository::findByOrderId)
                    .filter(Objects::nonNull)
                    .filter(order -> order.getMember() != null && memberId.equals(order.getMember().getMemberId()))
                    .findFirst()
                    .orElseThrow(() -> new InvalidPaymentException(ErrorMessages.INVALID_PAYMENT));
        }

        double totalAmount = calculateCartTotalAmount(memberId);
        if (!Double.isNaN(amountPaid) && Math.abs(totalAmount - amountPaid) > 0.01) {
            throw new InvalidPaymentException(ErrorMessages.PAYMENT_NOT_UP_TO_ACTUAL);
        }

        return makeOrder(memberId, request, paymentReference, true);
    }

    public double calculateCartTotalAmount(String memberId) {
        Member member = memberRepository.findByMemberId(memberId);
        if (Objects.isNull(member)) {
            throw new NoSuchResourceException(ErrorMessages.NO_SUCH_MEMBER);
        }
        if (Objects.isNull(member.getCart())) {
            throw new BadRequestException(ErrorMessages.NO_PRODUCT_ORDER);
        }

        List<CartItem> cartItems = cartItemRepository.findByCartId(member.getCart().getId());
        if (cartItems.isEmpty()) {
            throw new BadRequestException(ErrorMessages.NO_PRODUCT_ORDER);
        }

        return cartItems
                .stream()
                .map(cartItem -> {
                    double price = cartItem.getProduct().getPrice() -
                            cartItem.getProduct().getPrice() * cartItem.getProduct().getDiscount() * 0.01;
                    return price * cartItem.getQuantity();
                })
                .mapToDouble(d -> d)
                .sum();
    }

    private Order makeOrder(String memberId, OrderRequest request, String transactionReference, boolean paymentVerified) throws MessagingException {
        Member member = memberRepository.findByMemberId(memberId);
        if (Objects.isNull(member)) {
            throw new NoSuchResourceException(ErrorMessages.NO_SUCH_MEMBER);
        }
        if (Objects.isNull(member.getCurrentPackage())) {
            throw new BadRequestException(ErrorMessages.NOT_ON_PACKAGE);
        }
        List<CartItem> cartItems = cartItemRepository.findByCartId(member.getCart().getId());
        cartItems.sort(Comparator.comparing(a -> a.getProduct().getId()));

        double amount = paymentService.checkPaymentValidity(transactionReference);
        paymentService.sendMoneyToStoreOwner(cartItems.get(0).getStore(), amount);

        if (cartItems.isEmpty()) {
            throw new BadRequestException(ErrorMessages.NO_PRODUCT_ORDER);
        }

        double originalPrice = cartItems
                .stream()
                .map(cartItem -> cartItem.getProduct().getPrice())
                .mapToDouble(d -> d).sum();

        double totalPrice = cartItems
                .stream()
                .map(cartItem -> {
                    double price = cartItem.getProduct().getPrice() -
                            cartItem.getProduct().getPrice() * cartItem.getProduct().getDiscount() * 0.01;
                    return price * cartItem.getQuantity();
                })
                .mapToDouble(d -> d).sum();

        double totalBoughtPv = cartItems
                .stream()
                .map(c -> (c.getProduct().getPv() * c.getQuantity()))
                .mapToDouble(c -> c)
                .sum();
        double totalBoughtBv = cartItems
                .stream()
                .map(c -> (c.getProduct().getBv() * c.getQuantity()))
                .mapToDouble(c -> c)
                .sum();

        // Create an empty ProductOrderItem list
        List<OrderItem> orderItems = new ArrayList<>();
        // Create an Order
        Order order = new Order();
        order.setOrderId(OrderUtils.generateOrder(8));
        order.setOrderStatus(OrderStatus.PENDING);
        order.setOrderType(OrderType.BUY_PRODUCT);
        order.setOrderDate(LocalDateTime.now());
        order.setAddress(request.getAddress());
        order.setPhoneNumber(request.getPhoneNumber());
        order.setTotalAmount(totalPrice);

        Order newOrder = orderRepository.save(order);

        boolean hasDiscounted = cartItems.stream().anyMatch(item -> item.getProduct().isDiscounted());

        if (hasDiscounted) {
            if (totalPrice > member.getAvailableBalance()) {
                throw new InsufficientBalanceException(ErrorMessages.INSUFFICIENT_FUNDS);
            }
        }

        if ((request.getMemberType().equals(MemberType.REGULAR_MEMBER) ||
                request.getMemberType().equals(MemberType.SERVICE_CENTER)) && !hasDiscounted) {
            for (CartItem cartItem : cartItems) {
                StoreProduct storeProduct = storeProductRepository.findProductFromStore(
                        cartItem.getStore().getMemberId(),
                        cartItem.getProduct().getId());
                if (Objects.isNull(storeProduct)) {
                    throw new NoSuchResourceException(ErrorMessages.NO_SUCH_PRODUCT);
                }
                if (storeProduct.getBoughtQuantity() <= 0 && storeProduct.getBoughtQuantity() < cartItem.getQuantity()) {
                    throw new InsufficientProductQuantity(ErrorMessages.INSUFFICIENT_PRODUCT_IN_STOCK);
                }
                // Create an Order OrderItem
                ProductOrderItem orderItem = createProductOrderItem(storeProduct.getProduct(), storeProduct.getProduct().getId(),
                        storeProduct.getStore().getMemberId(),
                        cartItem);

                orderItem.setOrder(newOrder);
                orderItems.add(orderItem);
            }
        }
        else if (request.getMemberType().equals(MemberType.PREMIUM_STORE) || hasDiscounted) {
            List<String> productRequestIds = new ArrayList<>(cartItems.stream()
                    .map(cartItem -> cartItem.getProduct().getId())
                    .toList());
            List<Product> productsFromCompany = productRepository.findAllById(productRequestIds);
            productsFromCompany.sort(Comparator.comparing(Product::getId));
            if (productRequestIds.isEmpty()) {
                throw new InvalidOrderException(ErrorMessages.NO_PRODUCT_ORDER);
            }
            // Check if all the products request quantity are available
            for (int i = 0; i < productsFromCompany.size(); i++) {
                CartItem cartItem = cartItems.get(i);
                Product product = productsFromCompany.get(i);
                if (product.getAvailableQuantity() <= 0 && product.getAvailableQuantity() < cartItem.getQuantity()) {
                    throw new BadRequestException(ErrorMessages.INSUFFICIENT_PRODUCT_IN_STOCK);
                }
                // Create an Order OrderItem
                ProductOrderItem productItem = createProductOrderItem(product, product.getId(),
                        "Directly from company", cartItem);

                productItem.setOrder(newOrder);
                orderItems.add(productItem);
            }
            productRepository.saveAll(productsFromCompany);

            if (hasDiscounted) {
                member.setAvailableBalance(member.getAvailableBalance() - totalPrice);
                member.setCashback(member.getCashback() + (originalPrice - totalPrice));
            }
        }
        else {
            throw new BadRequestException(ErrorMessages.INVALID_OPERATION);
        }

        TransactionStatus status = hasDiscounted || paymentVerified ? TransactionStatus.COMPLETED : TransactionStatus.NOT_CONFIRMED;

        Transaction transaction = createTransaction(
                transactionReference,
                member.getMemberId(),
                "You requested to buy products!",
                newOrder.getOrderId(),
                totalPrice,
                TransactionType.BUY_PRODUCT,
                status
        );

        member.setLastActive(LocalDateTime.now());
        member.getCart().setCartItems(new ArrayList<>());
        memberRepository.save(member);
        cartItemRepository.deleteAllByCartId(member.getCart().getId());

        newOrder.setTransaction(transaction);
        newOrder.setStore(cartItems.get(0).getStore());
        newOrder.setOrderItems(orderItems);
        newOrder.setMember(member);
        newOrder = orderRepository.save(newOrder);

        // Email Confirmation
        String referenceId = transaction.getReferenceId();
        String storeName = cartItems.get(0).getStore() == null ? "Topnivo" : cartItems.get(0).getStore().getBusinessName();
        emailService.sendOrderConfirmationEmail(member, newOrder, storeName, referenceId, totalBoughtBv, totalBoughtPv, totalPrice);

        return newOrder;
    }

    private static ProductOrderItem createProductOrderItem(Product product, String id, String sellerId, CartItem cartItem) {
        ProductOrderItem productItem = new ProductOrderItem();
        productItem.setOriginalId(id);
        productItem.setBv(product.getBv());
        productItem.setPv(product.getPv());
        productItem.setName(product.getName());
        productItem.setDescription(product.getDescription());
        productItem.setImage(product.getImage());
        productItem.setCategoryName(product.getCategory().getName());
        productItem.setPrice(product.getPrice());
        productItem.setQuantity(cartItem.getQuantity());
        productItem.setSellerId(sellerId);
        return productItem;
    }

    private Transaction createTransaction(
            String transactionId,
            String memberId,
            String message,
            String orderId,
            double amount,
            TransactionType type,
            TransactionStatus status
    ) {
        Transaction transaction = new Transaction();
        transaction.setMemberId(memberId);
        transaction.setMessage(message);
        transaction.setOrderId(orderId);
        transaction.setAmount(amount);
        transaction.setTransactionDate(LocalDateTime.now());
        transaction.setType(type);
        transaction.setReferenceId(transactionId);
        transaction.setStatus(status);

        return transactionRepository.save(transaction);
    }

    public Page<Order> findAllOrderByPage(int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size);

        return orderRepository.findByOrderByOrderDateDesc(pageable);
    }

    public Order findOrderById(String memberId, String orderId) {
        Member member = memberRepository.findByMemberId(memberId);
        if (Objects.isNull(member)) {
            throw new NoSuchResourceException(ErrorMessages.NO_SUCH_MEMBER);
        }

        Order order = orderRepository.findByOrderId(orderId);
        if (Objects.isNull(order)) {
            throw new NoSuchResourceException(ErrorMessages.NO_SUCH_ORDER);
        }

        return order;
    }

    public Order changeOrderStatus(String memberId, String orderId, String status) {
        Member member = memberRepository.findByMemberId(memberId);
        Order order = orderRepository.findByOrderId(orderId);
        if (Objects.isNull(member)) {
            throw new NoSuchResourceException(ErrorMessages.NO_SUCH_MEMBER);
        }
        if (Objects.isNull(order)) {
            throw new NoSuchResourceException(ErrorMessages.NO_SUCH_ORDER);
        }

        try {
            OrderStatus orderStatus = OrderStatus.valueOf(status);
            order.setOrderStatus(orderStatus);
        }
        catch (Exception ex) {
            throw new BadRequestException(ErrorMessages.INVALID_ORDER_STATUS);
        }

        return orderRepository.save(order);
    }

    public Page<Order> findOrdersPageByMemberId(String memberId, String status, String from, String to, int page, int size) {
        Member member = memberRepository.findByMemberId(memberId);
        Pageable pageable = PageRequest.of(page - 1, size);
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        OrderStatus orderStatus = null;
        try {
            if (!status.equals("ALL")) {
                orderStatus = OrderStatus.valueOf(status);
            }
        } catch (Exception ex) {
            throw new BadRequestException(ErrorMessages.INVALID_ORDER_STATUS);
        }

        if (Objects.isNull(member)) {
            throw new NoSuchResourceException(ErrorMessages.NO_SUCH_MEMBER);
        }

        if (from.isEmpty() && to.isEmpty()) {
            if (status.equals("ALL")) {
                return orderRepository.findByMemberOrderByOrderDateDesc(member, pageable);
            } else {
                return orderRepository.findByMemberAndOrderStatusOrderByOrderDateDesc(member, orderStatus, pageable);
            }
        } else if (!from.isEmpty() && to.isEmpty()) {
            LocalDateTime fromDate = LocalDate.parse(from, dateFormatter).atStartOfDay();
            LocalDateTime toDate = LocalDate.now().atStartOfDay();
            if (status.equals("ALL")) {
                return orderRepository.findByMemberAndOrderDateBetweenOrderByOrderDateDesc(member, fromDate, toDate, pageable);
            } else {
                return orderRepository.findByMemberAndOrderStatusAndOrderDateBetweenOrderByOrderDateDesc(member, orderStatus, fromDate, toDate, pageable);
            }
        } else if (!from.isEmpty()) {
            LocalDateTime fromDate = LocalDate.parse(from, dateFormatter).atStartOfDay();
            LocalDateTime toDate = LocalDate.parse(to, dateFormatter).atStartOfDay();
            if (status.equals("ALL")) {
                return orderRepository.findByMemberAndOrderDateBetweenOrderByOrderDateDesc(member, fromDate, toDate, pageable);
            } else {
                return orderRepository.findByMemberAndOrderStatusAndOrderDateBetweenOrderByOrderDateDesc(member, orderStatus, fromDate, toDate, pageable);
            }
        } else {
            LocalDateTime fromDate = LocalDate.parse(to, dateFormatter).atStartOfDay();
            LocalDateTime toDate = LocalDate.parse(to, dateFormatter).atStartOfDay();
            if (status.equals("ALL")) {
                return orderRepository.findByMemberAndOrderDateBetweenOrderByOrderDateDesc(member, fromDate, toDate, pageable);
            } else {
                return orderRepository.findByMemberAndOrderStatusAndOrderDateBetweenOrderByOrderDateDesc(member, orderStatus, fromDate, toDate, pageable);
            }
        }
    }

    public Page<Order> findManageOrdersPageByStoreId(String memberId, String status, String from, String to, int page, int size) {
        Member store = memberRepository.findByMemberId(memberId);
        Pageable pageable = PageRequest.of(page - 1, size);
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        OrderStatus orderStatus = null;
        try {
            if (!status.equals("ALL")) {
                orderStatus = OrderStatus.valueOf(status);
            }
        } catch (Exception ex) {
            throw new BadRequestException(ErrorMessages.INVALID_ORDER_STATUS);
        }

        if (Objects.isNull(store)) {
            throw new NoSuchResourceException(ErrorMessages.NO_SUCH_MEMBER);
        }

        boolean isAdmin = store.getRoles().stream().anyMatch(role -> role.getName().equals("ROLE_" + MemberType.ADMIN));
        boolean isPremiumStore = store.getRoles().stream().anyMatch(role -> role.getName().equals("ROLE_" + MemberType.PREMIUM_STORE));
        boolean isServiceCenter = store.getRoles().stream().anyMatch(role -> role.getName().equals("ROLE_" + MemberType.SERVICE_CENTER));

        if (!(isAdmin || isPremiumStore || isServiceCenter)) {
            throw new BadRequestException(ErrorMessages.INVALID_OPERATION);
        }

        if (isAdmin) {
            store = null;
        }

        if (from.isEmpty() && to.isEmpty()) {
            if (status.equals("ALL")) {
                return orderRepository.findByStoreOrderByOrderDateDesc(store, pageable);
            } else {
                return orderRepository.findByStoreAndOrderStatusOrderByOrderDateDesc(store, orderStatus, pageable);
            }
        } else if (!from.isEmpty() && to.isEmpty()) {
            LocalDateTime fromDate = LocalDate.parse(from, dateFormatter).atStartOfDay();
            LocalDateTime toDate = LocalDate.now().atStartOfDay();
            if (status.equals("ALL")) {
                return orderRepository.findByStoreAndOrderDateBetweenOrderByOrderDateDesc(store, fromDate, toDate, pageable);
            } else {
                return orderRepository.findByStoreAndOrderStatusAndOrderDateBetweenOrderByOrderDateDesc(store, orderStatus, fromDate, toDate, pageable);
            }
        } else if (!from.isEmpty()) {
            LocalDateTime fromDate = LocalDate.parse(from, dateFormatter).atStartOfDay();
            LocalDateTime toDate = LocalDate.parse(to, dateFormatter).atStartOfDay();
            if (status.equals("ALL")) {
                return orderRepository.findByStoreAndOrderDateBetweenOrderByOrderDateDesc(store, fromDate, toDate, pageable);
            } else {
                return orderRepository.findByStoreAndOrderStatusAndOrderDateBetweenOrderByOrderDateDesc(store, orderStatus, fromDate, toDate, pageable);
            }
        } else {
            LocalDateTime fromDate = LocalDate.parse(to, dateFormatter).atStartOfDay();
            LocalDateTime toDate = LocalDate.parse(to, dateFormatter).atStartOfDay();
            if (status.equals("ALL")) {
                return orderRepository.findByStoreAndOrderDateBetweenOrderByOrderDateDesc(store, fromDate, toDate, pageable);
            } else {
                return orderRepository.findByStoreAndOrderStatusAndOrderDateBetweenOrderByOrderDateDesc(store, orderStatus, fromDate, toDate, pageable);
            }
        }
    }
}
