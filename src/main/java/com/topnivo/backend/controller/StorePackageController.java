package com.topnivo.backend.controller;

import com.topnivo.backend.model.entity.StorePackage;
import com.topnivo.backend.model.entity.Package;
import com.topnivo.backend.model.response.ApiResponse;
import com.topnivo.backend.model.response.ResponseStatus;
import com.topnivo.backend.service.StorePackageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/api/v1/store-packages")
@RequiredArgsConstructor
@Slf4j
public class StorePackageController {

    private final StorePackageService storePackageService;

    @GetMapping(value = "/{storePackageId}")
    public ResponseEntity<?> getStorePackageById(
            @PathVariable("storePackageId") int storePackageId
    ) {
        StorePackage storePackage = storePackageService.findStorePackageById(storePackageId);
        storePackage.setStore(null);
        ApiResponse<StorePackage> response = new ApiResponse<>(
                ResponseStatus.SUCCESS.name(),
                "Store Package retrieved successfully!",
                storePackage,
                null
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/stores/{storeId}")
    public ResponseEntity<?> getAllStorePackages(@PathVariable("storeId") String storeId) {
        List<StorePackage> packages = storePackageService.getAllStorePackages(storeId);
        packages.forEach(pac -> {
            pac.setStore(null);
            pac.setBoughtFromStore(null);
        });

        ApiResponse<List<Package>> response = new ApiResponse<>(
                ResponseStatus.SUCCESS.name(),
                "Store Packages retrieved successfully!",
                packages.stream().map(StorePackage::getPac).collect(Collectors.toList()),
                null
        );

        return ResponseEntity.ok(response);
    }
}
