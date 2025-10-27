/*
 * Copyright (c) 2025 Etrex Kuo. All rights reserved.
 */
package com.etrex.oms.repository;

import com.etrex.oms.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    Page<Product> findByStatus(Product.Status status, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.status = :status AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Product> searchByKeywordAndStatus(String keyword, Product.Status status, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Product> searchByKeyword(String keyword, Pageable pageable);

    List<Product> findByStatusOrderByCreatedAtDesc(Product.Status status);

    /**
     * Atomically deduct stock from a product.
     * This operation is atomic at the database level to prevent overselling.
     *
     * @param productId The ID of the product
     * @param quantity The quantity to deduct
     * @return The number of rows affected (1 if successful, 0 if insufficient stock)
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Product p SET p.stock = p.stock - :quantity WHERE p.id = :productId AND p.stock >= :quantity")
    int deductStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);

    /**
     * Atomically restore stock to a product (e.g., when cancelling an order).
     * This operation is atomic at the database level.
     *
     * @param productId The ID of the product
     * @param quantity The quantity to restore
     * @return The number of rows affected (1 if successful, 0 if product not found)
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Product p SET p.stock = p.stock + :quantity WHERE p.id = :productId")
    int restoreStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);
}