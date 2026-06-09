package com.topnivo.backend.controller;

import com.topnivo.backend.model.entity.Category;
import com.topnivo.backend.model.request.CategoryCreateRequest;
import com.topnivo.backend.model.response.ApiResponse;
import com.topnivo.backend.model.response.ResponseStatus;
import com.topnivo.backend.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    public ResponseEntity<?> createCategory(@RequestBody CategoryCreateRequest request) {
        Category category = categoryService.createCategory(request);
        ApiResponse<Category> response = new ApiResponse<>(
                ResponseStatus.CREATED.name(),
                "Category created successfully!",
                category,
                null
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/{categoryId}")
    public ResponseEntity<?> findCategoryById(
            @PathVariable("categoryId") int categoryId
    ) {
        Category category = categoryService.findCategoryById(categoryId);
        ApiResponse<Category> response = new ApiResponse<>(
                ResponseStatus.UPDATED.name(),
                "Category updated successfully!",
                category,
                null
        );

        return ResponseEntity.ok(response);
    }

    @PutMapping(value = "/{categoryId}")
    public ResponseEntity<?> updateCategory(
            @RequestBody CategoryCreateRequest request,
            @PathVariable("categoryId") int categoryId
    ) {
        Category category = categoryService.updateCategory(categoryId, request);
        ApiResponse<Category> response = new ApiResponse<>(
                ResponseStatus.UPDATED.name(),
                "Category updated successfully!",
                category,
                null
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<?> findAllCategories() {
        List<Category> categories = categoryService.findAll();

        ApiResponse<List<Category>> response = new ApiResponse<>(
                ResponseStatus.UPDATED.name(),
                "Category retrieved successfully!",
                categories,
                null
        );

        return ResponseEntity.ok(response);
    }

}
