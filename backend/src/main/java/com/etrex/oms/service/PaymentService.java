/*
 * Copyright (c) 2025 Etrex Kuo. All rights reserved.
 */
package com.etrex.oms.service;

import com.etrex.oms.dto.PaymentDTO;
import com.etrex.oms.dto.PaymentRequest;
import com.etrex.oms.entity.Order;
import com.etrex.oms.entity.OrderEvent;
import com.etrex.oms.entity.Payment;
import com.etrex.oms.entity.User;
import com.etrex.oms.exception.BusinessException;
import com.etrex.oms.exception.ResourceNotFoundException;
import com.etrex.oms.repository.OrderEventRepository;
import com.etrex.oms.repository.OrderRepository;
import com.etrex.oms.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final OrderEventRepository orderEventRepository;

    @Transactional
    public PaymentDTO initiatePayment(Long orderId, PaymentRequest paymentRequest) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (order.getStatus() != Order.Status.CREATED) {
            throw new BusinessException("Order cannot be paid in current status: " + order.getStatus());
        }

        // 模擬付款處理 - 檢查有效期限是否過期
        boolean paymentSuccess = true;
        String failureReason = null;

        // 測試用：檢查信用卡有效期限
        if ("CREDIT_CARD".equals(paymentRequest.getPaymentMethod())) {
            if (paymentRequest.getCardExpiry() != null) {
                String validationError = validateCardExpiry(paymentRequest.getCardExpiry());
                if (validationError != null) {
                    paymentSuccess = false;
                    failureReason = validationError;
                }
            }
        }

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setPaymentMethod(Payment.PaymentMethod.valueOf(paymentRequest.getPaymentMethod()));
        payment.setAmount(order.getTotalAmount());
        payment.setTransactionId(UUID.randomUUID().toString());

        if (paymentSuccess) {
            payment.setStatus(Payment.Status.SUCCESS);
            payment.setPaidAt(LocalDateTime.now());

            Payment savedPayment = paymentRepository.save(payment);

            // Update order status to PAID
            order.setStatus(Order.Status.PAID);
            orderRepository.save(order);

            createOrderEvent(order, "PAID", "Payment completed with method: " + paymentRequest.getPaymentMethod(), order.getCustomer());

            return convertToPaymentDTO(savedPayment);
        } else {
            // 付款失敗
            payment.setStatus(Payment.Status.FAILED);
            payment.setFailureReason(failureReason);
            Payment savedPayment = paymentRepository.save(payment);

            createOrderEvent(order, "PAYMENT_FAILED", failureReason, order.getCustomer());

            return convertToPaymentDTO(savedPayment);
        }
    }

    /**
     * 驗證信用卡有效期限
     * @param cardExpiry 信用卡有效期限 (格式: MM/YY)
     * @return 如果有效則返回 null，否則返回錯誤訊息
     */
    private String validateCardExpiry(String cardExpiry) {
        // Format: MM/YY
        if (cardExpiry == null || !cardExpiry.matches("\\d{2}/\\d{2}")) {
            return "信用卡有效期限格式錯誤，應為 MM/YY 格式（例如：01/25）";
        }

        try {
            String[] parts = cardExpiry.split("/");
            int month = Integer.parseInt(parts[0]);
            int year = 2000 + Integer.parseInt(parts[1]); // YY to YYYY

            // 驗證月份範圍
            if (month < 1 || month > 12) {
                return "信用卡有效期限的月份錯誤，月份必須在 01 到 12 之間";
            }

            // 檢查是否過期
            LocalDateTime expiryDate = LocalDateTime.of(year, month, 1, 0, 0).plusMonths(1).minusSeconds(1);
            if (LocalDateTime.now().isAfter(expiryDate)) {
                return "信用卡已過期";
            }

            return null; // 驗證通過
        } catch (Exception e) {
            return "信用卡有效期限無效：" + e.getMessage();
        }
    }

    @Transactional
    public PaymentDTO completePayment(Long orderId, String transactionId) {
        Payment payment = paymentRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

        if (payment.getStatus() != Payment.Status.PENDING) {
            throw new BusinessException("Payment is not in pending status");
        }

        payment.setStatus(Payment.Status.SUCCESS);
        payment.setPaidAt(LocalDateTime.now());
        Payment savedPayment = paymentRepository.save(payment);

        Order order = payment.getOrder();
        order.setStatus(Order.Status.PAID);
        orderRepository.save(order);

        createOrderEvent(order, "PAID", "Payment completed successfully", order.getCustomer());

        return convertToPaymentDTO(savedPayment);
    }

    private PaymentDTO convertToPaymentDTO(Payment payment) {
        PaymentDTO dto = new PaymentDTO();
        dto.setId(payment.getId());
        dto.setOrderId(payment.getOrder().getId());
        dto.setPaymentMethod(payment.getPaymentMethod().name());
        dto.setAmount(payment.getAmount());
        dto.setStatus(payment.getStatus().name());
        dto.setTransactionId(payment.getTransactionId());
        dto.setPaidAt(payment.getPaidAt());
        dto.setFailureReason(payment.getFailureReason());
        dto.setCreatedAt(payment.getCreatedAt());
        return dto;
    }

    private void createOrderEvent(Order order, String eventType, String message, User modifiedBy) {
        OrderEvent event = new OrderEvent();
        event.setOrder(order);
        event.setEventType(eventType);
        event.setMessage(message);
        event.setModifiedBy(modifiedBy);
        event.setCreatedAt(LocalDateTime.now());
        orderEventRepository.save(event);
    }
}
