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

        // Check if order is cancelled
        if (order.getStatus() == Order.Status.CANCELLED) {
            throw new BusinessException("Cannot pay for a cancelled order");
        }

        // Only CREATED orders can be paid
        if (order.getStatus() != Order.Status.CREATED) {
            throw new BusinessException("Order cannot be paid in current status: " + order.getStatus());
        }

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setPaymentMethod(Payment.PaymentMethod.valueOf(paymentRequest.getPaymentMethod()));
        payment.setAmount(order.getTotalAmount());
        payment.setTransactionId(UUID.randomUUID().toString());

        // 銀行轉帳：設為 PENDING，等待管理員確認收款
        if ("BANK_TRANSFER".equals(paymentRequest.getPaymentMethod())) {
            payment.setStatus(Payment.Status.PENDING);
            Payment savedPayment = paymentRepository.save(payment);

            // 訂單狀態保持 CREATED（不改為 PAID）
            createOrderEvent(order, "PAYMENT_PENDING",
                "Awaiting bank transfer confirmation. Please transfer to account: 1234-5678-9012, Bank code: 808",
                order.getCustomer());

            return convertToPaymentDTO(savedPayment);
        }

        // 其他付款方式：模擬付款處理 - 檢查有效期限是否過期
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

    /**
     * 管理員確認銀行轉帳收款
     * @param paymentId 付款 ID
     * @return 更新後的付款資訊
     */
    @Transactional
    public PaymentDTO confirmBankTransfer(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

        // 只有 PENDING 狀態的付款可以確認
        if (payment.getStatus() != Payment.Status.PENDING) {
            throw new BusinessException("Only pending payments can be confirmed. Current status: " + payment.getStatus());
        }

        // 只有銀行轉帳可以通過此方法確認
        if (payment.getPaymentMethod() != Payment.PaymentMethod.BANK_TRANSFER) {
            throw new BusinessException("Only bank transfer payments can be confirmed through this endpoint");
        }

        // 確認收款
        payment.setStatus(Payment.Status.SUCCESS);
        payment.setPaidAt(LocalDateTime.now());
        Payment savedPayment = paymentRepository.save(payment);

        // 更新訂單狀態為已付款
        Order order = payment.getOrder();
        order.setStatus(Order.Status.PAID);
        orderRepository.save(order);

        // 記錄事件
        createOrderEvent(order, "PAID", "Bank transfer confirmed by admin", order.getCustomer());

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
