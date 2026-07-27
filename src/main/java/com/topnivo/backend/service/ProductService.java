package com.topnivo.backend.service;

import com.topnivo.backend.exception.exception.BadRequestException;
import com.topnivo.backend.exception.exception.ErrorMessages;
import com.topnivo.backend.exception.exception.NoSuchResourceException;
import com.topnivo.backend.mapper.ProductMapper;
import com.topnivo.backend.model.entity.Category;
import com.topnivo.backend.model.entity.Member;
import com.topnivo.backend.model.entity.Package;
import com.topnivo.backend.model.entity.Product;
import com.topnivo.backend.model.entity.StoreProduct;
import com.topnivo.backend.model.request.ProductCreateRequest;
import com.topnivo.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final PackageRepository packageRepository;
    private final ProductMapper productMapper;
    private final CategoryRepository categoryRepository;
    private final MemberRepository memberRepository;
    private final StoreProductRepository storeProductRepository;
    private final FileService fileService;

    public Product createProduct(ProductCreateRequest request, MultipartFile multipartFile) throws IOException {
        checkIfAdminIsValid();

        Product newProduct = productMapper.requestToProduct(request);
        Category category = categoryRepository.findById(request.getCategoryId());
        if (Objects.isNull(category)) {
            throw new NoSuchResourceException(ErrorMessages.NO_SUCH_CATEGORY);
        }

        if (multipartFile.isEmpty()) {
            throw new BadRequestException(ErrorMessages.NO_IMAGE_IN_PRODUCT);
        }

        newProduct.setCategory(category);
        newProduct.setImage(fileService.fileBtyeToGenerateFileUrl(multipartFile.getBytes()));

        if (request.getProductType().equals("discounted")) {
            newProduct.setDiscounted(true);
            newProduct.setDiscount(request.getDiscount());

            Package aPackage = packageRepository.findById(request.getPackageId());
            if (aPackage == null) {
                throw new NoSuchResourceException(ErrorMessages.NO_SUCH_PACKAGE);
            }

            newProduct.setEligiblePackage(aPackage);
        }

        return productRepository.save(newProduct);
    }

    public Product updateProduct(String productId, ProductCreateRequest request, MultipartFile multipartFile) throws IOException {
        checkIfAdminIsValid();

        Product product = productRepository.findById(productId).orElse(null);
        Category category = categoryRepository.findById(request.getCategoryId());

        if (Objects.isNull(product)) {
            throw new NoSuchResourceException(ErrorMessages.NO_SUCH_PRODUCT);
        }
        if (Objects.isNull(category)) {
            throw new NoSuchResourceException(ErrorMessages.NO_SUCH_CATEGORY);
        }

        if (request.getName() != null) {
            product.setName(request.getName());
        }
        if (request.getDescription() != null) {
            product.setDescription(request.getDescription());
        }
        if (request.getAvailableQuantity() > 0) {
            product.setAvailableQuantity(request.getAvailableQuantity());
        }
        if (request.getPrice() > 0) {
            product.setPrice(request.getPrice());
        }
        if (request.getBv() > 0) {
            product.setBv(request.getBv());
        }
        if (request.getPv() > 0) {
            product.setPv(request.getPv());
        }
        if (!category.getName().equals(product.getCategory().getName())) {
            product.setCategory(category);
        }
        if (!Objects.isNull(multipartFile)) {
            product.setImage(fileService.fileBtyeToGenerateFileUrl(multipartFile.getBytes()));
        }

        return productRepository.save(product);
    }

    public Product findProductById(String productId) {
        Product product = productRepository.findById(productId).orElse(null);
        if (Objects.isNull(product)) {
            throw new NoSuchResourceException(ErrorMessages.NO_SUCH_PRODUCT);
        }

        return product;
    }

    public Page<Product> findProductsByPage(int page, int size, String search, int categoryId) {
        Pageable pageable = PageRequest.of(page - 1, size);
        Category category = categoryRepository.findById(categoryId);

        if (search.isEmpty()) {
            if (categoryId == -1) {
                return productRepository.findAll(pageable);
            }
            else {
                if (Objects.isNull(category)) {
                    throw new NoSuchResourceException(ErrorMessages.NO_SUCH_CATEGORY);
                }
                return productRepository.findByCategory(category, pageable);
            }
        }
        else {
            if (categoryId == -1) {
                return productRepository.findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(search, search, pageable);
            } else {
                if (Objects.isNull(category)) {
                    throw new NoSuchResourceException(ErrorMessages.NO_SUCH_CATEGORY);
                }
                return productRepository.findByCategoryAndNameAndDescriptionPage(search, category, pageable);
            }
        }
    }

    public Page<Product> findDiscountedProductsByPage(int page, int size, String search) {
        Pageable pageable = PageRequest.of(page - 1, size);

        if (search.isEmpty()) {
            return productRepository.findByIsDiscountedTrue(pageable);
        }
        else {
         return productRepository.findByIsDiscountedTrue(search, pageable);
        }
    }

    public Page<StoreProduct> findAllProductsFromAStoreByPage(String memberId, int page, int size, String search, int categoryId) {
        Member member = memberRepository.findByMemberId(memberId);
        if (Objects.isNull(member)) {
            throw new NoSuchResourceException(ErrorMessages.NO_SUCH_MEMBER);
        }
        Pageable pageable = PageRequest.of(page - 1, size);
        Category category = categoryRepository.findById(categoryId);

        if (categoryId == -1) {
            return storeProductRepository.searchProductsInStore(member, search, pageable);
        }
        else {
            if (Objects.isNull(category)) {
                throw new NoSuchResourceException(ErrorMessages.NO_SUCH_CATEGORY);
            }
            return storeProductRepository.searchProductsInStoreInCategory(member, search, categoryId, pageable);
        }
    }

    public StoreProduct findProductFromStoreById(int id) {
        Optional<StoreProduct> storeProduct = storeProductRepository.findById(id);
        if (storeProduct.isEmpty()) {
            throw new NoSuchResourceException(ErrorMessages.NO_SUCH_PRODUCT);
        }

        return storeProduct.get();
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
