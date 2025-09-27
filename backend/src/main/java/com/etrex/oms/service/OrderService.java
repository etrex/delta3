/*
 * Copyright (c) 2025 Etrex Kuo. All rights reserved.
 */
package com.etrex.oms.service;

import com.etrex.oms.dto.*;
import com.etrex.oms.entity.*;
import com.etrex.oms.exception.ResourceNotFoundException;
import com.etrex.oms.exception.BusinessException;
import com.etrex.oms.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderEventRepository orderEventRepository;
    private final PaymentRepository paymentRepository;
    private final ProductService productService;

    public OrderDTO createOrder(CreateOrderRequest request) {
        User customer = userRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        Order order = new Order();
        order.setCustomer(customer);
        order.setStatus(Order.Status.CREATED);

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> items = new ArrayList<>();

        for (CreateOrderRequest.OrderItemRequest itemDTO : request.getItems()) {
            Product product = productRepository.findById(itemDTO.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

            if (!productService.checkStock(product.getId(), itemDTO.getQuantity())) {
                throw new BusinessException("Insufficient stock for product: " + product.getName());
            }

            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProduct(product);
            item.setQuantity(itemDTO.getQuantity());
            item.setPrice(product.getPrice());

            items.add(item);
            totalAmount = totalAmount.add(product.getPrice().multiply(BigDecimal.valueOf(itemDTO.getQuantity())));

            productService.updateStock(product.getId(), itemDTO.getQuantity());
        }

        order.setItems(items);
        order.setTotalAmount(totalAmount);

        Order savedOrder = orderRepository.save(order);

        createOrderEvent(savedOrder, "CREATED", "Order created successfully");

        return convertToDTO(savedOrder);
    }

    public OrderDTO getOrderById(Long id) {
        Order order = orderRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        return convertToDTO(order);
    }

    public Page<OrderDTO> getOrders(Long customerId, String status, Pageable pageable) {
        Page<Order> orders;

        if (customerId != null && status != null) {
            User customer = userRepository.findById(customerId)
                    .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
            orders = orderRepository.findByCustomerAndStatus(customer, Order.Status.valueOf(status), pageable);
        } else if (customerId != null) {
            User customer = userRepository.findById(customerId)
                    .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
            orders = orderRepository.findByCustomer(customer, pageable);
        } else if (status != null) {
            orders = orderRepository.findByStatus(Order.Status.valueOf(status), pageable);
        } else {
            orders = orderRepository.findAll(pageable);
        }

        return orders.map(this::convertToDTO);
    }

    public PaymentDTO initiatePayment(Long orderId, PaymentDTO paymentDTO) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (order.getStatus() != Order.Status.CREATED) {
            throw new BusinessException("Order cannot be paid in current status: " + order.getStatus());
        }

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setPaymentMethod(Payment.PaymentMethod.valueOf(paymentDTO.getPaymentMethod()));
        payment.setAmount(order.getTotalAmount());
        payment.setStatus(Payment.Status.PENDING);
        payment.setTransactionId(UUID.randomUUID().toString());

        Payment savedPayment = paymentRepository.save(payment);

        createOrderEvent(order, "PAYMENT_INITIATED", "Payment initiated with method: " + paymentDTO.getPaymentMethod());

        return convertToPaymentDTO(savedPayment);
    }

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

        createOrderEvent(order, "PAID", "Payment completed successfully");

        return convertToPaymentDTO(savedPayment);
    }

    public OrderDTO cancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (order.getStatus() != Order.Status.CREATED && order.getStatus() != Order.Status.PAID) {
            throw new BusinessException("Order cannot be cancelled in current status: " + order.getStatus());
        }

        // Restore stock
        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            product.setStock(product.getStock() + item.getQuantity());
            productRepository.save(product);
        }

        // Handle refund if paid
        if (order.getStatus() == Order.Status.PAID) {
            List<Payment> payments = paymentRepository.findByOrderAndStatus(order, Payment.Status.SUCCESS);
            for (Payment payment : payments) {
                payment.setStatus(Payment.Status.REFUNDED);
                paymentRepository.save(payment);
            }
            createOrderEvent(order, "REFUNDED", "Order cancelled and payment refunded");
        }

        order.setStatus(Order.Status.CANCELLED);
        Order savedOrder = orderRepository.save(order);

        createOrderEvent(savedOrder, "CANCELLED", "Order cancelled");

        return convertToDTO(savedOrder);
    }

    public OrderDTO shipOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (order.getStatus() != Order.Status.PAID && order.getStatus() != Order.Status.APPROVED) {
            throw new BusinessException("Order cannot be shipped in current status: " + order.getStatus());
        }

        order.setStatus(Order.Status.SHIPPED);
        Order savedOrder = orderRepository.save(order);

        createOrderEvent(savedOrder, "SHIPPED", "Order has been shipped");

        return convertToDTO(savedOrder);
    }

    private void createOrderEvent(Order order, String eventType, String message) {
        OrderEvent event = new OrderEvent();
        event.setOrder(order);
        event.setEventType(eventType);
        event.setMessage(message);
        orderEventRepository.save(event);
    }

    private OrderDTO convertToDTO(Order order) {
        OrderDTO dto = new OrderDTO();
        dto.setId(order.getId());
        dto.setCustomerId(order.getCustomer().getId());
        dto.setCustomerName(order.getCustomer().getUsername());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setStatus(order.getStatus().name());
        dto.setCreatedAt(order.getCreatedAt());
        dto.setUpdatedAt(order.getUpdatedAt());

        if (order.getItems() != null) {
            dto.setItems(order.getItems().stream().map(this::convertToItemDTO).collect(Collectors.toList()));
        }

        if (order.getPayments() != null) {
            dto.setPayments(order.getPayments().stream().map(this::convertToPaymentDTO).collect(Collectors.toList()));
        }

        return dto;
    }

    private OrderItemDTO convertToItemDTO(OrderItem item) {
        OrderItemDTO dto = new OrderItemDTO();
        dto.setId(item.getId());
        dto.setProductId(item.getProduct().getId());
        dto.setProductName(item.getProduct().getName());
        dto.setQuantity(item.getQuantity());
        dto.setPrice(item.getPrice());
        return dto;
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
        dto.setCreatedAt(payment.getCreatedAt());
        return dto;
    }
}