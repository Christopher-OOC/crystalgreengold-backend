package com.topnivo.backend.controller;

import com.topnivo.backend.mapper.MemberMapper;
import com.topnivo.backend.model.entity.AdminSetting;
import com.topnivo.backend.model.entity.Member;
import com.topnivo.backend.model.request.AdminSettingUpdateRequest;
import com.topnivo.backend.model.request.MemberUpdateRequest;
import com.topnivo.backend.model.response.ApiResponse;
import com.topnivo.backend.model.response.MemberResponse;
import com.topnivo.backend.model.response.ResponseStatus;
import com.topnivo.backend.service.AdminSettingService;
import com.topnivo.backend.service.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(value = "/api/v1/admins")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final AdminSettingService adminSettingService;
    private final MemberService memberService;
    private final MemberMapper memberMapper;

    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @PutMapping(value = "/{memberId}/members")
    public ResponseEntity<?> updateMemberRoleAndEnable(
            @PathVariable("memberId") String memberId,
            @RequestParam(value = "role", required = false, defaultValue = "") String role,
            @RequestParam(value = "enabled", required = true) boolean enabled
    ) {
        Member member = memberService.adminUpdateMember(memberId, role, enabled);
        MemberResponse memberResponse = memberMapper.memberToResponse(member);
        ApiResponse<MemberResponse> response = new ApiResponse<>(
                ResponseStatus.UPDATED.name(),
                "Admin has updated member successfully!",
                memberResponse,
                null
        );

        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @PutMapping(value = "/update-member-info/{memberId}")
    public ResponseEntity<?> updateMemberInfo(
            @PathVariable("memberId") String memberId,
            @RequestBody MemberUpdateRequest request
            ) {
        Member member = memberService.adminUpdateMemberInfo(memberId, request);
        MemberResponse memberResponse = memberMapper.memberToResponse(member);
        ApiResponse<MemberResponse> response = new ApiResponse<>(
                ResponseStatus.UPDATED.name(),
                "Admin has updated member successfully!",
                memberResponse,
                null
        );

        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @PutMapping(value = "/settings")
    public ResponseEntity<?> updateAdminSettings(
            @RequestBody List<AdminSettingUpdateRequest> request
    ) {
        List<AdminSetting> settings = adminSettingService.updateAdminSettings(request);
        ApiResponse<List<AdminSetting>> response = new ApiResponse<>(
                ResponseStatus.UPDATED.name(),
                "Admin setting updated successfully!",
                settings,
                null
        );

        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @GetMapping(value = "/settings")
    public ResponseEntity<?> findAllAdminSettings() {
        List<AdminSetting> settings = adminSettingService.findAllAdminSettings();
        ApiResponse<List<AdminSetting>> response = new ApiResponse<>(
                ResponseStatus.SUCCESS.name(),
                "Admin settings retrieved successfully!",
                settings,
                null
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/settings/{settingId}")
    public ResponseEntity<?> findAdminSettingById(@PathVariable("settingId") int settingId) {
        AdminSetting setting = adminSettingService.findAdminSetting(settingId);
        ApiResponse<AdminSetting> response = new ApiResponse<>(
                ResponseStatus.SUCCESS.name(),
                "Admin setting retrieved successfully!",
                setting,
                null
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/settings/name/{name}")
    public ResponseEntity<?> findAdminSettingByName(@PathVariable("name") String name) {
        AdminSetting setting = adminSettingService.findAdminSettingByName(name);
        ApiResponse<AdminSetting> response = new ApiResponse<>(
                ResponseStatus.SUCCESS.name(),
                "Admin setting retrieved successfully!",
                setting,
                null
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/login-as-user/{memberId}/{adminId}")
    public ResponseEntity<?> adminLoginAsUser(
        @PathVariable("memberId") String memberId,
        @PathVariable("adminId") String adminId
    ) {
        Map<String, Object> data = memberService.loginAsUser(memberId, adminId);
        ApiResponse<Map<String, Object>> response = new ApiResponse<>(
                ResponseStatus.SUCCESS.name(),
                "Admin setting retrieved successfully!",
                data,
                null
        );

        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAnyRole('ADMIN')")
    @GetMapping(value = "/{memberId}/activate-package/{packageId}")
    public ResponseEntity<?> adminActivateUserPackage(
            @PathVariable("memberId") String memberId,
            @PathVariable("packageId") int packageId
    ) {
        Map<String, Object> data = memberService.adminActivateUserPackage(memberId, packageId);
        ApiResponse<Map<String, Object>> response = new ApiResponse<>(
                ResponseStatus.SUCCESS.name(),
                "Admin setting retrieved successfully!",
                data,
                null
        );

        return ResponseEntity.ok(response);
    }
}
