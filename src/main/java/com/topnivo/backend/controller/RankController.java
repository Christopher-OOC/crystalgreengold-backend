package com.topnivo.backend.controller;

import com.topnivo.backend.model.entity.Rank;
import com.topnivo.backend.model.request.RankCreateRequest;
import com.topnivo.backend.model.response.ApiResponse;
import com.topnivo.backend.model.response.ResponseStatus;
import com.topnivo.backend.service.RankService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api/v1/ranks")
@RequiredArgsConstructor
@Slf4j
public class RankController {

    private final RankService rankService;

    @PostMapping
    public ResponseEntity<?> createRank(@RequestBody RankCreateRequest request) {
        Rank rank = rankService.createRank(request);
        ApiResponse<Rank> response = new ApiResponse<>(
                ResponseStatus.CREATED.name(),
                "Rank created successfully",
                rank,
                null
        );

        return ResponseEntity.ok(response);
    }

    @PutMapping(value = "/{id}")
    public ResponseEntity<?> updateRank(@RequestBody RankCreateRequest request, @PathVariable(value = "id") int id) {
        Rank rank = rankService.updateRank(id, request);
        ApiResponse<Rank> response = new ApiResponse<>(
                ResponseStatus.UPDATED.name(),
                "Rank updated successfully",
                rank,
                null
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<?> findAllRanks() {
        List<Rank> ranks = rankService.findAllRanks();
        ApiResponse<List<Rank>> response = new ApiResponse<>(
                ResponseStatus.SUCCESS.name(),
                "Ranks retrieved successfully",
                ranks,
                null
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/{id}")
    public ResponseEntity<?> findById(@PathVariable(value = "id") int id) {
        Rank rank = rankService.findById(id);
        ApiResponse<Rank> response = new ApiResponse<>(
                ResponseStatus.SUCCESS.name(),
                "Rank retrieved successfully",
                rank,
                null
        );

        return ResponseEntity.ok(response);
    }
}
