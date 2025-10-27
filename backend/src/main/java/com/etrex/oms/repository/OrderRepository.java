/*
 * Copyright (c) 2025 Etrex Kuo. All rights reserved.
 */
package com.etrex.oms.repository;

import com.etrex.oms.entity.Order;
import com.etrex.oms.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {
    Page<Order> findByCustomer(User customer, Pageable pageable);

    Page<Order> findByStatus(Order.Status status, Pageable pageable);

    Page<Order> findByCustomerAndStatus(User customer, Order.Status status, Pageable pageable);

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.items WHERE o.id = :id")
    Optional<Order> findByIdWithItems(Long id);

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.payments WHERE o.id = :id")
    Optional<Order> findByIdWithPayments(Long id);

    Optional<Order> findByOrderNo(String orderNo);

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.items WHERE o.orderNo = :orderNo")
    Optional<Order> findByOrderNoWithItems(String orderNo);

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.payments WHERE o.orderNo = :orderNo")
    Optional<Order> findByOrderNoWithPayments(String orderNo);

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.items i LEFT JOIN FETCH i.product WHERE o.customer.id = :customerId AND o.status = :status")
    Optional<Order> findByCustomerIdAndStatusWithItemsAndProducts(Long customerId, Order.Status status);

    // ✅ Override findAll to use EntityGraph (solve N+1 query problem)
    @Override
    @EntityGraph(value = "Order.withItemsAndProducts", type = EntityGraph.EntityGraphType.FETCH)
    @NonNull
    Page<Order> findAll(@NonNull Specification<Order> spec, @NonNull Pageable pageable);
}