package com.topnivo.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.topnivo.backend.mapper.StoreProductMapper;
import com.topnivo.backend.model.entity.Product;
import com.topnivo.backend.model.entity.StoreProduct;
import com.topnivo.backend.model.request.ProductCreateRequest;
import com.topnivo.backend.model.response.ApiResponse;
import com.topnivo.backend.model.response.ResponseStatus;
import com.topnivo.backend.model.response.StoreProductResponse;
import com.topnivo.backend.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(value = "/api/v1/products")
@RequiredArgsConstructor
@Slf4j
public class ProductController {

    private final ProductService productService;
    private final ModelMapper modelMapper;
    private final StoreProductMapper storeProductMapper;
    private final ObjectMapper objectMapper;

    @PostMapping(consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<?> createProduct(
            @RequestParam(value = "data", required = true) String requestBody,
            @RequestParam(value = "file", required = true) MultipartFile multipartFile
            ) throws IOException {
        ProductCreateRequest productRequest = objectMapper.readValue(requestBody, ProductCreateRequest.class);
        Product product = productService.createProduct(productRequest, multipartFile);

        ApiResponse<Product> response = new ApiResponse<>(
                ResponseStatus.CREATED.name(),
                "Product created successfully",
                product,
                null
        );

        return ResponseEntity.ok(response);
    }

    @PutMapping(value = "/{productId}", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<?> updateProduct(
            @PathVariable("productId") String productId,
            @RequestParam(value = "data", required = true) String requestBody,
            @RequestParam(value = "file", required = false) MultipartFile multipartFile
    ) throws IOException {
        ProductCreateRequest productRequest = objectMapper.readValue(requestBody, ProductCreateRequest.class);
        Product product = productService.updateProduct(productId, productRequest, multipartFile);

        ApiResponse<Product> response = new ApiResponse<>(
                ResponseStatus.UPDATED.name(),
                "Product updated successfully!",
                product,
                null
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/{productId}")
    public ResponseEntity<?> findProductById(@PathVariable("productId") String productId) {
        Product product = productService.findProductById(productId);

        ApiResponse<Product> response = new ApiResponse<>(
                ResponseStatus.SUCCESS.name(),
                "Product retrieved successfully",
                product,
                null
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<?> findAllProductsByPage(
            @RequestParam(name = "page", required = false, defaultValue = "1") int page,
            @RequestParam(name = "size", required = false, defaultValue = "10") int size,
            @RequestParam(name = "search", required = false, defaultValue = "") String search,
            @RequestParam(name = "categoryId", required = false, defaultValue = "-1") int categoryId
    ) {
        Page<Product> productPage = productService.findProductsByPage(page, size, search, categoryId);
        Map<String, Integer> metadata = new LinkedHashMap<>();
        metadata.put("page", page);
        metadata.put("size", size);
        metadata.put("numberOfElements", productPage.getNumberOfElements());
        metadata.put("totalNumberOfElements", (int)productPage.getTotalElements());
        metadata.put("totalPages", productPage.getTotalPages());

        ApiResponse<List<Product>> response = new ApiResponse<>(
                ResponseStatus.SUCCESS.name(),
                "Products retrieved successfully",
                productPage.getContent(),
                metadata
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/stores/{memberId}")
    public ResponseEntity<?> findAllProductsFromAStoreByPage(
            @PathVariable(value = "memberId", required = true) String memberId,
            @RequestParam(name = "page", required = false, defaultValue = "1") int page,
            @RequestParam(name = "size", required = false, defaultValue = "10") int size,
            @RequestParam(name = "search", required = false, defaultValue = "") String search,
            @RequestParam(name = "categoryId", required = false, defaultValue = "-1") int categoryId
    ) {
        Page<StoreProduct> productPage = productService.findAllProductsFromAStoreByPage(memberId, page, size, search, categoryId);
        List<StoreProductResponse> responses = new ArrayList<>();
        for (StoreProduct storeProduct : productPage.getContent()) {
            StoreProductResponse response = storeProductMapper.storeProductToResponse(storeProduct);
            responses.add(response);
        }

        Map<String, Integer> metadata = new LinkedHashMap<>();
        metadata.put("page", page);
        metadata.put("size", size);
        metadata.put("numberOfElements", productPage.getNumberOfElements());
        metadata.put("totalNumberOfElements", (int)productPage.getTotalElements());
        metadata.put("totalPages", productPage.getTotalPages());

        ApiResponse<List<StoreProductResponse>> response = new ApiResponse<>(
                ResponseStatus.SUCCESS.name(),
                "Products from store retrieved successfully",
                responses,
                metadata
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/stores/products/{id}")
    public ResponseEntity<?> findProductFromStoreById(@PathVariable("id") int id) {
        StoreProduct storeProduct = productService.findProductFromStoreById(id);
        StoreProductResponse storeProductToResponse = storeProductMapper.storeProductToResponse(storeProduct);

        ApiResponse<StoreProductResponse> response = new ApiResponse<>(
                ResponseStatus.SUCCESS.name(),
                "Product from store retrieved successfully",
                storeProductToResponse,
                null
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/discounted")
    public ResponseEntity<?> findAllDiscountedProductsByPage(
            @RequestParam(name = "page", required = false, defaultValue = "1") int page,
            @RequestParam(name = "size", required = false, defaultValue = "10") int size,
            @RequestParam(name = "search", required = false, defaultValue = "") String search
    ) {
        Page<Product> productPage = productService.findDiscountedProductsByPage(page, size, search);
        Map<String, Integer> metadata = new LinkedHashMap<>();
        metadata.put("page", page);
        metadata.put("size", size);
        metadata.put("numberOfElements", productPage.getNumberOfElements());
        metadata.put("totalNumberOfElements", (int)productPage.getTotalElements());
        metadata.put("totalPages", productPage.getTotalPages());

        ApiResponse<List<Product>> response = new ApiResponse<>(
                ResponseStatus.SUCCESS.name(),
                "Discounted products retrieved successfully",
                productPage.getContent(),
                metadata
        );

        return ResponseEntity.ok(response);
    }
}
