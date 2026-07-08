package com.topnivo.backend.controller;

import com.topnivo.backend.mapper.MemberMapper;
import com.topnivo.backend.model.constant.MemberLeg;
import com.topnivo.backend.model.entity.Member;
import com.topnivo.backend.model.entity.Order;
import com.topnivo.backend.model.request.*;
import com.topnivo.backend.model.response.*;
import com.topnivo.backend.model.response.ResponseStatus;
import com.topnivo.backend.repository.MemberRepository;
import com.topnivo.backend.service.CommissionService;
import com.topnivo.backend.service.MemberService;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping(value = "/api/v1/members")
@RequiredArgsConstructor
@Slf4j
public class MemberController {

    private final MemberService memberService;
    private final MemberMapper memberMapper;
    private final MemberRepository memberRepository;
    private final CommissionService commissionService;

    @PostMapping
    public ResponseEntity<?> createCustomer(
            @RequestBody MemberCreateRequest request,
            @RequestParam(value = "sponsor", required = false, defaultValue = "") String sponsor,
            @RequestParam(value = "placer", required = false, defaultValue = "") String placer,
            @RequestParam(value = "leg", required = false, defaultValue = "") MemberLeg leg

    ) throws MessagingException {
        Member member = memberService.createCustomer(request, sponsor, placer, leg);
        MemberResponse memberResponse = memberMapper.memberToResponse(member);
        ApiResponse<MemberResponse> response = new ApiResponse<>(
                ResponseStatus.CREATED.name(),
                "Member created successfully!",
                memberResponse,
                null
        );

        return ResponseEntity.ok(response);
    }

    @PutMapping(value = "/{memberId}/can-receive-payment")
    public ResponseEntity<?> updateMemberCanReceivePayment(
            @PathVariable(value = "memberId") String memberId,
            @RequestParam("canReceivePayment") boolean canReceivePayment
    ) throws IOException {
        Member member = memberService.updateMemberCanReceivePayment(memberId, canReceivePayment);
        MemberResponse memberResponse = memberMapper.memberToResponse(member);
        ApiResponse<MemberResponse> response = new ApiResponse<>(
                ResponseStatus.UPDATED.name(),
                "Member retrieved successfully!",
                memberResponse,
                null
        );

        return ResponseEntity.ok(response);
    }

    @PutMapping(value = "/{memberId}", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<?> uploadProfilePicture(
            @PathVariable(value = "memberId") String memberId,
            @RequestParam("file") MultipartFile multipartFile
            ) throws IOException {
        Member member = memberService.uploadProfilePicture(memberId, multipartFile);
        MemberResponse memberResponse = memberMapper.memberToResponse(member);
        ApiResponse<MemberResponse> response = new ApiResponse<>(
                ResponseStatus.UPDATED.name(),
                "Member retrieved successfully!",
                memberResponse,
                null
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/{memberId}")
    public ResponseEntity<?> getMemberById(@PathVariable("memberId")  String memberId)  {
        Member member = memberService.findMemberByMemberId(memberId);
        MemberResponse memberResponse = memberMapper.memberToResponse(member);
        ApiResponse<MemberResponse> response = new ApiResponse<>(
                ResponseStatus.CREATED.name(),
                "Member retrieved successfully!",
                memberResponse,
                null
        );

        return ResponseEntity.ok(response);
    }

    //@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @GetMapping
    public ResponseEntity<?> findMemberByPageAndRole(
            @RequestParam(name = "page", required = false, defaultValue = "1") int page,
            @RequestParam(name = "size", required = false, defaultValue = "10") int size,
            @RequestParam(name = "search", required = false, defaultValue = "") String search,
            @RequestParam(name = "role", required = false, defaultValue = "ALL") String role,
            @RequestParam(name = "sortByBv", required = false, defaultValue = "ALL") String sortByBv,
            @RequestParam(name = "enabled", required = false, defaultValue = "ALL") String enabled
    )
    {
        Page<Member> productPage = memberService.findMemberByPageAndRole(page, size, search, role, sortByBv, enabled);
        List<MemberResponse> responses = new ArrayList<>();
        for (Member member : productPage.getContent()) {
            MemberResponse response = memberMapper.memberToResponse(member);
            responses.add(response);
        }

        Map<String, Integer> metadata = new LinkedHashMap<>();
        metadata.put("page", page);
        metadata.put("size", size);
        metadata.put("numberOfElements", productPage.getNumberOfElements());
        metadata.put("totalNumberOfElements", (int)productPage.getTotalElements());
        metadata.put("totalPages", productPage.getTotalPages());

        ApiResponse<List<MemberResponse>> response = new ApiResponse<>(
                ResponseStatus.SUCCESS.name(),
                "Members retrieved successfully",
                responses,
                metadata
        );

        return ResponseEntity.ok(response);
    }

    @PutMapping(value = "/{memberId}/add-roots")
    public ResponseEntity<?> addSponsorAndPlacer(
            @PathVariable(value = "memberId", required = true) String memberId,
            @RequestParam(value = "sponsor", required = true) String sponsor,
            @RequestParam(value = "placer", required = true) String placer,
            @RequestParam(value = "leg", required = true) String leg
    )
    {
        Member member = memberService.addSponsorAndPlacer(memberId, sponsor, placer, leg);
        MemberResponse memberResponse = memberMapper.memberToResponse(member);
        ApiResponse<MemberResponse> response = new ApiResponse<>(
                ResponseStatus.UPDATED.name(),
                "Member sponsor and placer added successfully!",
                memberResponse,
                null
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/{memberId}/transfer-funds")
    public ResponseEntity<?> addFundsToMember(
            @PathVariable("memberId") String memberId,
            @RequestBody TransferRequest transferRequest
            ) {
        Member member = memberService.addFundsToMember(memberId, transferRequest);
        MemberResponse memberResponse = memberMapper.memberToResponse(member);
        ApiResponse<MemberResponse> response = new ApiResponse<>(
                ResponseStatus.SUCCESS.name(),
                "Funds transferred successfully!",
                memberResponse,
                null
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/{memberId}/activate-package")
    public ResponseEntity<?> activatePackageById(
            @PathVariable("memberId") String memberId,
            @RequestBody ActivatePackageRequest request
    ) throws MessagingException {

        Order order = memberService.activatePackageByAdmin(memberId, request.getPackageId(), request.getStoreId(), request.getTxnReference());
        Member member = memberService.confirmOrderById(memberId, order.getOrderId(), "CONFIRMED");
        MemberResponse memberResponse = memberMapper.memberToResponse(member);
        ApiResponse<MemberResponse> response = new ApiResponse<>(
                ResponseStatus.ACTIVATED.name(),
                "Package activated successfully!",
                memberResponse,
                null
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/{memberId}/buy-package")
    public ResponseEntity<?> buyPackageById(
            @PathVariable("memberId") String memberId,
            @RequestBody BuyPackageRequest request
    ) throws MessagingException {
        Order order = memberService.buyPackageById(memberId, request.getPackageId(), request.getStoreId(), request.getQuantity(), request.getTxnReference());
        Member member = memberService.confirmOrderById(memberId, order.getOrderId(), "CONFIRMED");
        MemberResponse memberResponse = memberMapper.memberToResponse(member);
        ApiResponse<MemberResponse> response = new ApiResponse<>(
                ResponseStatus.SUCCESS.name(),
                "Package bought successfully!",
                memberResponse,
                null
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/{memberId}/change-password")
    public ResponseEntity<?> changePassword(
            @RequestBody ChangePasswordRequest request,
            @PathVariable(value = "memberId") String memberId
    ) {
        Member member = memberService.changePassword(memberId, request);
        MemberResponse memberResponse = memberMapper.memberToResponse(member);
        ApiResponse<MemberResponse> response = new ApiResponse<>(
                ResponseStatus.SUCCESS.name(),
                "Password changed successfully!",
                memberResponse,
                null
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/forgot-password")
    public ResponseEntity<?> forgotPassword(
            @RequestParam(value = "username") String username
    ) throws MessagingException {
        Member member = memberService.forgotPassword(username);
        MemberResponse memberResponse = memberMapper.memberToResponse(member);
        ApiResponse<MemberResponse> response = new ApiResponse<>(
                ResponseStatus.SUCCESS.name(),
                "New Password has been sent to your email successfully!",
                memberResponse,
                null
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/{memberId}/add-store-info")
    public ResponseEntity<?> addStoreInfo(
            @RequestBody StoreInfoRequest request,
            @PathVariable(value = "memberId") String memberId
    ) {
        Member member = memberService.addStoreInfo(memberId, request);
        MemberResponse memberResponse = memberMapper.memberToResponse(member);
        ApiResponse<MemberResponse> response = new ApiResponse<>(
                ResponseStatus.SUCCESS.name(),
                "Store info added successfully!",
                memberResponse,
                null
        );

        return ResponseEntity.ok(response);
    }
    
    @PostMapping(value = "/{memberId}/add-account-details")
    public ResponseEntity<?> addAccountDetails(
            @RequestBody AccountDetailsRequest request,
            @PathVariable(value = "memberId") String memberId
    ) {
        Member member = memberService.addAccountDetails(memberId, request);
        MemberResponse memberResponse = memberMapper.memberToResponse(member);
        ApiResponse<MemberResponse> response = new ApiResponse<>(
                ResponseStatus.SUCCESS.name(),
                "Bank account added successfully!",
                memberResponse,
                null
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/check/{username}")
    public ResponseEntity<?> findByUsername(
            @PathVariable(value = "username") String username
    ) {
        Member member = memberService.findEligibleSponsorByUsername(username);
        MemberResponse memberResponse = memberMapper.memberToResponse(member);
        ApiResponse<MemberResponse> response = new ApiResponse<>(
                ResponseStatus.SUCCESS.name(),
                "Member retrieved successfully!",
                memberResponse,
                null
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/check-downline")
    public ResponseEntity<?> placerDownLineOfSponsor(
            @RequestParam(value = "sponsor", required = true) String sponsor,
            @RequestParam(value = "placer", required = true) String placer
    ) {
        boolean isDownline = memberService.isPlacerDownlineOfSponsor(sponsor, placer);
        ApiResponse<Boolean> response = new ApiResponse<>(
                ResponseStatus.SUCCESS.name(),
                "Placer is a down line of sponsor!",
                isDownline,
                null
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/check-leg")
    public ResponseEntity<?> checkPlacerLeg(
            @RequestParam(value = "placer", required = true) String placer,
            @RequestParam(value = "leg", required = true) String leg
    ) {
        boolean isLegAvailable = memberService.checkPlacerLeg(placer, leg);
        ApiResponse<Boolean> response = new ApiResponse<>(
                ResponseStatus.SUCCESS.name(),
                "Placer is a down line of sponsor!",
                isLegAvailable,
                null
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/service-centers")
    public ResponseEntity<?> findAllServiceCenters() {
        List<Member> serviceCenters = memberService.findAllServiceCenters();
        List<MemberResponse> responses = new ArrayList<>();

        for (Member member : serviceCenters) {
            responses.add(memberMapper.memberToResponse(member));
        }

        ApiResponse<List<MemberResponse>> response = new ApiResponse<>(
                ResponseStatus.SUCCESS.name(),
                "Service centers retrieved successfully!",
                responses,
                null
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/premium-stores")
    public ResponseEntity<?> findAllPremiumStores() {
        List<Member> premiumStores = memberService.findAllPremiumStores();
        List<MemberResponse> responses = new ArrayList<>();

        for (Member member : premiumStores) {
            responses.add(memberMapper.memberToResponse(member));
        }

        ApiResponse<List<MemberResponse>> response = new ApiResponse<>(
                ResponseStatus.SUCCESS.name(),
                "Premium stores retrieved successfully!",
                responses,
                null
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/{memberId}/genealogy")
    public ResponseEntity<?> findGenealogyTree(
            @PathVariable(value = "memberId") String memberId
    ) {
        MemberNodeResponse nodeTree = memberService.findGenealogyTree3Levels(memberId);
        ApiResponse<MemberNodeResponse> response = new ApiResponse<>(
                ResponseStatus.SUCCESS.name(),
                "Genealogy tree retrieved successfully!",
                nodeTree,
                null
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/{memberId}/analysis")
    public ResponseEntity<?> showMemberAnalysis(@PathVariable(value = "memberId") String memberId) {
        Map<String, Object> memberAnalysis = memberService.showMemberAnalysis(memberId);
        ApiResponse<Map<String, Object>> response = new ApiResponse<>(
                ResponseStatus.SUCCESS.name(),
                "Member analysis retrieved successfully!",
                memberAnalysis,
                null
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/{memberId}/account-details")
    public ResponseEntity<?> getStoreAccountDetails(@PathVariable(value = "memberId") String memberId) {
        StoreResponse storeResponse = new StoreResponse();

        if (memberId.equals("company")) {
            storeResponse.setAddress("4, Afariogun Street, Awolowo Way, Ikeja, Lagos, Nigeria.");
            storeResponse.setImage(null);
            storeResponse.setPhoneNumber("090topnivo");
            storeResponse.setStoreId(null);
            storeResponse.setBusinessName("Topnivo International");
            storeResponse.setAccountName("Topnivo International");
            storeResponse.setBankName("Stanbic IBTC");
            storeResponse.setAccountNumber("0034395890");
        }
        else {
            Member member = memberService.findMemberByMemberId(memberId);
            storeResponse.setAddress(member.getAddress());
            storeResponse.setImage(null);
            storeResponse.setPhoneNumber(member.getPhoneNumber());
            storeResponse.setStoreId(member.getMemberId());
            storeResponse.setBusinessName(member.getBusinessName());

            if (member.getAccountDetails() != null) {
                storeResponse.setAccountName(member.getAccountDetails().getAccountName());
                storeResponse.setBankName(member.getAccountDetails().getBankName());
                storeResponse.setAccountNumber(member.getAccountDetails().getAccountNumber());
            }
        }

        ApiResponse<StoreResponse> response = new ApiResponse<>(
                ResponseStatus.SUCCESS.name(),
                "Store account details retrieved successfully!",
                storeResponse,
                null
        );

        return ResponseEntity.ok(response);
    }
}
