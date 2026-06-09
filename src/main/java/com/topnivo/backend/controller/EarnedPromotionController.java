package com.topnivo.backend.controller;

import com.topnivo.backend.mapper.MemberMapper;
import com.topnivo.backend.model.entity.EarnedPromotion;
import com.topnivo.backend.model.response.ApiResponse;
import com.topnivo.backend.model.response.EarnedPromotionResponse;
import com.topnivo.backend.model.response.ResponseStatus;
import com.topnivo.backend.service.EarnedPromotionService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(value = "/api/v1/earned-promos")
@RequiredArgsConstructor
public class EarnedPromotionController {

    private final EarnedPromotionService earnedPromotionService;
    private final ModelMapper modelMapper;
    private final MemberMapper memberMapper;

    @GetMapping
    public ResponseEntity<?> getPromosByPage(
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @RequestParam(value = "size", required = false, defaultValue = "10") int size,
            @RequestParam(value = "received", required = false, defaultValue = "ALL") String hasReceived
    ) {
        Page<EarnedPromotion> promotionPage = earnedPromotionService.findByPage(page, size, hasReceived);
        List<EarnedPromotionResponse> responses = new ArrayList<>();
        for (EarnedPromotion promotion : promotionPage.getContent()) {
            EarnedPromotionResponse promotionResponse = modelMapper.map(promotion, EarnedPromotionResponse.class);
            promotionResponse.setMember(memberMapper.memberToResponse(promotion.getMember()));

            responses.add(promotionResponse);
        }

        Map<String, Integer> metadata = new LinkedHashMap<>();
        metadata.put("page", page);
        metadata.put("size", size);
        metadata.put("numberOfElements", promotionPage.getNumberOfElements());
        metadata.put("totalNumberOfElements", (int)promotionPage.getTotalElements());
        metadata.put("totalPages", promotionPage.getTotalPages());

        ApiResponse<List<EarnedPromotionResponse>> response = new ApiResponse<>(
                ResponseStatus.SUCCESS.name(),
                "Earned Promos retrieved successfully",
                responses,
                metadata
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/{id}")
    public ResponseEntity<?> findById(@PathVariable(value = "id") int id) {
        EarnedPromotion earnedPromotion = earnedPromotionService.findById(id);
        EarnedPromotionResponse promotionResponse = modelMapper.map(earnedPromotion, EarnedPromotionResponse.class);
        promotionResponse.setMember(memberMapper.memberToResponse(earnedPromotion.getMember()));

        ApiResponse<EarnedPromotionResponse> response = new ApiResponse<>(
                ResponseStatus.SUCCESS.name(),
                "A earned promo retrieved successfully",
                promotionResponse,
                null
        );

        return ResponseEntity.ok(response);
    }

    @PutMapping(value = "/{id}")
    public ResponseEntity<?> adminUpdatePromotionState(
            @PathVariable(value = "id") int id,
            @RequestParam(value = "received", required = true) boolean received
    ) {
        EarnedPromotion earnedPromotion = earnedPromotionService.adminUpdatePromotionState(id, received);
        EarnedPromotionResponse promotionResponse = modelMapper.map(earnedPromotion, EarnedPromotionResponse.class);
        promotionResponse.setMember(memberMapper.memberToResponse(earnedPromotion.getMember()));
        ApiResponse<EarnedPromotionResponse> response = new ApiResponse<>(
                ResponseStatus.SUCCESS.name(),
                "Earned Promos received status updated successfully",
                promotionResponse,
                null
        );

        return ResponseEntity.ok(response);
    }
}
