/*
 * Copyright (c) 2025 Etrex Kuo. All rights reserved.
 */
package com.etrex.oms.service;

import com.etrex.oms.dto.PaymentDTO;
import com.etrex.oms.dto.PaymentRequest;
import com.etrex.oms.entity.*;
import com.etrex.oms.exception.BusinessException;
import com.etrex.oms.exception.ResourceNotFoundException;
import com.etrex.oms.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderEventRepository orderEventRepository;

    @InjectMocks
    private PaymentService paymentService;

    private Order testOrder;
    private User testCustomer;
    private Payment testPayment;

    @BeforeEach
    void setUp() {
        testCustomer = new User();
        testCustomer.setId(1L);
        testCustomer.setUsername("testuser");

        testOrder = new Order();
        testOrder.setId(1L);
        testOrder.setOrderNo("ORD-001");
        testOrder.setCustomer(testCustomer);
        testOrder.setStatus(Order.Status.CREATED);
        testOrder.setTotalAmount(100);

        testPayment = new Payment();
        testPayment.setId(1L);
        testPayment.setOrder(testOrder);
        testPayment.setPaymentMethod(Payment.PaymentMethod.CREDIT_CARD);
        testPayment.setAmount(100);
        testPayment.setStatus(Payment.Status.SUCCESS);
        testPayment.setTransactionId("TXN-001");
    }

    @Test
    void initiatePayment_Success() {
        PaymentRequest paymentRequest = new PaymentRequest();
        paymentRequest.setOrderId(1L);
        paymentRequest.setPaymentMethod("CREDIT_CARD");
        paymentRequest.setAmount(100);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.setId(1L);
            return payment;
        });
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        when(orderEventRepository.save(any(OrderEvent.class))).thenReturn(new OrderEvent());

        PaymentDTO result = paymentService.initiatePayment(1L, paymentRequest);

        assertNotNull(result);
        assertEquals("CREDIT_CARD", result.getPaymentMethod());
        verify(paymentRepository).save(any(Payment.class));
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void initiatePayment_OrderNotFound() {
        PaymentRequest paymentRequest = new PaymentRequest();
        paymentRequest.setOrderId(999L);
        paymentRequest.setPaymentMethod("CREDIT_CARD");
        paymentRequest.setAmount(100);

        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> paymentService.initiatePayment(999L, paymentRequest));
    }

    @Test
    void initiatePayment_InvalidStatus() {
        PaymentRequest paymentRequest = new PaymentRequest();
        paymentRequest.setOrderId(1L);
        paymentRequest.setPaymentMethod("CREDIT_CARD");
        paymentRequest.setAmount(100);

        testOrder.setStatus(Order.Status.PAID);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        assertThrows(BusinessException.class, () -> paymentService.initiatePayment(1L, paymentRequest));
    }

    @Test
    void initiatePayment_ExpiredCard_ShouldRecordAsFailed() {
        // 準備過期的信用卡資料（2020年1月過期）
        PaymentRequest paymentRequest = new PaymentRequest();
        paymentRequest.setOrderId(1L);
        paymentRequest.setPaymentMethod("CREDIT_CARD");
        paymentRequest.setAmount(100);
        paymentRequest.setCardExpiry("01/20"); // 已過期
        paymentRequest.setCardCvv("123");
        paymentRequest.setCardName("Test User");

        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.setId(1L);
            return payment;
        });
        when(orderEventRepository.save(any(OrderEvent.class))).thenReturn(new OrderEvent());

        // 執行付款
        PaymentDTO result = paymentService.initiatePayment(1L, paymentRequest);

        // 驗證付款狀態為 FAILED
        assertNotNull(result);
        assertEquals("FAILED", result.getStatus());

        // 驗證付款記錄被儲存（狀態為 FAILED）
        verify(paymentRepository).save(argThat(payment ->
            payment.getStatus() == Payment.Status.FAILED
        ));

        // 驗證訂單狀態沒有被更新為 PAID
        verify(orderRepository, never()).save(any(Order.class));

        // 驗證有建立 PAYMENT_FAILED 事件
        verify(orderEventRepository).save(any(OrderEvent.class));
    }

    @Test
    void initiatePayment_ValidCard_ShouldSucceed() {
        // 準備未過期的信用卡資料（2030年12月過期）
        PaymentRequest paymentRequest = new PaymentRequest();
        paymentRequest.setOrderId(1L);
        paymentRequest.setPaymentMethod("CREDIT_CARD");
        paymentRequest.setAmount(100);
        paymentRequest.setCardExpiry("12/30"); // 未過期
        paymentRequest.setCardCvv("123");
        paymentRequest.setCardName("Test User");

        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.setId(1L);
            return payment;
        });
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        when(orderEventRepository.save(any(OrderEvent.class))).thenReturn(new OrderEvent());

        // 執行付款
        PaymentDTO result = paymentService.initiatePayment(1L, paymentRequest);

        // 驗證付款成功
        assertNotNull(result);

        // 驗證付款記錄被儲存（狀態為 SUCCESS）
        verify(paymentRepository).save(argThat(payment ->
            payment.getStatus() == Payment.Status.SUCCESS
        ));

        // 驗證訂單狀態被更新為 PAID
        verify(orderRepository).save(argThat(order ->
            order.getStatus() == Order.Status.PAID
        ));

        // 驗證有建立 PAID 事件
        verify(orderEventRepository).save(any(OrderEvent.class));
    }

    @Test
    void initiatePayment_NonCreditCard_ShouldNotCheckExpiry() {
        // 測試非信用卡付款方式不檢查有效期限
        PaymentRequest paymentRequest = new PaymentRequest();
        paymentRequest.setOrderId(1L);
        paymentRequest.setPaymentMethod("CASH");
        paymentRequest.setAmount(100);
        // 不設定 cardExpiry

        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.setId(1L);
            return payment;
        });
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        when(orderEventRepository.save(any(OrderEvent.class))).thenReturn(new OrderEvent());

        // 執行付款
        PaymentDTO result = paymentService.initiatePayment(1L, paymentRequest);

        // 驗證付款成功
        assertNotNull(result);

        // 驗證付款記錄被儲存（狀態為 SUCCESS）
        verify(paymentRepository).save(argThat(payment ->
            payment.getStatus() == Payment.Status.SUCCESS
        ));
    }

    @Test
    void completePayment_Success() {
        testPayment.setStatus(Payment.Status.PENDING);
        when(paymentRepository.findByTransactionId("TXN-001")).thenReturn(Optional.of(testPayment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        when(orderEventRepository.save(any(OrderEvent.class))).thenReturn(new OrderEvent());

        PaymentDTO result = paymentService.completePayment(1L, "TXN-001");

        assertNotNull(result);
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void completePayment_NotFound() {
        when(paymentRepository.findByTransactionId("INVALID")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> paymentService.completePayment(1L, "INVALID"));
    }

    @Test
    void completePayment_InvalidStatus() {
        testPayment.setStatus(Payment.Status.SUCCESS);
        when(paymentRepository.findByTransactionId("TXN-001")).thenReturn(Optional.of(testPayment));

        assertThrows(BusinessException.class,
            () -> paymentService.completePayment(1L, "TXN-001"));
    }

    @Test
    void initiatePayment_InvalidCardExpiryFormat_ShouldRecordAsFailed() {
        // 測試無效格式：0101（缺少斜線）
        PaymentRequest paymentRequest = new PaymentRequest();
        paymentRequest.setOrderId(1L);
        paymentRequest.setPaymentMethod("CREDIT_CARD");
        paymentRequest.setAmount(100);
        paymentRequest.setCardExpiry("0101"); // 錯誤格式
        paymentRequest.setCardCvv("123");
        paymentRequest.setCardName("Test User");

        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.setId(1L);
            return payment;
        });
        when(orderEventRepository.save(any(OrderEvent.class))).thenReturn(new OrderEvent());

        // 執行付款
        PaymentDTO result = paymentService.initiatePayment(1L, paymentRequest);

        // 驗證付款狀態為 FAILED
        assertNotNull(result);
        assertEquals("FAILED", result.getStatus());

        // 驗證付款記錄被儲存（狀態為 FAILED）
        verify(paymentRepository).save(argThat(payment ->
            payment.getStatus() == Payment.Status.FAILED
        ));

        // 驗證訂單狀態沒有被更新為 PAID
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void initiatePayment_InvalidMonth_ShouldRecordAsFailed() {
        // 測試無效月份：13/25
        PaymentRequest paymentRequest = new PaymentRequest();
        paymentRequest.setOrderId(1L);
        paymentRequest.setPaymentMethod("CREDIT_CARD");
        paymentRequest.setAmount(100);
        paymentRequest.setCardExpiry("13/25"); // 月份超出範圍
        paymentRequest.setCardCvv("123");
        paymentRequest.setCardName("Test User");

        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.setId(1L);
            return payment;
        });
        when(orderEventRepository.save(any(OrderEvent.class))).thenReturn(new OrderEvent());

        // 執行付款
        PaymentDTO result = paymentService.initiatePayment(1L, paymentRequest);

        // 驗證付款狀態為 FAILED
        assertNotNull(result);
        assertEquals("FAILED", result.getStatus());

        // 驗證付款記錄被儲存（狀態為 FAILED）
        verify(paymentRepository).save(argThat(payment ->
            payment.getStatus() == Payment.Status.FAILED
        ));
    }

    @Test
    void initiatePayment_InvalidMonth00_ShouldRecordAsFailed() {
        // 測試無效月份：00/25
        PaymentRequest paymentRequest = new PaymentRequest();
        paymentRequest.setOrderId(1L);
        paymentRequest.setPaymentMethod("CREDIT_CARD");
        paymentRequest.setAmount(100);
        paymentRequest.setCardExpiry("00/25"); // 月份為 0
        paymentRequest.setCardCvv("123");
        paymentRequest.setCardName("Test User");

        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.setId(1L);
            return payment;
        });
        when(orderEventRepository.save(any(OrderEvent.class))).thenReturn(new OrderEvent());

        // 執行付款
        PaymentDTO result = paymentService.initiatePayment(1L, paymentRequest);

        // 驗證付款狀態為 FAILED
        assertNotNull(result);
        assertEquals("FAILED", result.getStatus());

        // 驗證付款記錄被儲存（狀態為 FAILED）
        verify(paymentRepository).save(argThat(payment ->
            payment.getStatus() == Payment.Status.FAILED
        ));
    }
}
