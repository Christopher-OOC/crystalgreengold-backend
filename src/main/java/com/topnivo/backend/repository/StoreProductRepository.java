package com.topnivo.backend.repository;

import com.topnivo.backend.model.entity.Member;
import com.topnivo.backend.model.entity.StoreProduct;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

public interface StoreProductRepository extends JpaRepository<StoreProduct, Integer>, PagingAndSortingRepository<StoreProduct, Integer> {

    @Query(value = """
    SELECT sp FROM StoreProduct sp
    WHERE sp.store.memberId = ?1 AND sp.product.id = ?2
    """)
    StoreProduct findProductFromStore(String sellerId, String productId);

    @Query("""
    SELECT sp FROM StoreProduct sp
    WHERE sp.store = :store
    AND (
        UPPER(sp.product.name) LIKE UPPER(CONCAT('%', :search, '%'))
        OR
        UPPER(sp.product.description) LIKE UPPER(CONCAT('%', :search, '%'))
    )
    """)
    Page<StoreProduct> searchProductsInStore(@Param("store") Member store, @Param("search") String search, Pageable pageable);

    @Query("""
    SELECT sp FROM StoreProduct sp
    WHERE sp.store = :store
    AND (
        UPPER(sp.product.name) LIKE UPPER(CONCAT('%', :search, '%'))
        OR
        UPPER(sp.product.description) LIKE UPPER(CONCAT('%', :search, '%'))
    )
    AND sp.product.category.id = :categoryId
    """)
    Page<StoreProduct> searchProductsInStoreInCategory(@Param("store") Member store, @Param("search") String search, @Param("categoryId") int categoryId, Pageable pageable);


}
