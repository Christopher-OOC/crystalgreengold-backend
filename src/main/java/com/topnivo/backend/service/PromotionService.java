package com.topnivo.backend.service;

import com.topnivo.backend.exception.exception.BadRequestException;
import com.topnivo.backend.exception.exception.ErrorMessages;
import com.topnivo.backend.exception.exception.NoSuchResourceException;
import com.topnivo.backend.model.entity.Member;
import com.topnivo.backend.model.entity.Promotion;
import com.topnivo.backend.model.request.PromotionCreateRequest;
import com.topnivo.backend.repository.MemberRepository;
import com.topnivo.backend.repository.PromotionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PromotionService {

    private final PromotionRepository promotionRepository;
    private final FileService fileService;
    private final MemberRepository memberRepository;

    public Promotion createPromotion(PromotionCreateRequest request, MultipartFile multipartFile) throws IOException {
        checkIfAdminIsValid();

        Promotion promotion = new Promotion();
        promotion.setName(request.getName());
        promotion.setDescription(request.getDescription());
        promotion.setTargetPv(request.getTargetPv());
        promotion.setPrize(request.getPrize());
        promotion.setEnabled(true);
        if (multipartFile.isEmpty()) {
            throw new BadRequestException(ErrorMessages.NO_IMAGE_IN_PROMOTION);
        }
        promotion.setImage(fileService.fileBtyeToGenerateFileUrl(multipartFile.getBytes()));

        return promotionRepository.save(promotion);
    }

    public Promotion updatePromotion(int id, PromotionCreateRequest request, MultipartFile multipartFile) throws IOException {
        checkIfAdminIsValid();

        Optional<Promotion> optional = promotionRepository.findById(id);
        if (optional.isEmpty()) {
            throw new NoSuchResourceException(ErrorMessages.NO_SUCH_PROMOTION);
        }
        Promotion promotion = optional.get();
        promotion.setName(request.getName());
        promotion.setDescription(request.getDescription());
        promotion.setTargetPv(request.getTargetPv());
        promotion.setPrize(request.getPrize());
        promotion.setEnabled(request.isEnabled());
        if (!Objects.isNull(multipartFile)) {
            promotion.setImage(fileService.fileBtyeToGenerateFileUrl(multipartFile.getBytes()));
        }

        return promotionRepository.save(promotion);
    }

    public List<Promotion> findAllPromotions() {
        return promotionRepository.findAll();
    }

    public void deletePromotionById(int id) {
        checkIfAdminIsValid();

        promotionRepository.deleteById(id);
    }

    public Promotion findPromotionById(int id) {
        Optional<Promotion> optional = promotionRepository.findById(id);
        if (optional.isEmpty()) {
            throw new NoSuchResourceException(ErrorMessages.NO_SUCH_PROMOTION);
        }

        return optional.get();
    }

    private void checkIfAdminIsValid() {
        var authorities = SecurityContextHolder.getContext().getAuthentication().getAuthorities();
        boolean isAdminSuperAdmin = authorities.stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN"));
        String adminUserName = SecurityContextHolder.getContext().getAuthentication().getName();
        Member admin = memberRepository.findByUsernameIgnoreCase(adminUserName);
        if (admin == null) {
            throw new NoSuchResourceException(ErrorMessages.NO_SUCH_MEMBER);
        }

        if (!isAdminSuperAdmin && admin.getSponsor() == null && admin.getPlacer() == null) {
            throw new BadRequestException(ErrorMessages.NO_SPONSOR_AND_PLACER);
        }
    }
}
