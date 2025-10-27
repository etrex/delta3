/*
 * Copyright (c) 2025 Etrex Kuo. All rights reserved.
 */
package com.etrex.oms.repository;

import com.etrex.oms.entity.Order;
import com.etrex.oms.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByOrder(Order order);
    Optional<Payment> findByTransactionId(String transactionId);
    List<Payment> findByOrderAndStatus(Order order, Payment.Status status);

    // 批次查詢：根據訂單 ID 列表查詢所有付款記錄
    @Query("SELECT p FROM Payment p WHERE p.order.id IN :orderIds")
    List<Payment> findByOrderIdIn(@Param("orderIds") List<Long> orderIds);
}