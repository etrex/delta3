/*
 * Copyright (c) 2025 Etrex Kuo. All rights reserved.
 */
package com.etrex.oms.service;

import com.etrex.oms.dto.*;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OrderEventRepository orderEventRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private ShippingRepository shippingRepository;

    @Mock
    private ProductService productService;

    @InjectMocks
    private OrderService orderService;

    private User testCustomer;
    private Product testProduct;
    private Order testOrder;
    private OrderItem testOrderItem;
    private Payment testPayment;
    private Shipping testShipping;

    @BeforeEach
    void setUp() {
        testCustomer = new User();
        testCustomer.setId(1L);
        testCustomer.setUsername("testuser");

        testProduct = new Product();
        testProduct.setId(1L);
        testProduct.setName("Test Product");
        testProduct.setPrice(new BigDecimal("99.99"));
        testProduct.setStock(100);

        testOrder = new Order();
        testOrder.setId(1L);
        testOrder.setOrderNo("ORD-001");
        testOrder.setCustomer(testCustomer);
        testOrder.setTotalAmount(new BigDecimal("99.99"));
        testOrder.setStatus(Order.Status.CREATED);
        testOrder.setCreatedAt(LocalDateTime.now());

        testOrderItem = new OrderItem();
        testOrderItem.setId(1L);
        testOrderItem.setOrder(testOrder);
        testOrderItem.setProduct(testProduct);
        testOrderItem.setQuantity(1);
        testOrderItem.setPrice(new BigDecimal("99.99"));

        testOrder.setItems(Arrays.asList(testOrderItem));

        testPayment = new Payment();
        testPayment.setId(1L);
        testPayment.setOrder(testOrder);
        testPayment.setAmount(new BigDecimal("99.99"));
        testPayment.setStatus(Payment.Status.SUCCESS);
        testPayment.setPaymentMethod(Payment.PaymentMethod.CREDIT_CARD);
        testPayment.setTransactionId("TXN-001");

        testShipping = new Shipping();
        testShipping.setId(1L);
        testShipping.setOrder(testOrder);
        testShipping.setStatus(Shipping.Status.APPROVED);
        testShipping.setTrackingNumber("TRACK-001");
    }

    @Test
    void createOrder_Success() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerId(1L);

        CreateOrderRequest.OrderItemRequest itemRequest = new CreateOrderRequest.OrderItemRequest();
        itemRequest.setProductId(1L);
        itemRequest.setQuantity(1);
        request.setItems(Arrays.asList(itemRequest));

        when(userRepository.findById(1L)).thenReturn(Optional.of(testCustomer));
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(productService.checkStock(1L, 1)).thenReturn(true);
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        when(orderEventRepository.save(any(OrderEvent.class))).thenReturn(new OrderEvent());

        OrderDTO result = orderService.createOrder(request);

        assertNotNull(result);
        assertEquals("ORD-001", result.getOrderNo());
        verify(userRepository).findById(1L);
        verify(productRepository).findById(1L);
        verify(productService).checkStock(1L, 1);
        verify(productService).updateStock(1L, 1);
        verify(orderRepository).save(any(Order.class));
        verify(orderEventRepository).save(any(OrderEvent.class));
    }

    @Test
    void createOrder_CustomerNotFound() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerId(999L);
        request.setItems(Arrays.asList());

        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> orderService.createOrder(request));
    }

    @Test
    void createOrder_ProductNotFound() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerId(1L);

        CreateOrderRequest.OrderItemRequest itemRequest = new CreateOrderRequest.OrderItemRequest();
        itemRequest.setProductId(999L);
        itemRequest.setQuantity(1);
        request.setItems(Arrays.asList(itemRequest));

        when(userRepository.findById(1L)).thenReturn(Optional.of(testCustomer));
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> orderService.createOrder(request));
    }

    @Test
    void createOrder_InsufficientStock() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerId(1L);

        CreateOrderRequest.OrderItemRequest itemRequest = new CreateOrderRequest.OrderItemRequest();
        itemRequest.setProductId(1L);
        itemRequest.setQuantity(200);
        request.setItems(Arrays.asList(itemRequest));

        when(userRepository.findById(1L)).thenReturn(Optional.of(testCustomer));
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(productService.checkStock(1L, 200)).thenReturn(false);

        assertThrows(BusinessException.class, () -> orderService.createOrder(request));
    }

    @Test
    void getOrderById_Success() {
        when(orderRepository.findByIdWithItems(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.findByIdWithPayments(1L)).thenReturn(Optional.of(testOrder));

        OrderDTO result = orderService.getOrderById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("ORD-001", result.getOrderNo());
    }

    @Test
    void getOrderById_NotFound() {
        when(orderRepository.findByIdWithItems(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> orderService.getOrderById(999L));
    }

    @Test
    void getOrders_WithCustomerIdAndStatus() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Order> orderPage = new PageImpl<>(Arrays.asList(testOrder));

        when(userRepository.findById(1L)).thenReturn(Optional.of(testCustomer));
        when(orderRepository.findByCustomerAndStatus(testCustomer, Order.Status.CREATED, pageable))
                .thenReturn(orderPage);

        Page<OrderDTO> result = orderService.getOrders(1L, "CREATED", pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getOrders_WithCustomerIdOnly() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Order> orderPage = new PageImpl<>(Arrays.asList(testOrder));

        when(userRepository.findById(1L)).thenReturn(Optional.of(testCustomer));
        when(orderRepository.findByCustomer(testCustomer, pageable)).thenReturn(orderPage);

        Page<OrderDTO> result = orderService.getOrders(1L, null, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getOrders_WithStatusOnly() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Order> orderPage = new PageImpl<>(Arrays.asList(testOrder));

        when(orderRepository.findByStatus(Order.Status.CREATED, pageable)).thenReturn(orderPage);

        Page<OrderDTO> result = orderService.getOrders(null, "CREATED", pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getOrders_WithoutFilters() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Order> orderPage = new PageImpl<>(Arrays.asList(testOrder));

        when(orderRepository.findAll(pageable)).thenReturn(orderPage);

        Page<OrderDTO> result = orderService.getOrders(null, null, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void initiatePayment_Success() {
        PaymentDTO paymentDTO = new PaymentDTO();
        paymentDTO.setPaymentMethod("CREDIT_CARD");

        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        when(orderEventRepository.save(any(OrderEvent.class))).thenReturn(new OrderEvent());

        PaymentDTO result = orderService.initiatePayment(1L, paymentDTO);

        assertNotNull(result);
        assertEquals("CREDIT_CARD", result.getPaymentMethod());
        verify(paymentRepository).save(any(Payment.class));
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void initiatePayment_OrderNotFound() {
        PaymentDTO paymentDTO = new PaymentDTO();
        paymentDTO.setPaymentMethod("CREDIT_CARD");

        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> orderService.initiatePayment(999L, paymentDTO));
    }

    @Test
    void initiatePayment_InvalidStatus() {
        PaymentDTO paymentDTO = new PaymentDTO();
        paymentDTO.setPaymentMethod("CREDIT_CARD");

        testOrder.setStatus(Order.Status.PAID);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        assertThrows(BusinessException.class, () -> orderService.initiatePayment(1L, paymentDTO));
    }

    @Test
    void completePayment_Success() {
        testPayment.setStatus(Payment.Status.PENDING);
        when(paymentRepository.findByTransactionId("TXN-001")).thenReturn(Optional.of(testPayment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        when(orderEventRepository.save(any(OrderEvent.class))).thenReturn(new OrderEvent());

        PaymentDTO result = orderService.completePayment(1L, "TXN-001");

        assertNotNull(result);
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void completePayment_NotFound() {
        when(paymentRepository.findByTransactionId("TXN-999")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> orderService.completePayment(1L, "TXN-999"));
    }

    @Test
    void completePayment_InvalidStatus() {
        testPayment.setStatus(Payment.Status.SUCCESS);
        when(paymentRepository.findByTransactionId("TXN-001")).thenReturn(Optional.of(testPayment));

        assertThrows(BusinessException.class, () -> orderService.completePayment(1L, "TXN-001"));
    }

    @Test
    void cancelOrder_Success_Created() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);
        when(orderEventRepository.save(any(OrderEvent.class))).thenReturn(new OrderEvent());

        OrderDTO result = orderService.cancelOrder(1L);

        assertNotNull(result);
        verify(productRepository).save(any(Product.class));
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void cancelOrder_Success_Paid() {
        testOrder.setStatus(Order.Status.PAID);
        List<Payment> payments = Arrays.asList(testPayment);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(paymentRepository.findByOrderAndStatus(testOrder, Payment.Status.SUCCESS)).thenReturn(payments);
        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        when(orderEventRepository.save(any(OrderEvent.class))).thenReturn(new OrderEvent());

        OrderDTO result = orderService.cancelOrder(1L);

        assertNotNull(result);
        verify(paymentRepository).save(any(Payment.class));
        verify(orderEventRepository, times(2)).save(any(OrderEvent.class));
    }

    @Test
    void cancelOrder_InvalidStatus() {
        testOrder.setStatus(Order.Status.SHIPPED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        assertThrows(BusinessException.class, () -> orderService.cancelOrder(1L));
    }

    @Test
    void shipOrder_Success() {
        testOrder.setStatus(Order.Status.PAID);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        when(orderEventRepository.save(any(OrderEvent.class))).thenReturn(new OrderEvent());

        OrderDTO result = orderService.shipOrder(1L);

        assertNotNull(result);
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void shipOrder_InvalidStatus() {
        testOrder.setStatus(Order.Status.CREATED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        assertThrows(BusinessException.class, () -> orderService.shipOrder(1L));
    }

    @Test
    void getOrderByOrderNo_Success() {
        when(orderRepository.findByOrderNoWithItems("ORD-001")).thenReturn(Optional.of(testOrder));
        when(orderRepository.findByOrderNoWithPayments("ORD-001")).thenReturn(Optional.of(testOrder));

        OrderDTO result = orderService.getOrderByOrderNo("ORD-001");

        assertNotNull(result);
        assertEquals("ORD-001", result.getOrderNo());
    }

    @Test
    void getOrderByOrderNo_NotFound() {
        when(orderRepository.findByOrderNoWithItems("ORD-999")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> orderService.getOrderByOrderNo("ORD-999"));
    }

    @Test
    void approveOrder_Success() {
        testOrder.setStatus(Order.Status.PAID);
        when(orderRepository.findByOrderNo("ORD-001")).thenReturn(Optional.of(testOrder));
        when(shippingRepository.save(any(Shipping.class))).thenReturn(testShipping);
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        when(orderEventRepository.save(any(OrderEvent.class))).thenReturn(new OrderEvent());

        OrderDTO result = orderService.approveOrder("ORD-001");

        assertNotNull(result);
        verify(shippingRepository).save(any(Shipping.class));
    }

    @Test
    void approveOrder_WithExistingShipping() {
        testOrder.setStatus(Order.Status.PAID);
        testOrder.setShipping(testShipping);
        when(orderRepository.findByOrderNo("ORD-001")).thenReturn(Optional.of(testOrder));
        when(shippingRepository.save(any(Shipping.class))).thenReturn(testShipping);
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        when(orderEventRepository.save(any(OrderEvent.class))).thenReturn(new OrderEvent());

        OrderDTO result = orderService.approveOrder("ORD-001");

        assertNotNull(result);
        verify(shippingRepository).save(any(Shipping.class));
    }

    @Test
    void approveOrder_InvalidStatus() {
        testOrder.setStatus(Order.Status.CREATED);
        when(orderRepository.findByOrderNo("ORD-001")).thenReturn(Optional.of(testOrder));

        assertThrows(BusinessException.class, () -> orderService.approveOrder("ORD-001"));
    }

    @Test
    void shipOrderWithDetails_Success() {
        testOrder.setStatus(Order.Status.PAID);
        LocalDateTime estimatedDelivery = LocalDateTime.now().plusDays(3);

        when(orderRepository.findByOrderNo("ORD-001")).thenReturn(Optional.of(testOrder));
        when(shippingRepository.save(any(Shipping.class))).thenReturn(testShipping);
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        when(orderEventRepository.save(any(OrderEvent.class))).thenReturn(new OrderEvent());

        OrderDTO result = orderService.shipOrderWithDetails("ORD-001", "TRACK-001", "UPS", estimatedDelivery, "Handle with care");

        assertNotNull(result);
        verify(shippingRepository).save(any(Shipping.class));
    }

    @Test
    void shipOrderWithDetails_WithExistingShipping() {
        testOrder.setStatus(Order.Status.APPROVED);
        testOrder.setShipping(testShipping);
        LocalDateTime estimatedDelivery = LocalDateTime.now().plusDays(3);

        when(orderRepository.findByOrderNo("ORD-001")).thenReturn(Optional.of(testOrder));
        when(shippingRepository.save(any(Shipping.class))).thenReturn(testShipping);
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        when(orderEventRepository.save(any(OrderEvent.class))).thenReturn(new OrderEvent());

        OrderDTO result = orderService.shipOrderWithDetails("ORD-001", "TRACK-002", "FedEx", estimatedDelivery, "Fragile");

        assertNotNull(result);
        verify(shippingRepository).save(any(Shipping.class));
    }

    @Test
    void deliverOrder_Success() {
        testOrder.setStatus(Order.Status.SHIPPED);
        testOrder.setShipping(testShipping);
        LocalDateTime deliveryDate = LocalDateTime.now();

        when(orderRepository.findByOrderNo("ORD-001")).thenReturn(Optional.of(testOrder));
        when(shippingRepository.save(any(Shipping.class))).thenReturn(testShipping);
        when(orderEventRepository.save(any(OrderEvent.class))).thenReturn(new OrderEvent());

        OrderDTO result = orderService.deliverOrder("ORD-001", deliveryDate, "Delivered successfully");

        assertNotNull(result);
        verify(shippingRepository).save(any(Shipping.class));
    }

    @Test
    void deliverOrder_WithNullDeliveryDate() {
        testOrder.setStatus(Order.Status.SHIPPED);
        testOrder.setShipping(testShipping);

        when(orderRepository.findByOrderNo("ORD-001")).thenReturn(Optional.of(testOrder));
        when(shippingRepository.save(any(Shipping.class))).thenReturn(testShipping);
        when(orderEventRepository.save(any(OrderEvent.class))).thenReturn(new OrderEvent());

        OrderDTO result = orderService.deliverOrder("ORD-001", null, "Delivered");

        assertNotNull(result);
        verify(shippingRepository).save(any(Shipping.class));
    }

    @Test
    void deliverOrder_WithExistingNotes() {
        testOrder.setStatus(Order.Status.SHIPPED);
        testShipping.setNotes("Previous note");
        testOrder.setShipping(testShipping);

        when(orderRepository.findByOrderNo("ORD-001")).thenReturn(Optional.of(testOrder));
        when(shippingRepository.save(any(Shipping.class))).thenReturn(testShipping);
        when(orderEventRepository.save(any(OrderEvent.class))).thenReturn(new OrderEvent());

        OrderDTO result = orderService.deliverOrder("ORD-001", null, "New note");

        assertNotNull(result);
        verify(shippingRepository).save(any(Shipping.class));
    }

    @Test
    void deliverOrder_InvalidStatus() {
        testOrder.setStatus(Order.Status.PAID);
        when(orderRepository.findByOrderNo("ORD-001")).thenReturn(Optional.of(testOrder));

        assertThrows(BusinessException.class, () -> orderService.deliverOrder("ORD-001", null, null));
    }

    @Test
    void deliverOrder_NoShipping() {
        testOrder.setStatus(Order.Status.SHIPPED);
        testOrder.setShipping(null);
        when(orderRepository.findByOrderNo("ORD-001")).thenReturn(Optional.of(testOrder));

        assertThrows(BusinessException.class, () -> orderService.deliverOrder("ORD-001", null, null));
    }
}
