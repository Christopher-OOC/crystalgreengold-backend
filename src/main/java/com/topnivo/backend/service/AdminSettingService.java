package com.topnivo.backend.service;

import com.topnivo.backend.exception.exception.BadRequestException;
import com.topnivo.backend.exception.exception.ErrorMessages;
import com.topnivo.backend.exception.exception.NoSuchResourceException;
import com.topnivo.backend.model.constant.AdminSettings;
import com.topnivo.backend.model.entity.AdminSetting;
import com.topnivo.backend.model.entity.Member;
import com.topnivo.backend.model.request.AdminSettingUpdateRequest;
import com.topnivo.backend.repository.AdminSettingRepository;
import com.topnivo.backend.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminSettingService {

    private final AdminSettingRepository adminSettingRepository;
    private final MemberRepository memberRepository;

    public List<AdminSetting> updateAdminSettings(List<AdminSettingUpdateRequest> request) {
        checkIfAdminIsValid();

        List<AdminSetting> newSettings = new ArrayList<>();
        for (AdminSettingUpdateRequest r : request) {
            AdminSetting setting = adminSettingRepository.findById(r.getId());
            if (Objects.isNull(setting)) {
                throw new NoSuchResourceException(ErrorMessages.ADMIN_SETTINGS_NOT_CREATED);
            }
            setting.setValue(r.getValue());

            newSettings.add(setting);
        }

        return adminSettingRepository.saveAll(newSettings);
    }

    public AdminSetting findAdminSetting(int settingId) {
        AdminSetting setting = adminSettingRepository.findById(settingId);
        if (Objects.isNull(setting)) {
            throw new NoSuchResourceException(ErrorMessages.ADMIN_SETTINGS_NOT_CREATED);
        }

        return setting;
    }

    public List<AdminSetting> findAllAdminSettings() {
        return adminSettingRepository.findAll();
    }

    private void checkIfAdminIsValid() {
        var authorities = SecurityContextHolder.getContext().getAuthentication().getAuthorities();
        boolean isAdminSuperAdmin = authorities.stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN"));
        String adminUserName = SecurityContextHolder.getContext().getAuthentication().getName();
        Member admin = memberRepository.findByUsername(adminUserName);
        if (admin == null) {
            throw new NoSuchResourceException(ErrorMessages.NO_SUCH_MEMBER);
        }

        if (!isAdminSuperAdmin && admin.getSponsor() == null && admin.getPlacer() == null) {
            throw new BadRequestException(ErrorMessages.NO_SPONSOR_AND_PLACER);
        }
    }

    public AdminSetting findAdminSettingByName(String name) {

        return adminSettingRepository.findByName(name);
    }
}
