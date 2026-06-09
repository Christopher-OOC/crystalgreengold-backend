package com.topnivo.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.topnivo.backend.model.entity.Promotion;
import com.topnivo.backend.model.request.PromotionCreateRequest;
import com.topnivo.backend.model.response.ApiResponse;
import com.topnivo.backend.model.response.ResponseStatus;
import com.topnivo.backend.service.PromotionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping(value = "/api/v1/promotions")
@RequiredArgsConstructor
@Slf4j
public class PromotionController {

    private final PromotionService promotionService;
    private final ObjectMapper objectMapper;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createPromotion(
            @RequestParam(value = "data") String requestBody,
            @RequestParam(value = "file", required = true) MultipartFile multipartFile
            ) throws IOException {
        PromotionCreateRequest request = objectMapper.readValue(requestBody, PromotionCreateRequest.class);
        Promotion promotion = promotionService.createPromotion(request, multipartFile);

        ApiResponse<Promotion> response = new ApiResponse<>(
                ResponseStatus.CREATED.name(),
                "Promotion created successfully",
                promotion,
                null
        );

        return ResponseEntity.ok(response);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updatePromotion(
            @PathVariable(value = "id") int id,
            @RequestParam(value = "data") String requestBody,
            @RequestParam(value = "file", required = false) MultipartFile multipartFile
    ) throws IOException {
        PromotionCreateRequest request = objectMapper.readValue(requestBody, PromotionCreateRequest.class);
        Promotion promotion = promotionService.updatePromotion(id, request, multipartFile);

        ApiResponse<Promotion> response = new ApiResponse<>(
                ResponseStatus.UPDATED.name(),
                "Promotion updated successfully",
                promotion,
                null
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<?> findAllPromotions() {
        List<Promotion> allPromotions = promotionService.findAllPromotions();
        ApiResponse<List<Promotion>> response = new ApiResponse<>(
                ResponseStatus.SUCCESS.name(),
                "Promotion updated successfully",
                allPromotions,
                null
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/{id}")
    public ResponseEntity<?> findPromotionById(@PathVariable(value = "id") int id) {
        Promotion promotion = promotionService.findPromotionById(id);
        ApiResponse<Promotion> response = new ApiResponse<>(
                ResponseStatus.SUCCESS.name(),
                "Promotion deleted successfully",
                promotion,
                null
        );

        return ResponseEntity.ok(response);
    }

    @DeleteMapping(value = "/{id}")
    public ResponseEntity<?> deletePromotionById(@PathVariable(value = "id") int id) {
        promotionService.deletePromotionById(id);
        ApiResponse<Void> response = new ApiResponse<>(
                ResponseStatus.DELETED.name(),
                "Promotion deleted successfully",
                null,
                null
        );

        return ResponseEntity.ok(response);
    }
}
