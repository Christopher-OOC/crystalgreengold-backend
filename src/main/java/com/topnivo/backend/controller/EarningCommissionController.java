package com.topnivo.backend.controller;

import com.topnivo.backend.model.entity.EarningCommission;
import com.topnivo.backend.model.response.ApiResponse;
import com.topnivo.backend.model.response.EarningCommissionResponse;
import com.topnivo.backend.model.response.ResponseStatus;
import com.topnivo.backend.service.EarningCommissionService;
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
@RequestMapping(value = "/api/v1/bonuses")
@RequiredArgsConstructor
public class EarningCommissionController {

    private final EarningCommissionService earningCommissionService;
    private final ModelMapper modelMapper;

    @GetMapping(value = "/members/{memberId}")
    public ResponseEntity<?> getBonusesByPage(
            @PathVariable(value = "memberId", required = true) String memberId,
            @RequestParam(name = "page", required = false, defaultValue = "1") int page,
            @RequestParam(name = "size", required = false, defaultValue = "10") int size,
            @RequestParam(name = "type", required = false, defaultValue = "ALL") String type
    ) {
        Page<EarningCommission> bonusPage = earningCommissionService.getBonusesByPage(memberId, page, size, type);

        Map<String, Integer> metadata = new LinkedHashMap<>();
        metadata.put("page", page);
        metadata.put("size", size);
        metadata.put("numberOfElements", bonusPage.getNumberOfElements());
        metadata.put("totalNumberOfElements", (int) bonusPage.getTotalElements());
        metadata.put("totalPages", bonusPage.getTotalPages());

        List<EarningCommissionResponse> responseList = new ArrayList<>();
        bonusPage.getContent().forEach(commission ->
                responseList.add(modelMapper.map(commission, EarningCommissionResponse.class)));

        ApiResponse<List<EarningCommissionResponse>> response = new ApiResponse<>(
                ResponseStatus.SUCCESS.name(),
                "Earning commissions retrieved successfully!",
                responseList,
                metadata
        );

        return ResponseEntity.ok(response);
    }
}
