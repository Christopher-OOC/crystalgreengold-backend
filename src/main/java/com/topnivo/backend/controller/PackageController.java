package com.topnivo.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.topnivo.backend.model.constant.PackageName;
import com.topnivo.backend.model.entity.Package;
import com.topnivo.backend.model.entity.StorePackage;
import com.topnivo.backend.model.request.PackageCreateRequest;
import com.topnivo.backend.model.response.ApiResponse;
import com.topnivo.backend.model.response.ResponseStatus;
import com.topnivo.backend.service.PackageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping(value = "/api/v1/packages")
@RequiredArgsConstructor
@Slf4j
public class PackageController {

    private final PackageService packageService;
    private final ObjectMapper objectMapper;

    @PostMapping(consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<?> createAPackage(
            @RequestParam(value = "data", required = true) String requestBody,
            @RequestParam(value = "file", required = true) MultipartFile multipartFile
    ) throws IOException {

        PackageCreateRequest packageRequest = objectMapper.readValue(requestBody, PackageCreateRequest.class);
        Package _package = packageService.createAPackage(packageRequest, multipartFile);

        ApiResponse<Package> response = new ApiResponse<>(
                ResponseStatus.CREATED.name(),
                "Package created successfully!",
                _package,
                null
        );

        return ResponseEntity.ok(response);
    }

    @PutMapping(value = "/{packageId}", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<?> updateAPackage(
            @PathVariable(value = "packageId") int packageId,
            @RequestParam(value = "data", required = true) String requestBody,
            @RequestParam(value = "file", required = false) MultipartFile multipartFile
    ) throws IOException {

        PackageCreateRequest packageRequest = objectMapper.readValue(requestBody, PackageCreateRequest.class);
        Package _package = packageService.updateAPackage(packageId, packageRequest, multipartFile);

        ApiResponse<Package> response = new ApiResponse<>(
                ResponseStatus.UPDATED.name(),
                "Package updated successfully!",
                _package,
                null
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/{packageId}")
    public ResponseEntity<?> getPackageById(@PathVariable("packageId") int packageId) {
        Package packages = packageService.findPackageById(packageId);

        ApiResponse<Package> response = new ApiResponse<>(
                ResponseStatus.SUCCESS.name(),
                "Package retrieved successfully!",
                packages,
                null
        );

        return ResponseEntity.ok(response);
    }


    @GetMapping
    public ResponseEntity<?> getAllPackages() {
        List<Package> packages = packageService.getAllPackages();
        packages = packages
                .stream()
                .filter(pac -> !pac.getName().equals(PackageName.FREE.name()))
                .toList();

        ApiResponse<List<Package>> response = new ApiResponse<>(
                ResponseStatus.SUCCESS.name(),
                "Package retrieved successfully!",
                packages,
                null
        );

        return ResponseEntity.ok(response);
    }

    @DeleteMapping(value = "/{packageId}")
    public ResponseEntity<?> deleteAPackage(@PathVariable("packageId") int packageId) {
        packageService.deleteAPackage(packageId);

        ApiResponse<Void> response = new ApiResponse<>(
                ResponseStatus.DELETED.name(),
                "Package deleted successfully!",
                null,
                null
        );

        return ResponseEntity.ok(response);
    }
}
