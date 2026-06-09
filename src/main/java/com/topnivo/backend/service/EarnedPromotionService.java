package com.topnivo.backend.service;

import com.topnivo.backend.exception.exception.BadRequestException;
import com.topnivo.backend.exception.exception.ErrorMessages;
import com.topnivo.backend.exception.exception.NoSuchResourceException;
import com.topnivo.backend.model.entity.EarnedPromotion;
import com.topnivo.backend.model.entity.Member;
import com.topnivo.backend.repository.EarnedPromotionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class EarnedPromotionService {

    private final EarnedPromotionRepository earnedPromotionRepository;
    private final MemberService memberService;

    public Page<EarnedPromotion> findByPage(int page, int size, String hasReceived) {
        Pageable pageable = PageRequest.of(page - 1, size);

        if (!List.of("ALL", "TRUE", "FALSE").contains(hasReceived)) {
            throw new BadRequestException(ErrorMessages.INVALID_HAS_RECEIVED);
        }

        if (hasReceived.equals("ALL")) {
            return earnedPromotionRepository.findAll(pageable);
        }
        else if (hasReceived.equals("TRUE")) {
            return earnedPromotionRepository.findByHasReceived(true, pageable);
        }
        else {
            return earnedPromotionRepository.findByHasReceived(false, pageable);
        }
    }

    public EarnedPromotion findById(int id) {
        Optional<EarnedPromotion> earnedPromotion = earnedPromotionRepository.findById(id);
        if (earnedPromotion.isEmpty()) {
            throw new NoSuchResourceException(ErrorMessages.NO_SUCH_PROMOTION);
        }

        return earnedPromotion.get();
    }

    public EarnedPromotion adminUpdatePromotionState(int id, boolean hasReceived) {
        checkIfAdminIsValid();
        EarnedPromotion earnedPromotion = findById(id);
        earnedPromotion.setHasReceived(hasReceived);

        return earnedPromotionRepository.save(earnedPromotion);
    }

    private void checkIfAdminIsValid() {
        var authorities = SecurityContextHolder.getContext().getAuthentication().getAuthorities();
        boolean isAdminSuperAdmin = authorities.stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN"));
        String adminUserName = SecurityContextHolder.getContext().getAuthentication().getName();
        Member admin = memberService.findMemberByUsername(adminUserName);

        if (!isAdminSuperAdmin && admin.getSponsor() == null && admin.getPlacer() == null) {
            throw new BadRequestException(ErrorMessages.NO_SPONSOR_AND_PLACER);
        }
    }
}
