package com.orderflow.product.repository;

import com.orderflow.product.domain.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    Optional<Product> findByIdAndActiveTrue(UUID id);

    boolean existsBySku(String sku);

    @Query("""
            SELECT p FROM Product p
            WHERE p.active = true
            AND (:categoryId IS NULL OR p.category.id = :categoryId)
            AND (:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<Product> findActiveProducts(UUID categoryId, String search, Pageable pageable);
}
