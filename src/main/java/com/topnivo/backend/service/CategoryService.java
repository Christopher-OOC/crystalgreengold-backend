package com.topnivo.backend.service;

import com.topnivo.backend.exception.exception.BadRequestException;
import com.topnivo.backend.exception.exception.ErrorMessages;
import com.topnivo.backend.exception.exception.NoSuchResourceException;
import com.topnivo.backend.exception.exception.ResourceAlreadyExistException;
import com.topnivo.backend.mapper.CategoryMapper;
import com.topnivo.backend.model.entity.Category;
import com.topnivo.backend.model.entity.Member;
import com.topnivo.backend.model.request.CategoryCreateRequest;
import com.topnivo.backend.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryMapper categoryMapper;
    private final CategoryRepository categoryRepository;
    private final MemberService memberService;

    public Category createCategory(CategoryCreateRequest request) {
        checkIfAdminIsValid();

        Category checkCategory = categoryRepository.findByName(request.getName());
        if (!Objects.isNull(checkCategory)) {
            throw new ResourceAlreadyExistException(ErrorMessages.CATEGORY_ALREADY_EXIST);
        }

        Category category = Category
                .builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();

        return categoryRepository.save(category);
    }

    public Category updateCategory(int categoryId, CategoryCreateRequest request) {
        checkIfAdminIsValid();

        Category checkCategory = categoryRepository.findById(categoryId);
        if (Objects.isNull(checkCategory)) {
            throw new NoSuchResourceException(ErrorMessages.NO_SUCH_CATEGORY);
        }

        if (!request.getName().isBlank()) {
            checkCategory.setName(request.getName());
        }
        if (!request.getDescription().isBlank()) {
            checkCategory.setDescription(request.getDescription());
        }

        return categoryRepository.save(checkCategory);
    }

    public List<Category> findAll() {
        return categoryRepository.findAll();
    }

    public Category findCategoryById(int categoryId) {
        Category checkCategory = categoryRepository.findById(categoryId);
        if (Objects.isNull(checkCategory)) {
            throw new NoSuchResourceException(ErrorMessages.NO_SUCH_CATEGORY);
        }

        return checkCategory;
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
