package com.topnivo.backend.configs;

import com.topnivo.backend.model.constant.AdminSettings;
import com.topnivo.backend.model.constant.MemberType;
import com.topnivo.backend.model.constant.PackageName;
import com.topnivo.backend.model.entity.AdminSetting;
import com.topnivo.backend.model.entity.Member;
import com.topnivo.backend.model.entity.Package;
import com.topnivo.backend.model.entity.Role;
import com.topnivo.backend.repository.AdminSettingRepository;
import com.topnivo.backend.repository.MemberRepository;
import com.topnivo.backend.repository.PackageRepository;
import com.topnivo.backend.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class DeployInitData {

    private final MemberRepository memberRepository;
    private final RoleRepository roleRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final AdminSettingRepository adminSettingRepository;
    private final PackageRepository packageRepository;

    @EventListener
    public void loadInitialData(ApplicationReadyEvent event) {
        Package freePackage = packageRepository.findByName(PackageName.FREE.name());
        if (freePackage == null) {
            freePackage = Package
                    .builder()
                    .name(PackageName.FREE.name())
                    .bv(100)
                    .pv(100)
                    .price(100)
                    .description("Free Package")
                    .directCommissionRate(10)
                    .binaryCommissionRate(10)
                    .dailyCapping(100)
                    .build();

            freePackage = packageRepository.save(freePackage);
        }

        Role role = roleRepository.findByName("ROLE_" + MemberType.SUPER_ADMIN.name());

        if (role == null) {
            Role memberRole = roleRepository
                    .save(Role.builder().name("ROLE_" + MemberType.REGULAR_MEMBER.name()).build());
            Role serviceRole = roleRepository
                    .save(Role.builder().name("ROLE_" + MemberType.SERVICE_CENTER.name()).build());
            Role premiumRole = roleRepository
                    .save(Role.builder().name("ROLE_" + MemberType.PREMIUM_STORE.name()).build());
            Role adminRole = roleRepository.save(Role.builder().name("ROLE_" + MemberType.ADMIN.name()).build());
            Role superRole = roleRepository.save(Role.builder().name("ROLE_" + MemberType.SUPER_ADMIN.name()).build());

            Member superAdmin = new Member();

            superAdmin.setMemberId(UUID.randomUUID().toString());
            superAdmin.setEmail("topnivoinfo@gmail.com");
            superAdmin.setUsername("super-admin");
            superAdmin.setPassword(passwordEncoder.encode("super-admin"));
            superAdmin.setEnabled(true);
            superAdmin.setFirstName("super-admin");
            superAdmin.setLastName("super-admin");
            superAdmin.setRoles(Arrays.asList(superRole));
            superAdmin = memberRepository.save(superAdmin);

            Member admin = new Member();
            admin.setMemberId(UUID.randomUUID().toString());
            admin.setEmail("olojedechristopher24@gmail.com");
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin"));
            admin.setEnabled(true);
            admin.setFirstName("admin");
            admin.setLastName("admin");
            admin.setRoles(Arrays.asList(adminRole));

            admin.setPlacer(superAdmin);

            admin = memberRepository.save(admin);

            Member premiumStore = new Member();
            premiumStore.setMemberId(UUID.randomUUID().toString());
            premiumStore.setEmail("olojedechristopher24@gmail.com");
            premiumStore.setUsername("premium-store");
            premiumStore.setPassword(passwordEncoder.encode("premium-store"));
            premiumStore.setEnabled(true);
            premiumStore.setFirstName("premium-store");
            premiumStore.setLastName("premium-store");
            premiumStore.setRoles(Arrays.asList(premiumRole));
            premiumStore.setCurrentPackage(freePackage);

            premiumStore.setPlacer(superAdmin);

            premiumStore = memberRepository.save(premiumStore);

            superAdmin.setLeftLeg(premiumStore);
            superAdmin.setRightLeg(premiumStore);

            memberRepository.save(superAdmin);

            for (AdminSettings s : AdminSettings.values()) {
                AdminSetting adminSetting = new AdminSetting();
                adminSetting.setName(s.name());
                adminSetting.setValue(10.0);

                adminSettingRepository.save(adminSetting);
            }
        }

        Member premiumStore = memberRepository.findByUsernameIgnoreCase("premium-store");
        if (premiumStore != null && premiumStore.getCurrentPackage() == null) {
            premiumStore.setCurrentPackage(freePackage);
            memberRepository.save(premiumStore);
            log.info("Assigned FREE package to seeded premium-store account.");
        }
    }
}
