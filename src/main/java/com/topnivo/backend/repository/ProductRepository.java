package com.topnivo.backend.repository;

import com.topnivo.backend.model.entity.Category;
import com.topnivo.backend.model.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProductRepository extends PagingAndSortingRepository<Product, String>, JpaRepository<Product, String> {
    Optional<Product> findById(String id);

    Page<Product> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(String search1, String search2, Pageable pageable);

    @Query(value = """
    SELECT p FROM Product p
    WHERE p.category = :category
    AND (
        UPPER(p.name) LIKE UPPER(CONCAT('%', :search, '%'))
        OR
        UPPER(p.description) LIKE UPPER(CONCAT('%', :search, '%'))
    )
    """)
    Page<Product> findByCategoryAndNameAndDescriptionPage(@Param("search") String search, @Param("category") Category category, Pageable pageable);

    Page<Product> findByCategory(Category category, Pageable pageable);

    Page<Product> findByIsDiscountedTrue(Pageable pageable);

    @Query(value = """
    SELECT p FROM Product p
    WHERE (
        UPPER(p.name) LIKE UPPER(CONCAT('%', :search, '%'))
        OR
        UPPER(p.description) LIKE UPPER(CONCAT('%', :search, '%'))
    )
    """)
    Page<Product> findByIsDiscountedTrue(@Param("search") String search, Pageable pageable);

}
