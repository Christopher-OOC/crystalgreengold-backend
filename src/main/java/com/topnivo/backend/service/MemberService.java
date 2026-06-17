package com.topnivo.backend.service;

import com.topnivo.backend.exception.exception.*;
import com.topnivo.backend.mapper.MemberMapper;
import com.topnivo.backend.model.constant.*;
import com.topnivo.backend.model.entity.*;
import com.topnivo.backend.model.entity.Package;
import com.topnivo.backend.model.request.*;
import com.topnivo.backend.model.response.MemberLittleResponse;
import com.topnivo.backend.model.response.MemberNodeResponse;
import com.topnivo.backend.model.response.MemberResponse;
import com.topnivo.backend.repository.*;
import com.topnivo.backend.security.JwtService;
import com.topnivo.backend.util.OrderUtils;
import com.topnivo.backend.util.PasswordUtils;
import com.topnivo.backend.util.TransactionUtils;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class MemberService {

    private static final String DEFAULT_PASSWORD = "test";

    private final MemberRepository memberRepository;
    private final EmailService emailService;
    private final FileService fileService;
    private final PackageService packageService;
    private final MemberMapper memberMapper;
    private final BCryptPasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final PaymentService paymentService;
    private final TransactionRepository transactionRepository;
    private final PromotionRepository promotionRepository;
    private final EarnedPromotionRepository earnedPromotionRepository;
    private final CommissionService commissionService;
    private final StorePackageRepository storePackageRepository;
    private final OrderRepository orderRepository;
    private final StoreProductRepository storeProductRepository;
    private final ProductRepository productRepository;
    private final JwtService jwtService;

    public Member createCustomer(MemberCreateRequest request, String sponsor, String placer, MemberLeg leg) throws MessagingException {
        validateUsernameAvailability(request.getUsername());

        Member newMember = buildNewMember(request);

        if (!sponsor.isEmpty() && !placer.isEmpty() && leg != null) {
            placeMemberInNetwork(newMember, sponsor, placer, leg);
        } else if (!sponsor.isEmpty() || !placer.isEmpty() || leg != null) {
            throw new BadRequestException(ErrorMessages.INVALID_PARAMETER_REGISTRATION);
        }

        setMemberPasswordAndSendEmail(newMember);
        return memberRepository.save(newMember);
    }

    private void validateUsernameAvailability(String username) {
        if (memberRepository.findByUsername(username) != null) {
            throw new ResourceAlreadyExistException(ErrorMessages.MEMBER_ALREADY_EXIST);
        }
    }

    private Member buildNewMember(MemberCreateRequest request) {
        Member newMember = memberMapper.requestToCustomer(request);
        newMember.setMemberId(UUID.randomUUID().toString());
        newMember.setEnabled(true);
        newMember.getAccountListUsernames().add(request.getUsername());

        Role role = roleRepository.findByName("ROLE_" + MemberType.REGULAR_MEMBER.name());
        newMember.setRoles(Collections.singletonList(role));

        return newMember;
    }

    private void placeMemberInNetwork(Member newMember, String sponsorUsername, String placerUsername, MemberLeg leg) {
        Member sponsor = findMemberByUsername(sponsorUsername);
        Member placer = findMemberByUsername(placerUsername);

        validatePlacement(sponsor, placer, leg);

        newMember.setSponsor(sponsor);
        newMember.setPlacer(placer);

        if (leg == MemberLeg.LEFT) {
            placer.setLeftLeg(newMember);
        } else {
            placer.setRightLeg(newMember);
        }

        memberRepository.save(placer);
    }

    private void validatePlacement(Member sponsor, Member placer, MemberLeg leg) {
        if (sponsor.getId() != placer.getId() && !isPlacerDownlineOfSponsor(sponsor, placer)) {
            throw new BadRequestException(ErrorMessages.INVALID_INVITE);
        }

        if (isPlacerLegsFull(placer)) {
            throw new BadRequestException(ErrorMessages.PLACER_LEG_FULL);
        }

        if (leg == MemberLeg.LEFT && placer.getLeftLeg() != null) {
            throw new BadRequestException(ErrorMessages.LEFT_LEG_OCCUPIED);
        }

        if (leg == MemberLeg.RIGHT && placer.getRightLeg() != null) {
            throw new BadRequestException(ErrorMessages.RIGHT_LEG_OCCUPIED);
        }
    }

    private void setMemberPasswordAndSendEmail(Member member) throws MessagingException {
//        String password = DEFAULT_PASSWORD;
        String password = PasswordUtils.generatePassword(8);
        member.setPassword(passwordEncoder.encode(password));
        sendWelcomeEmail(member, password);
    }

    private void sendWelcomeEmail(Member member, String password) throws MessagingException {
        String toEmail = member.getEmail();
        String fullName = member.getFirstName() + " " + member.getLastName();
        String username = member.getUsername();
        emailService.sendAccountCreationEmail(fullName, toEmail, username, password);
    }

    public Member adminUpdateMember(String memberId, String role, boolean enabled) {
        checkIfAdminIsValid();

        Member member = findMemberByMemberId(memberId);
        Role regularRole = roleRepository.findByName("ROLE_" + MemberType.REGULAR_MEMBER);

        var authorities = SecurityContextHolder.getContext().getAuthentication().getAuthorities();

        // Only SUPER_ADMIN can make member ADMIN
        boolean isAdminSuperAdmin = authorities.stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN"));

        boolean isAdminAdmin = authorities.stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        boolean isMemberSuperAdmin = member.getRoles()
                .stream()
                .anyMatch(a -> a.getName().equals("ROLE_SUPER_ADMIN"));

        boolean isMemberAdmin = member.getRoles()
                .stream()
                .anyMatch(a -> a.getName().equals("ROLE_ADMIN"));

        if (!role.isEmpty()) {
            Role newRole = roleRepository.findByName(role);
            if (newRole == null) {
                throw new BadRequestException(ErrorMessages.INVALID_ROLE);
            }

            if (isAdminSuperAdmin || isAdminAdmin) {
                if (isAdminSuperAdmin) {
                    if (isMemberAdmin || (!isMemberSuperAdmin && role.equals("ROLE_ADMIN"))) {
                        member.getRoles().clear();
                        member.getRoles().addAll(Arrays.asList(regularRole, newRole));
                        member.setEnabled(enabled);
                    } else {
                        throw new BadRequestException(ErrorMessages.INVALID_OPERATION);
                    }
                } else {
                    if (!isMemberSuperAdmin && !isMemberAdmin && !role.equals("ROLE_ADMIN")) {
                        member.getRoles().clear();
                        member.getRoles().addAll(Arrays.asList(regularRole, newRole));
                        member.setEnabled(enabled);
                    } else {
                        throw new BadRequestException(ErrorMessages.INVALID_OPERATION);
                    }
                }
            } else {
                throw new BadRequestException(ErrorMessages.INVALID_OPERATION);
            }
        } else {
            if (isAdminSuperAdmin) {
                if (!isMemberAdmin) {
                    throw new BadRequestException(ErrorMessages.INVALID_OPERATION);
                } else {
                    member.setEnabled(enabled);
                }
            } else if (isAdminAdmin) {
                if (isMemberSuperAdmin || isMemberAdmin) {
                    throw new BadRequestException(ErrorMessages.INVALID_OPERATION);
                } else {
                    member.setEnabled(enabled);
                }
            } else {
                throw new BadRequestException(ErrorMessages.INVALID_OPERATION);
            }
        }

        return memberRepository.save(member);
    }

    private void checkIfAdminIsValid() {
        var authorities = SecurityContextHolder.getContext().getAuthentication().getAuthorities();
        boolean isAdminSuperAdmin = authorities.stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN"));
        String adminUserName = SecurityContextHolder.getContext().getAuthentication().getName();
        Member admin = findMemberByUsername(adminUserName);

        if (!isAdminSuperAdmin && admin.getSponsor() == null && admin.getPlacer() == null) {
            throw new BadRequestException(ErrorMessages.NO_SPONSOR_AND_PLACER);
        }
    }

    public Member findMemberByUsername(String username) {
        return Optional.ofNullable(memberRepository.findByUsername(username))
                .orElseThrow(() -> new NoSuchResourceException(ErrorMessages.NO_SUCH_MEMBER));
    }

    public Member findEligibleSponsorByUsername(String username) {
        String memberUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        Member sponsor = memberRepository.findByUsername(username);

        if (sponsor == null) {
            throw new NoSuchResourceException(ErrorMessages.NO_SUCH_MEMBER);
        }

        if (sponsor.getUsername().equals(memberUsername)) {
            throw new NoSuchResourceException(ErrorMessages.INVALID_SPONSOR);
        }

//        if (!sponsor.getUsername().equals("super-admin") && sponsor.getSponsor() == null) {
//            throw new NoSuchResourceException(ErrorMessages.INVALID_SPONSOR);
//        }

        return sponsor;
    }

    public Member findMemberByMemberId(String memberId) {
        return Optional.ofNullable(memberRepository.findByMemberId(memberId))
                .orElseThrow(() -> new NoSuchResourceException(ErrorMessages.NO_SUCH_MEMBER));
    }

    public List<Member> findAllServiceCenters() {
        return memberRepository.findByRolesName("ROLE_" + MemberType.SERVICE_CENTER.name());
    }

    public List<Member> findAllPremiumStores() {
        return memberRepository.findByRolesName("ROLE_" + MemberType.PREMIUM_STORE.name());
    }

    public List<Member> findAllMemberDownLines(Member member) {
        List<Member> downLines = new ArrayList<>();
        if (member != null) {
            collectAllDownLines(member, downLines);
        }

        return downLines;
    }

    public Member uploadProfilePicture(String memberId, MultipartFile multipartFile) throws IOException {
        Member member = findMemberByMemberId(memberId);
        if (multipartFile != null) {
            member.setImage(fileService.fileBtyeToGenerateFileUrl(multipartFile.getBytes()));
        }

        return memberRepository.save(member);
    }

    public MemberNodeResponse findGenealogyTree(String memberId) {
        Member member = findMemberByMemberId(memberId);
        MemberNodeResponse rootNode = new MemberNodeResponse();
        Queue<Member> memberQueue = new LinkedList<>();
        Queue<MemberNodeResponse> nodeQueue = new LinkedList<>();

        memberQueue.offer(member);
        nodeQueue.offer(rootNode);

        int currentId = 1;
        rootNode.setId(currentId++);

        while (!memberQueue.isEmpty()) {
            Member currentMember = memberQueue.poll();
            MemberNodeResponse currentNode = nodeQueue.poll();

            setNodeProperties(currentNode, currentMember);

            if (currentMember.getLeftLeg() != null) {
                MemberNodeResponse leftNode = new MemberNodeResponse();
                leftNode.setId(currentId++);
                currentNode.setLeft(leftNode);
                memberQueue.offer(currentMember.getLeftLeg());
                nodeQueue.offer(leftNode);
            }

            if (currentMember.getRightLeg() != null) {
                MemberNodeResponse rightNode = new MemberNodeResponse();
                rightNode.setId(currentId++);
                currentNode.setRight(rightNode);
                memberQueue.offer(currentMember.getRightLeg());
                nodeQueue.offer(rightNode);
            }
        }

        return rootNode;
    }

    public MemberNodeResponse findGenealogyTree3Levels(String memberId) {
        Member member = findMemberByMemberId(memberId);
        MemberNodeResponse rootNode = new MemberNodeResponse();

        Queue<Member> memberQueue = new LinkedList<>();
        Queue<MemberNodeResponse> nodeQueue = new LinkedList<>();
        Queue<Integer> levelQueue = new LinkedList<>();
        memberQueue.offer(member);
        nodeQueue.offer(rootNode);
        levelQueue.offer(1);

        int currentId = 1;
        rootNode.setId(currentId++);

        while (!memberQueue.isEmpty()) {
            Member currentMember = memberQueue.poll();
            MemberNodeResponse currentNode = nodeQueue.poll();
            int currentLevel = levelQueue.poll();

            assert currentNode != null;
            setNodeProperties(currentNode, currentMember);

            if (currentLevel <= 3) {
                if (currentMember.getLeftLeg() != null) {
                    MemberNodeResponse leftNode = new MemberNodeResponse();
                    leftNode.setLevel(currentLevel);
                    leftNode.setId(currentId++);
                    currentNode.setLeft(leftNode);
                    memberQueue.offer(currentMember.getLeftLeg());
                    nodeQueue.offer(leftNode);
                    levelQueue.offer(currentLevel + 1);
                }

                if (currentMember.getRightLeg() != null) {
                    MemberNodeResponse rightNode = new MemberNodeResponse();
                    rightNode.setLevel(currentLevel);
                    rightNode.setId(currentId++);
                    currentNode.setRight(rightNode);
                    memberQueue.offer(currentMember.getRightLeg());
                    nodeQueue.offer(rightNode);
                    levelQueue.offer(currentLevel + 1);
                }
            }
        }

        return rootNode;
    }

    public Member activatePackageById(String memberId, int packageId, String storeId, String txnReference) throws MessagingException {
        paymentService.checkPaymentValidity(txnReference);

        if (Objects.equals(storeId, "null") || storeId == null) {
            storeId = null;
        }
        Member member = findMemberByMemberId(memberId);
        Member store = memberRepository.findByMemberId(storeId);
        Package newPackage = packageService.findPackageById(packageId);

        List<Order> activateOrders = orderRepository.findByMemberAndOrderType(member, OrderType.ACTIVATE_PACKAGE);
        if (!activateOrders.isEmpty()) {
           for (Order order : activateOrders) {
               if (order.getTransaction().getStatus() == TransactionStatus.NOT_CONFIRMED) {
                  throw new BadRequestException(ErrorMessages.CANNOT_ACTIVATE_PACKAGE_AGAIN);
               }
           }
        }

        validateSponsorAndPlacer(member);
        validatePackage(newPackage);

        UserRoleType roleType = determineUserRoleType();

        if (roleType == UserRoleType.REGULAR_MEMBER ||
                roleType == UserRoleType.SERVICE_CENTER ||
                roleType == UserRoleType.PREMIUM_STORE) {
            if (member.getCurrentPackage() == null) {
                activateNewPackage(member, store, newPackage, roleType);
            } else {
                upgradePackage(member, store, newPackage, roleType);
            }
        } else {
            throw new BadRequestException(ErrorMessages.INVALID_OPERATION);
        }

        member.setLastActive(LocalDateTime.now());
        return memberRepository.save(member);
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

    private void activateNewPackage(Member member, Member store, Package newPackage, UserRoleType roleType) throws MessagingException {
        double packagePrice = roundToTwoDecimalPlaces(newPackage.getPrice());

        if (roleType == UserRoleType.PREMIUM_STORE) {
            if (store == null) {
                PackageOrderItem orderItem = createPackageOrderItem(newPackage, 1);
                Order order = createOrder(member, null, packagePrice, orderItem, OrderType.ACTIVATE_PACKAGE);

                String txId = TransactionUtils.generateTransactionId();
                createTransaction(
                        txId,
                        member.getMemberId(),
                        "You requested to activate a package!",
                        order.getOrderId(),
                        packagePrice,
                        TransactionType.ACTIVATE_PACKAGE,
                        TransactionStatus.NOT_CONFIRMED
                );
                Transaction transaction = createTransaction(
                        txId,
                        null,
                        "A request to activate a package!",
                        order.getOrderId(),
                        packagePrice,
                        TransactionType.ACTIVATE_PACKAGE,
                        TransactionStatus.NOT_CONFIRMED
                );

                order.setTransaction(transaction);
                orderRepository.save(order);
            } else {
                throw new BadRequestException(ErrorMessages.BUY_ONLY_FROM_COMPANY);
            }
        } else {
            if (store == null) {
                throw new NoSuchResourceException("Please buy from appropriate store!");
            }
            validateAndGetStorePackage(store, newPackage, roleType);

            PackageOrderItem orderItem = createPackageOrderItem(newPackage, 1);
            Order order = createOrder(member, store, packagePrice, orderItem, OrderType.ACTIVATE_PACKAGE);

            String txId = TransactionUtils.generateTransactionId();
            createTransaction(
                    txId,
                    member.getMemberId(),
                    "You requested to activate a package!",
                    order.getOrderId(),
                    packagePrice,
                    TransactionType.ACTIVATE_PACKAGE,
                    TransactionStatus.NOT_CONFIRMED
            );
            Transaction transaction = createTransaction(
                    txId,
                    store.getMemberId(),
                    "A request to activate a package!",
                    order.getOrderId(),
                    packagePrice,
                    TransactionType.ACTIVATE_PACKAGE,
                    TransactionStatus.NOT_CONFIRMED
            );

            order.setTransaction(transaction);
            orderRepository.save(order);

            sendOrderPackageEmail(member, order, store, txId, newPackage);
        }
    }

    private void sendOrderPackageEmail(Member member, Order order, Member store, String txId, Package aPackage) throws MessagingException {
        emailService.sendOrderConfirmationEmail(
                member,
                order,
                store != null ? store.getBusinessName() : "Topnivo",
                txId,
                aPackage.getBv(),
                aPackage.getPv(),
                aPackage.getPrice()
        );
    }

    private void upgradePackage(Member member, Member store, Package newPackage, UserRoleType roleType) throws MessagingException {
        double newPrice = roundToTwoDecimalPlaces(newPackage.getPrice());
        double currentPrice = roundToTwoDecimalPlaces(member.getCurrentPackage().getPrice());
        Package oldPackage = member.getCurrentPackage();

        if (newPrice == currentPrice) {
            throw new PackageDowngradeException(ErrorMessages.ALREADY_ON_PACKAGE);
        }
        if (newPrice < currentPrice) {
            throw new PackageDowngradeException(ErrorMessages.PACKAGE_DOWNGRADE);
        }

        double amountToPay = newPrice - currentPrice;
        double differenceInPv = newPackage.getPv() - oldPackage.getPv();
        double differenceInBv = newPackage.getBv() - oldPackage.getBv();

        if (roleType == UserRoleType.PREMIUM_STORE) {
            if (store == null) {
                PackageOrderItem orderItem = createPackageOrderItem(newPackage, 1);
                Order order = createOrder(member, null, amountToPay, orderItem, OrderType.UPGRADE_PACKAGE);

                String txId = TransactionUtils.generateTransactionId();
                createTransaction(
                        txId,
                        member.getMemberId(),
                        "You requested to upgrade a package!",
                        order.getOrderId(),
                        amountToPay,
                        TransactionType.UPGRADE_PACKAGE,
                        TransactionStatus.NOT_CONFIRMED
                );
                Transaction transaction = createTransaction(
                        txId,
                        null,
                        "A request to upgrade a package!",
                        order.getOrderId(),
                        amountToPay,
                        TransactionType.UPGRADE_PACKAGE,
                        TransactionStatus.NOT_CONFIRMED
                );

                order.setTransaction(transaction);
                orderRepository.save(order);

                sendOrderPackageEmail(member, order, store, txId, newPackage);
            } else {
                throw new BadRequestException(ErrorMessages.BUY_ONLY_FROM_COMPANY);
            }
        } else {
            if (store == null) {
                throw new NoSuchResourceException("Please buy from appropriate store!");
            }
            validateAndGetStorePackage(store, newPackage, roleType);

            PackageOrderItem orderItem = createPackageOrderItem(newPackage, 1);
            Order order = createOrder(member, store, amountToPay, orderItem, OrderType.UPGRADE_PACKAGE);

            String txId = TransactionUtils.generateTransactionId();
            createTransaction(
                    txId,
                    member.getMemberId(),
                    "You requested to upgrade a package!",
                    order.getOrderId(),
                    amountToPay,
                    TransactionType.UPGRADE_PACKAGE,
                    TransactionStatus.NOT_CONFIRMED
            );
            Transaction transaction = createTransaction(
                    txId,
                    store.getMemberId(),
                    "A request to upgrade a package!",
                    order.getOrderId(),
                    amountToPay,
                    TransactionType.UPGRADE_PACKAGE,
                    TransactionStatus.NOT_CONFIRMED
            );

            order.setTransaction(transaction);
            orderRepository.save(order);

            sendOrderPackageEmail(member, order, store, txId, newPackage);
        }
    }

    public Member buyPackageById(String memberId, int packageId, String storeId, int quantity, String txnReference) throws MessagingException {
        paymentService.checkPaymentValidity(txnReference);

        if (Objects.equals(storeId, "null") || storeId == null) {
            storeId = null;
        }
        Member member = findMemberByMemberId(memberId);
        Member store = memberRepository.findByMemberId(storeId);
        Package newPackage = packageService.findPackageById(packageId);

        if (storeId != null && store == null) {
            throw new NoSuchResourceException(ErrorMessages.NO_SUCH_MEMBER + " (Store)");
        }

        if (member.getCurrentPackage() == null) {
            throw new BadRequestException(ErrorMessages.NOT_ON_PACKAGE);
        }

        validateSponsorAndPlacer(member);
        validatePackage(newPackage);

        UserRoleType roleType = determineUserRoleType();

        if (roleType == UserRoleType.SERVICE_CENTER && store != null) {
            buyPackage(member, store, newPackage, quantity);
        } else if (roleType == UserRoleType.PREMIUM_STORE && store == null) {
            buyPackage(member, null, newPackage, quantity);
        } else {
            throw new BadRequestException(ErrorMessages.INVALID_OPERATION);
        }

        member.setLastActive(LocalDateTime.now());
        return memberRepository.save(member);
    }

    private void buyPackage(Member member, Member store, Package newPackage, int quantity) throws MessagingException {
        double totalPackagePrice = roundToTwoDecimalPlaces(newPackage.getPrice() * quantity);

        if (store == null) {
            // For premium store
            PackageOrderItem orderItem = createPackageOrderItem(newPackage, quantity);
            Order order = createOrder(member, null, totalPackagePrice, orderItem, OrderType.BUY_PACKAGE);

            String txId = TransactionUtils.generateTransactionId();
            createTransaction(
                    txId,
                    member.getMemberId(),
                    "You requested to buy packages!",
                    order.getOrderId(),
                    totalPackagePrice,
                    TransactionType.BUY_PACKAGE,
                    TransactionStatus.NOT_CONFIRMED
            );
            Transaction transaction = createTransaction(
                    txId,
                    null,
                    "A request to buy packages!",
                    order.getOrderId(),
                    totalPackagePrice,
                    TransactionType.BUY_PACKAGE,
                    TransactionStatus.NOT_CONFIRMED
            );

            order.setTransaction(transaction);
            orderRepository.save(order);

            sendOrderPackageEmail(member, order, store, txId, newPackage);
        } else {
            // For service center
            StorePackage storePackage = storePackageRepository.findByStoreAndPac(store, newPackage);
            if (storePackage == null) {
                throw new NoSuchResourceException(ErrorMessages.NO_SUCH_PACKAGE_IN_STORE);
            }
            if (storePackage.getBoughtQuantity() == 0 || storePackage.getBoughtQuantity() < quantity) {
                throw new BadRequestException(ErrorMessages.INSUFFICIENT_PACKAGE_IN_STOCK);
            }

            PackageOrderItem orderItem = createPackageOrderItem(newPackage, quantity);
            Order order = createOrder(member, store, totalPackagePrice, orderItem, OrderType.BUY_PACKAGE);

            String txId = TransactionUtils.generateTransactionId();
            createTransaction(
                    txId,
                    member.getMemberId(),
                    "You requested to buy packages!",
                    order.getOrderId(),
                    totalPackagePrice,
                    TransactionType.BUY_PACKAGE,
                    TransactionStatus.NOT_CONFIRMED
            );
            Transaction transaction = createTransaction(
                    txId,
                    store.getMemberId(),
                    "A request to buy packages!",
                    order.getOrderId(),
                    totalPackagePrice,
                    TransactionType.BUY_PACKAGE,
                    TransactionStatus.NOT_CONFIRMED
            );

            order.setTransaction(transaction);
            orderRepository.save(order);

            sendOrderPackageEmail(member, order, store, txId, newPackage);
        }

        memberRepository.save(member);
    }

    public Member confirmOrderById(String memberId, String orderId, String status) {
        Member member = findMemberByMemberId(memberId);
        Order order = orderRepository.findByOrderId(orderId);
        if (order == null) {
            throw new NoSuchResourceException(ErrorMessages.NO_SUCH_ORDER);
        }

        if (!List.of("CONFIRMED", "NOT_CONFIRMED", "DECLINED", "REFUNDED").contains(status)) {
            throw new BadRequestException(ErrorMessages.INVALID_TRANSACTION_PARAMETER);
        }

        if (order.getTransaction().getStatus().name().equals("CONFIRMED")) {
            throw new BadRequestException(ErrorMessages.CANNOT_NO_LONGER_PERFORM_ACTION);
        }

        Member buyer = order.getMember();
        Member store = order.getStore();
        UserRoleType storeRoleType = determineUserRoleType();
        Member boughtFromStore = null;

        if (store == null) {
            if (storeRoleType != UserRoleType.PREMIUM_STORE) {
                throw new BadRequestException(ErrorMessages.INVALID_OPERATION);
            }
        } else {
            if (!store.getMemberId().equals(memberId)) {
                throw new BadRequestException(ErrorMessages.INVALID_OPERATION);
            }
        }

        if (status.equals("CONFIRMED")) {
            if (order.getOrderType() == OrderType.ACTIVATE_PACKAGE ||
                    order.getOrderType() == OrderType.UPGRADE_PACKAGE ||
                    order.getOrderType() == OrderType.BUY_PACKAGE
            ) {
                OrderItem orderItem = order.getOrderItems().get(0);
                Package newPackage = packageService.findPackageById(Integer.parseInt(orderItem.getOriginalId()));
                Package oldPackage = buyer.getCurrentPackage();
                double bv;
                double pv;

                if (storeRoleType == UserRoleType.ADMIN ||
                        storeRoleType == UserRoleType.PREMIUM_STORE ||
                        storeRoleType == UserRoleType.SERVICE_CENTER) {

                    StorePackage storePackage = storePackageRepository.findByStoreAndPac(store, newPackage);

                    if (storePackage != null) {
                        boughtFromStore = storePackage.getBoughtFromStore();
                    }

                    if (order.getOrderType() == OrderType.ACTIVATE_PACKAGE) {
                        bv = newPackage.getBv();
                        pv = newPackage.getPv();

                        buyer.setCurrentPackage(newPackage);
                        commissionService.updateSponsorNewRegistrationCount(buyer);
                        commissionService.sendDirectAndIndirectReferralCommission(buyer, pv);

                        if (storeRoleType != UserRoleType.ADMIN) {
                            storePackage.setBoughtQuantity(storePackage.getBoughtQuantity() - 1);
                            storePackageRepository.save(storePackage);
                        }

                    } else if (order.getOrderType() == OrderType.UPGRADE_PACKAGE) {
                        bv = newPackage.getBv() - oldPackage.getBv();
                        pv = newPackage.getPv() - oldPackage.getPv();

                        buyer.setCurrentPackage(newPackage);
                        commissionService.sendDirectAndIndirectReferralCommission(buyer, pv);

                        if (storeRoleType != UserRoleType.ADMIN) {
                            storePackage.setBoughtQuantity(storePackage.getBoughtQuantity() - 1);
                            storePackageRepository.save(storePackage);
                        }
                    } else {
                        bv = newPackage.getBv() * orderItem.getQuantity();
                        pv = newPackage.getPv() * orderItem.getQuantity();

                        StorePackage oldStorePackage = storePackageRepository.findByStoreAndPac(buyer, newPackage);
                        if (oldStorePackage == null) {
                            StorePackage newStorePackage = new StorePackage();
                            newStorePackage.setPac(newPackage);
                            newStorePackage.setStore(buyer);
                            newStorePackage.setBoughtQuantity(orderItem.getQuantity());
                            newStorePackage.setBoughtFromStore(storePackage != null ? storePackage.getStore() : null);
                            storePackageRepository.save(newStorePackage);
                        } else {
                            oldStorePackage.setBoughtQuantity(oldStorePackage.getBoughtQuantity() + orderItem.getQuantity());
                            storePackageRepository.save(oldStorePackage);
                        }

                        if (storeRoleType != UserRoleType.ADMIN) {
                            storePackage.setBoughtQuantity(storePackage.getBoughtQuantity() - orderItem.getQuantity());
                            storePackageRepository.save(storePackage);
                        }
                    }

                    if (storeRoleType == UserRoleType.PREMIUM_STORE) {
                        commissionService.sendPremiumStoreBonus(store, pv);
                    } else if (storeRoleType == UserRoleType.SERVICE_CENTER) {
                        commissionService.sendServiceCenterBonus(store, boughtFromStore, pv);
                    }

                    commissionService.addBinaryBvAndPvToAllUpLines(buyer, bv, pv);
                } else {
                    throw new BadRequestException(ErrorMessages.INVALID_OPERATION);
                }
            } else if (order.getOrderType() == OrderType.BUY_PRODUCT) {
                List<OrderItem> orderItems = order.getOrderItems();
                double bv = orderItems.stream().mapToDouble(o -> o.getBv() * o.getQuantity()).sum();
                double pv = orderItems.stream().mapToDouble(o -> o.getPv() * o.getQuantity()).sum();

                if (storeRoleType == UserRoleType.ADMIN) {
                    for (OrderItem o : orderItems) {
                        ProductOrderItem orderItem = (ProductOrderItem) o;
                        StoreProduct buyerStoreProduct = storeProductRepository.findProductFromStore(buyer.getMemberId(),
                                orderItem.getOriginalId());
                        Product product = productRepository.findById(orderItem.getOriginalId()).get();

                        if (buyerStoreProduct == null) {
                            StoreProduct newStoreProduct = new StoreProduct();
                            newStoreProduct.setProduct(product);
                            newStoreProduct.setBoughtQuantity(orderItem.getQuantity());
                            newStoreProduct.setStore(buyer);
                            newStoreProduct.setBoughtFromStore(null);
                            storeProductRepository.save(newStoreProduct);
                        } else {
                            buyerStoreProduct.setBoughtQuantity(buyerStoreProduct.getBoughtQuantity() + orderItem.getQuantity());
                            storeProductRepository.save(buyerStoreProduct);
                        }

                        product.setAvailableQuantity(product.getAvailableQuantity() - orderItem.getQuantity());
                        productRepository.save(product);
                    }
                } else if (storeRoleType == UserRoleType.PREMIUM_STORE) {
                    for (OrderItem o : orderItems) {
                        ProductOrderItem orderItem = (ProductOrderItem) o;
                        StoreProduct sellerStoreProduct = storeProductRepository.findProductFromStore(orderItem.getSellerId(),
                                orderItem.getOriginalId());
                        StoreProduct buyerStoreProduct = storeProductRepository.findProductFromStore(buyer.getMemberId(),
                                orderItem.getOriginalId());

                        if (buyerStoreProduct == null) {
                            Product product = productRepository.findById(orderItem.getOriginalId()).get();

                            StoreProduct newStoreProduct = new StoreProduct();
                            newStoreProduct.setProduct(product);
                            newStoreProduct.setBoughtQuantity(orderItem.getQuantity());
                            newStoreProduct.setStore(buyer);
                            newStoreProduct.setBoughtFromStore(store);
                            storeProductRepository.save(newStoreProduct);
                        } else {
                            buyerStoreProduct.setBoughtQuantity(buyerStoreProduct.getBoughtQuantity() + orderItem.getQuantity());
                            storeProductRepository.save(buyerStoreProduct);
                        }

                        if (sellerStoreProduct != null) {
                            sellerStoreProduct.setBoughtQuantity(sellerStoreProduct.getBoughtQuantity() - orderItem.getQuantity());
                            storeProductRepository.save(sellerStoreProduct);
                        }
                    }

                    commissionService.sendPremiumStoreBonus(store, pv);
                } else if (storeRoleType == UserRoleType.SERVICE_CENTER) {
                    for (OrderItem o : orderItems) {
                        ProductOrderItem orderItem = (ProductOrderItem) o;
                        StoreProduct sellerStoreProduct = storeProductRepository.findProductFromStore(orderItem.getSellerId(),
                                orderItem.getOriginalId());
                        boughtFromStore = sellerStoreProduct.getBoughtFromStore();
                        sellerStoreProduct.setBoughtQuantity(sellerStoreProduct.getBoughtQuantity() - orderItem.getQuantity());
                        storeProductRepository.save(sellerStoreProduct);
                    }

                    // Send pv, bv and uniLevel commission
                    // 20% to binary, 80% to uni level bonus
                    double toUpLine = (20 / 100.0) * pv;
                    double uniLevelPv = pv - toUpLine;

                    commissionService.addBinaryBvAndPvToAllUpLines(buyer, bv, toUpLine);
                    commissionService.sendServiceCenterBonus(store, boughtFromStore, pv);
                    commissionService.sendUniLevelCommission(buyer, uniLevelPv);
                }
            }

            List<Transaction> transactions = transactionRepository.findByOrderId(orderId);
            transactions.forEach(transaction -> {
                transaction.setStatus(TransactionStatus.CONFIRMED);
                transactionRepository.save(transaction);
            });

        } else {
            TransactionStatus newStatus = TransactionStatus.valueOf(status);

            List<Transaction> transactions = transactionRepository.findByOrderId(orderId);
            transactions.forEach(transaction -> {
                transaction.setStatus(newStatus);
                transactionRepository.save(transaction);
            });
        }

        memberRepository.save(buyer);

        return memberRepository.save(member);
    }

    private UserRoleType determineUserRoleType() {
        var authorities = SecurityContextHolder.getContext().getAuthentication().getAuthorities();

        String role = authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .filter(r -> !r.equals("ROLE_REGULAR_MEMBER"))
                .findFirst()
                .orElse("ROLE_REGULAR_MEMBER");

        return UserRoleType.fromString(role);
    }

    public Member updateMemberCanReceivePayment(String memberId, boolean canReceivePayment) {
        Member member = findMemberByMemberId(memberId);
        member.setCanReceivePayment(canReceivePayment);

        return memberRepository.save(member);
    }

    public Map<String, Object> loginAsUser(String memberId, String adminId) {
        Member member = findMemberByMemberId(memberId);
        findMemberByMemberId(adminId);
        String newJwt = jwtService.generateAccessToken(member.getUsername());
        MemberResponse memberResponse = memberMapper.memberToResponse(member);

        Map<String, Object> returnValue = Map.of(
                "member", memberResponse,
                "token", newJwt
        );

        return returnValue;
    }

    public Member adminUpdateMemberInfo(String memberId, MemberCreateRequest request) {
        Member member = findMemberByMemberId(memberId);

        if (request.getLastName() != null) {
            member.setLastName(request.getLastName());
        }
        if (request.getFirstName() != null) {
            member.setFirstName(request.getFirstName());
        }
        if (request.getEmail() != null) {
            member.setEmail(request.getEmail());
        }
        if (request.getPhoneNumber() != null) {
            member.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getAddress() != null) {
            member.setAddress(request.getAddress());
        }

        return memberRepository.save(member);
    }

    public enum UserRoleType {
        REGULAR_MEMBER,
        SERVICE_CENTER,
        PREMIUM_STORE,
        ADMIN,
        SUPER_ADMIN,
        UNKNOWN;

        public static UserRoleType fromString(String role) {
            return switch (role) {
                case "ROLE_REGULAR_MEMBER" -> REGULAR_MEMBER;
                case "ROLE_SERVICE_CENTER" -> SERVICE_CENTER;
                case "ROLE_PREMIUM_STORE" -> PREMIUM_STORE;
                case "ROLE_ADMIN" -> ADMIN;
                case "ROLE_SUPER_ADMIN" -> SUPER_ADMIN;
                default -> UNKNOWN;
            };
        }
    }

    private void validateMemberPassword(Member member, String password) {
        if (!passwordEncoder.matches(password, member.getPassword())) {
            throw new InvalidCredentialException(ErrorMessages.INCORRECT_PASSWORD);
        }
    }

    private void validateSponsorAndPlacer(Member member) {
        if (member.getSponsor() == null || member.getPlacer() == null) {
            throw new BadRequestException(ErrorMessages.NO_SPONSOR_AND_PLACER);
        }
    }

    private Order createOrder(Member member, Member store, double packagePrice, PackageOrderItem orderItem, OrderType orderType) {
        Order newOrder = new Order();
        newOrder.setOrderId(OrderUtils.generateOrder(8));
        newOrder.setOrderType(orderType);
        newOrder.setOrderStatus(OrderStatus.PENDING);
        newOrder.setOrderDate(LocalDateTime.now());
        newOrder.setAddress(null);
        newOrder.setMember(member);
        newOrder.setStore(store);
        newOrder.setTotalAmount(packagePrice);

        newOrder = orderRepository.save(newOrder);
        orderItem.setOrder(newOrder);
        newOrder.getOrderItems().add(orderItem);

        return orderRepository.save(newOrder);
    }

    private PackageOrderItem createPackageOrderItem(Package newPackage, int quantity) {
        PackageOrderItem orderItem = new PackageOrderItem();
        orderItem.setOriginalId(newPackage.getId() + "");
        orderItem.setName(newPackage.getName());
        orderItem.setDescription(newPackage.getDescription());
        orderItem.setBv(newPackage.getBv());
        orderItem.setPv(newPackage.getPv());
        orderItem.setImage(newPackage.getImage());
        orderItem.setQuantity(quantity);
        orderItem.setPrice(roundToTwoDecimalPlaces(newPackage.getPrice()));
        orderItem.setPackageItems(newPackage.getPackageItems());
        orderItem.setDailyCapping(newPackage.getDailyCapping());
        orderItem.setBinaryCommissionRate(newPackage.getBinaryCommissionRate());
        orderItem.setDirectCommissionRate(newPackage.getDirectCommissionRate());
        return orderItem;
    }

    private StorePackage validateAndGetStorePackage(Member store, Package newPackage, UserRoleType roleType) {
        if (store == null) {
            throw new BadRequestException(ErrorMessages.NO_SUCH_STORE);
        }

        String storeRole = store.getRoles().stream()
                .map(Role::getName)
                .filter(r -> !r.equals("ROLE_REGULAR_MEMBER"))
                .findFirst()
                .orElse("ROLE_REGULAR_MEMBER");

        if (roleType == UserRoleType.REGULAR_MEMBER && !storeRole.equals("ROLE_SERVICE_CENTER")) {
            throw new BadRequestException(ErrorMessages.BUY_ONLY_FROM_SERVICE_CENTER);
        }

        if (roleType == UserRoleType.SERVICE_CENTER && !storeRole.equals("ROLE_PREMIUM_STORE")) {
            throw new BadRequestException(ErrorMessages.BUY_ONLY_FROM_PREMIUM_STORE);
        }

        StorePackage storePackage = storePackageRepository.findByStoreAndPac(store, newPackage);

        if (storePackage == null) {
            throw new BadRequestException(ErrorMessages.NO_SUCH_PACKAGE_IN_STORE);
        }

        if (storePackage.getBoughtQuantity() == 0) {
            throw new BadRequestException(ErrorMessages.INSUFFICIENT_PACKAGE_IN_STOCK);
        }
        return storePackage;
    }

    private double roundToTwoDecimalPlaces(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    public Member addFundsToMember(String memberId, TransferRequest transferRequest) {
        Member fromMember = findMemberByMemberId(memberId);
        Member toMember = findMemberByUsername(transferRequest.getUsername());

        if (fromMember.getAvailableBalance() >= transferRequest.getAmount()) {
            fromMember.setAvailableBalance(fromMember.getAvailableBalance() - transferRequest.getAmount());
            toMember.setAvailableBalance(toMember.getAvailableBalance() + transferRequest.getAmount());

            memberRepository.save(toMember);

            UserRoleType userRoleType = determineUserRoleType();

            String fromId = fromMember.getMemberId();

            if (userRoleType == UserRoleType.SUPER_ADMIN || userRoleType == UserRoleType.ADMIN) {
                fromId = null;
            }

            createTransferTransaction(fromId,
                    transferRequest.getAmount(),
                    "You transferred %f to %s.".formatted(transferRequest.getAmount(), toMember.getUsername()));

            createTransferTransaction(toMember.getMemberId(),
                    transferRequest.getAmount(),
                    "You received %f from %s.".formatted(transferRequest.getAmount(), fromMember.getUsername()));

            return memberRepository.save(fromMember);
        }
        else {
            throw new InsufficientBalanceException(ErrorMessages.INSUFFICIENT_FUNDS);
        }
    }

    private void createTransferTransaction(String memberId, double amount, String message) {
        Transaction transaction = new Transaction();
        transaction.setMemberId(memberId);
        transaction.setTransactionDate(LocalDateTime.now());
        transaction.setAmount(amount);
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setType(TransactionType.INTERNAL_TRANSFER);
        transaction.setMessage(message);
        transaction.setReferenceId(TransactionUtils.generateTransactionId());

        transactionRepository.save(transaction);
    }

    public Member changePassword(String memberId, ChangePasswordRequest request) {
        Member member = findMemberByMemberId(memberId);

        if (request.getOldPassword() != null && request.getNewPassword() != null && request.getConfirmNewPassword() != null) {
            if (!request.getNewPassword().equals(request.getConfirmNewPassword())) {
                throw new InvalidCredentialException(ErrorMessages.UNMATCHED_NEW_PASSWORDS);
            }
            if (!passwordEncoder.matches(request.getOldPassword(), member.getPassword())) {
                throw new InvalidCredentialException(ErrorMessages.INCORRECT_OLD_PASSWORD);
            }

            member.setPassword(passwordEncoder.encode(request.getNewPassword()));

            return memberRepository.save(member);
        } else {
            throw new InvalidCredentialException(ErrorMessages.MISSING_VALUES);
        }
    }

    @Transactional
    public Member forgotPassword(String username) throws MessagingException {
        Member member = findMemberByUsername(username);
        String memberName = member.getLastName() + " " + member.getFirstName();
        String memberEmail = member.getEmail();
        String newPassword = PasswordUtils.generatePassword(8);
        member.setPassword(passwordEncoder.encode(newPassword));

        // Send Email to member
        emailService.sendForgotPasswordEmail(memberName, memberEmail, newPassword);

        return memberRepository.save(member);
    }

    public Member addStoreInfo(String memberId, StoreInfoRequest request) {
        Member member = findMemberByMemberId(memberId);
        member.setBusinessName(request.getBusinessName());
        member.setAddress(request.getAddress());

        return memberRepository.save(member);
    }

    public Member addAccountDetails(String memberId, AccountDetailsRequest request) {
        Member member = findMemberByMemberId(memberId);
        AccountDetails accountDetails = AccountDetails.builder()
                .accountNumber(request.getAccountNumber())
                .accountName(request.getAccountName())
                .bankCode(request.getBankCode())
                .bankName(request.getBankName())
                .bankType(request.getBankType())
                .currency(request.getCurrency())
                .build();

        member.setAccountDetails(accountDetails);
        return memberRepository.save(member);
    }

    public Map<String, Object> showMemberAnalysis(String memberId) {
        Member member = findMemberByMemberId(memberId);
        MemberResponse memberResponse = memberMapper.memberToResponse(member);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("member", memberResponse);
        result.put("sponsor", getMemberLittleResponse(memberResponse.getSponsorId()));
        result.put("placer", getMemberLittleResponse(memberResponse.getPlacerId()));
        result.put("leftLeg", getMemberLittleResponse(memberResponse.getLeftLegId()));
        result.put("rightLeg", getMemberLittleResponse(memberResponse.getRightLegId()));
        result.put("promos", getEarnedPromotions(member));

        return result;
    }

    public Page<Member> findMemberByPageAndRole(int page, int size, String search, String role, String sortByBv, String enabled) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("registeredOn").descending());

        if (!List.of("ALL", "HIGHEST", "LOWEST").contains(sortByBv)) {
            throw new BadRequestException(ErrorMessages.NO_SUCH_SORT_BV);
        }

        if (!List.of("ALL", "TRUE", "FALSE").contains(enabled)) {
            throw new BadRequestException(ErrorMessages.NO_SUCH_ENABLED_OPTION);
        }

        boolean isEnabled = enabled.equals("TRUE");

        if (role.equals("ALL")) {
            if (search.isEmpty()) {
                if (sortByBv.equals("ALL") && enabled.equals("ALL")) {
                    return memberRepository.findAll(pageable);
                } else if (!sortByBv.equals("ALL") && !enabled.equals("ALL")) {
                    // sort by both
                    if (sortByBv.equals("HIGHEST")) {
                        return memberRepository.findAllByTotalBvAndEnabledSortDesc(isEnabled, pageable);
                    } else {
                        return memberRepository.findAllByTotalBvAndEnabledSortAsc(isEnabled, pageable);
                    }
                } else if (!sortByBv.equals("ALL")) {
                    if (sortByBv.equals("HIGHEST")) {
                        return memberRepository.findAllByTotalBvSortDesc(pageable);
                    } else {
                        return memberRepository.findAllByTotalBvSortAsc(pageable);
                    }
                } else {
                    // sort by enable
                    return memberRepository.findAllByEnabled(isEnabled, pageable);
                }
            } else {
                if (sortByBv.equals("ALL") && enabled.equals("ALL")) {
                    return memberRepository.findByEmailOrUserNameOrFirstNameOrLastName(search, pageable);
                } else if (!sortByBv.equals("ALL") && !enabled.equals("ALL")) {
                    // sort by both
                    if (sortByBv.equals("HIGHEST")) {
                        return memberRepository.findByEmailOrUserNameOrFirstNameOrLastNameAndSortByBvAndEnabledDesc(search, isEnabled, pageable);
                    } else {
                        return memberRepository.findByEmailOrUserNameOrFirstNameOrLastNameAndSortByBvAndEnabledAsc(search, isEnabled, pageable);
                    }
                } else if (!sortByBv.equals("ALL")) {
                    if (sortByBv.equals("HIGHEST")) {
                        return memberRepository.findByEmailOrUserNameOrFirstNameOrLastNameAndSortByBvDesc(search, pageable);
                    } else {
                        return memberRepository.findByEmailOrUserNameOrFirstNameOrLastNameAndSortByBvAsc(search, pageable);
                    }
                } else {
                    // sort by enable
                    return memberRepository.findByEmailOrUserNameOrFirstNameOrLastNameAndIsEnabled(search, isEnabled, pageable);
                }
            }
        } else {
            MemberType memberType = null;
            try {
                memberType = MemberType.valueOf(role);
            } catch (Exception ex) {
                throw new BadRequestException(ErrorMessages.NO_SUCH_MEMBER_TYPE);
            }

            if (sortByBv.equals("ALL") && enabled.equals("ALL")) {
                return memberRepository.findByEmailOrUserNameOrFirstNameOrLastNameAndRole(search, "ROLE_" + memberType.name(), pageable);
            } else if (!sortByBv.equals("ALL") && !enabled.equals("ALL")) {
                // sort by both
                if (sortByBv.equals("HIGHEST")) {
                    return memberRepository.findByEmailOrUserNameOrFirstNameOrLastNameAndRoleAndSortByBvAndEnabledDesc(search, "ROLE_" + memberType.name(), isEnabled, pageable);
                } else {
                    return memberRepository.findByEmailOrUserNameOrFirstNameOrLastNameAndRoleAndSortByBvAndEnabledAsc(search, "ROLE_" + memberType.name(), isEnabled, pageable);
                }
            } else if (!sortByBv.equals("ALL")) {
                if (sortByBv.equals("HIGHEST")) {
                    return memberRepository.findByEmailOrUserNameOrFirstNameOrLastNameAndRoleAndSortByBvDesc(search, "ROLE_" + memberType.name(), pageable);
                } else {
                    return memberRepository.findByEmailOrUserNameOrFirstNameOrLastNameAndRoleAndSortByBvAsc(search, "ROLE_" + memberType.name(), pageable);
                }
            } else {
                // sort by enable
                return memberRepository.findByEmailOrUserNameOrFirstNameOrLastNameAndRoleAndEnabled(search, "ROLE_" + memberType.name(), isEnabled, pageable);
            }
        }
    }

    private MemberLittleResponse getMemberLittleResponse(String memberId) {
        if (memberId == null) return null;
        Member member = memberRepository.findByMemberId(memberId);
        return member != null ? memberMapper.memberToLittleResponse(member) : null;
    }

    private List<EarnedPromotion> getEarnedPromotions(Member member) {
        List<EarnedPromotion> earnedPromotions = earnedPromotionRepository.findByMember(member);
        earnedPromotions.forEach(p -> p.setMember(null));
        return earnedPromotions;
    }

    private Page<Member> buildMemberPage(String search, MemberType memberType, String sortByBv, boolean isEnabled, Pageable pageable) {
        if (memberType == null) {
            return search.isEmpty() ?
                    getMembersWithoutRoleFilter(sortByBv, isEnabled, pageable) :
                    getMembersWithSearchWithoutRole(search, sortByBv, isEnabled, pageable);
        } else {
            return search.isEmpty() ?
                    getMembersWithRoleFilter(memberType, sortByBv, isEnabled, pageable) :
                    getMembersWithSearchAndRole(search, memberType, sortByBv, isEnabled, pageable);
        }
    }

    private Page<Member> getMembersWithoutRoleFilter(String sortByBv, boolean isEnabled, Pageable pageable) {
        if (sortByBv.equals("ALL") && isEnabled) {
            return memberRepository.findAll(pageable);
        } else if (!sortByBv.equals("ALL") && isEnabled) {
            return sortByBv.equals("HIGHEST") ?
                    memberRepository.findAllByTotalBvAndEnabledSortDesc(isEnabled, pageable) :
                    memberRepository.findAllByTotalBvAndEnabledSortAsc(isEnabled, pageable);
        } else if (!sortByBv.equals("ALL")) {
            return sortByBv.equals("HIGHEST") ?
                    memberRepository.findAllByTotalBvSortDesc(pageable) :
                    memberRepository.findAllByTotalBvSortAsc(pageable);
        } else {
            return memberRepository.findAllByEnabled(isEnabled, pageable);
        }
    }

    private void validateSortParameters(String sortByBv, String enabled) {
        if (!List.of("ALL", "HIGHEST", "LOWEST").contains(sortByBv)) {
            throw new BadRequestException(ErrorMessages.NO_SUCH_SORT_BV);
        }
        if (!List.of("ALL", "TRUE", "FALSE").contains(enabled)) {
            throw new BadRequestException(ErrorMessages.NO_SUCH_ENABLED_OPTION);
        }
    }

    private void setNodeProperties(MemberNodeResponse node, Member member) {
        node.setFirstName(member.getFirstName());
        node.setLastName(member.getLastName());
        node.setUsername(member.getUsername());
        node.setMemberId(member.getMemberId());
        node.setEnabled(member.isEnabled());
        node.setSponsor(member.getSponsor() != null ? member.getSponsor().getUsername() : null);
        node.setLeftBv(member.getTotalLeftBv());
        node.setRightBv(member.getTotalRightBv());
        node.setLeftPv(member.getBinaryLeftPv());
        node.setRightPv(member.getBinaryRightPv());
        node.setRank(member.getRank() != null ? member.getRank().getName() : "No Rank");
    }

    private void collectAllDownLines(Member member, List<Member> downLines) {
        if (member == null) return;

        if (member.getLeftLeg() != null) {
            downLines.add(member.getLeftLeg());
            collectAllDownLines(member.getLeftLeg(), downLines);
        }

        if (member.getRightLeg() != null) {
            downLines.add(member.getRightLeg());
            collectAllDownLines(member.getRightLeg(), downLines);
        }
    }

    private void validatePackage(Package pkg) {
        if (pkg == null) {
            throw new NoSuchResourceException(ErrorMessages.NO_SUCH_PACKAGE);
        }
    }

    public Member addSponsorAndPlacer(String memberId, String sponsorUsername, String placerUsername, String leg) {
        MemberLeg memberLeg = validateLegParameter(leg);
        Member member = findMemberByMemberId(memberId);
        Member sponsor = findMemberByUsername(sponsorUsername);
        Member placer = findMemberByUsername(placerUsername);

//        if (sponsor.getId() != placer.getId()) {
//            validateSponsorPlacerRelationship(sponsor, placer);
//        }

        validatePlacerLegAvailability(placer, memberLeg);

        member.setSponsor(sponsor);
        member.setPlacer(placer);

        if (memberLeg == MemberLeg.LEFT) {
            placer.setLeftLeg(member);
        } else {
            placer.setRightLeg(member);
        }

        memberRepository.save(placer);
        return memberRepository.save(member);
    }

    private MemberLeg validateLegParameter(String leg) {
        try {
            return MemberLeg.valueOf(leg.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(ErrorMessages.INVALID_LEG);
        }
    }

    private void validateSponsorPlacerRelationship(Member sponsor, Member placer) {
        if (!isPlacerDownlineOfSponsor(sponsor, placer)) {
            throw new BadRequestException(ErrorMessages.PLACER_NOT_SPONSOR_DOWNLINE);
        }
    }

    private void validatePlacerLegAvailability(Member placer, MemberLeg leg) {
        if (isPlacerLegsFull(placer)) {
            throw new BadRequestException(ErrorMessages.PLACER_LEG_FULL);
        }

        if (leg == MemberLeg.LEFT && placer.getLeftLeg() != null) {
            throw new BadRequestException(ErrorMessages.LEFT_LEG_OCCUPIED);
        }

        if (leg == MemberLeg.RIGHT && placer.getRightLeg() != null) {
            throw new BadRequestException(ErrorMessages.RIGHT_LEG_OCCUPIED);
        }
    }

    private Page<Member> getMembersWithSearchWithoutRole(String search, String sortByBv, boolean isEnabled, Pageable pageable) {
        if (sortByBv.equals("ALL") && isEnabled) {
            return memberRepository.findByEmailOrUserNameOrFirstNameOrLastName(search, pageable);
        } else if (!sortByBv.equals("ALL") && isEnabled) {
            return sortByBv.equals("HIGHEST") ?
                    memberRepository.findByEmailOrUserNameOrFirstNameOrLastNameAndSortByBvAndEnabledDesc(search, isEnabled, pageable) :
                    memberRepository.findByEmailOrUserNameOrFirstNameOrLastNameAndSortByBvAndEnabledAsc(search, isEnabled, pageable);
        } else if (!sortByBv.equals("ALL")) {
            return sortByBv.equals("HIGHEST") ?
                    memberRepository.findByEmailOrUserNameOrFirstNameOrLastNameAndSortByBvDesc(search, pageable) :
                    memberRepository.findByEmailOrUserNameOrFirstNameOrLastNameAndSortByBvAsc(search, pageable);
        } else {
            return memberRepository.findByEmailOrUserNameOrFirstNameOrLastNameAndIsEnabled(search, isEnabled, pageable);
        }
    }

    private Page<Member> getMembersWithRoleFilter(MemberType memberType, String sortByBv, boolean isEnabled, Pageable pageable) {
        if (sortByBv.equals("ALL") && isEnabled) {
            return memberRepository.findByRolesName("ROLE_" + memberType.name(), pageable);
        } else if (!sortByBv.equals("ALL") && isEnabled) {
            return sortByBv.equals("HIGHEST") ?
                    memberRepository.findByRolesNameAndSortByBvAndEnabledDesc("ROLE_" + memberType.name(), isEnabled, pageable) :
                    memberRepository.findByRolesNameAndSortByBvAndEnabledAsc("ROLE_" + memberType.name(), isEnabled, pageable);
        } else if (!sortByBv.equals("ALL")) {
            return sortByBv.equals("HIGHEST") ?
                    memberRepository.findByRolesNameAndSortByBvDesc("ROLE_" + memberType.name(), pageable) :
                    memberRepository.findByRolesNameAndSortByBvAsc("ROLE_" + memberType.name(), pageable);
        } else {
            return memberRepository.findByRolesNameAndEnabled("ROLE_" + memberType.name(), isEnabled, pageable);
        }
    }

    private Page<Member> getMembersWithSearchAndRole(String search, MemberType memberType, String sortByBv, boolean isEnabled, Pageable pageable) {
        if (sortByBv.equals("ALL") && isEnabled) {
            return memberRepository.findByEmailOrUserNameOrFirstNameOrLastNameAndRole(search, "ROLE_" + memberType.name(), pageable);
        } else if (!sortByBv.equals("ALL") && isEnabled) {
            return sortByBv.equals("HIGHEST") ?
                    memberRepository.findByEmailOrUserNameOrFirstNameOrLastNameAndRoleAndSortByBvAndEnabledDesc(search, "ROLE_" + memberType.name(), isEnabled, pageable) :
                    memberRepository.findByEmailOrUserNameOrFirstNameOrLastNameAndRoleAndSortByBvAndEnabledAsc(search, "ROLE_" + memberType.name(), isEnabled, pageable);
        } else if (!sortByBv.equals("ALL")) {
            return sortByBv.equals("HIGHEST") ?
                    memberRepository.findByEmailOrUserNameOrFirstNameOrLastNameAndRoleAndSortByBvDesc(search, "ROLE_" + memberType.name(), pageable) :
                    memberRepository.findByEmailOrUserNameOrFirstNameOrLastNameAndRoleAndSortByBvAsc(search, "ROLE_" + memberType.name(), pageable);
        } else {
            return memberRepository.findByEmailOrUserNameOrFirstNameOrLastNameAndRoleAndEnabled(search, "ROLE_" + memberType.name(), isEnabled, pageable);
        }
    }

    public boolean isPlacerDownlineOfSponsor(String _sponsor, String _placer) {
        if (_sponsor.equals(_placer)) {
            return true;
        }
        Member sponsor = findMemberByUsername(_sponsor);
        Member placer = findMemberByUsername(_placer);
        List<Member> downlines = findAllMemberDownLines(sponsor);
        return downlines.stream().anyMatch(d -> d.getId() == placer.getId());
    }

    private boolean isPlacerDownlineOfSponsor(Member sponsor, Member placer) {
        List<Member> downlines = findAllMemberDownLines(sponsor);
        return downlines.stream().anyMatch(d -> d.getId() == placer.getId());
    }

    private boolean isPlacerLegsFull(Member placer) {
        return placer.getLeftLeg() != null && placer.getRightLeg() != null;
    }

    public void test() {
        List<Member> members = memberRepository.findAll();
        List<Promotion> promotions = promotionRepository.findAll()
                .stream()
                .sorted(Comparator.comparingDouble(Promotion::getTargetPv))
                .collect(Collectors.toList());

        for (Member member : members) {
            for (Promotion promotion : promotions) {
                //createEarnedPromotion(member, promotion);
            }
        }
    }

    public boolean checkPlacerLeg(String username, String leg) {
        Member member = findMemberByUsername(username);
        if (!List.of("LEFT", "RIGHT").contains(leg)) {
            throw new BadRequestException(ErrorMessages.INVALID_LEG);
        }

        if (leg.equals("LEFT") && member.getLeftLeg() != null) {
            throw new BadRequestException(ErrorMessages.LEFT_LEG_OCCUPIED);
        }
        if (leg.equals("RIGHT") && member.getRightLeg() != null) {
            throw new BadRequestException(ErrorMessages.RIGHT_LEG_OCCUPIED);
        }

        return true;
    }
}