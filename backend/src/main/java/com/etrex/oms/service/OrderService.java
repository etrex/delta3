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
    private final ShippingRepository shippingRepository;
    private final ProductService productService;

    public OrderDTO createOrder(CreateOrderRequest request) {
        User customer = userRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        Order order = new Order();
        order.setOrderNo(generateOrderNo());
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
        Order order = orderRepository.findByIdWithItems(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        // Load payments separately to avoid MultipleBagFetchException
        Order orderWithPayments = orderRepository.findByIdWithPayments(id).orElse(order);
        order.setPayments(orderWithPayments.getPayments());

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
        payment.setStatus(Payment.Status.SUCCESS); // For testing, directly set to SUCCESS
        payment.setTransactionId(UUID.randomUUID().toString());
        payment.setPaidAt(LocalDateTime.now()); // Set paid time

        Payment savedPayment = paymentRepository.save(payment);

        // Update order status to PAID
        order.setStatus(Order.Status.PAID);
        orderRepository.save(order);

        createOrderEvent(order, "PAID", "Payment completed with method: " + paymentDTO.getPaymentMethod());

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

    public OrderDTO getOrderByOrderNo(String orderNo) {
        Order order = orderRepository.findByOrderNoWithItems(orderNo)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        // Load payments and shipping separately
        Order orderWithPayments = orderRepository.findByOrderNoWithPayments(orderNo).orElse(order);
        order.setPayments(orderWithPayments.getPayments());

        return convertToDTO(order);
    }

    public OrderDTO approveOrder(String orderNo) {
        Order order = orderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (order.getStatus() != Order.Status.PAID && order.getStatus() != Order.Status.CREATED) {
            throw new BusinessException("Only created or paid orders can be approved");
        }

        order.setStatus(Order.Status.APPROVED);

        // Create or update shipping
        Shipping shipping = order.getShipping();
        if (shipping == null) {
            shipping = new Shipping();
            shipping.setOrder(order);
            order.setShipping(shipping);
        }
        shipping.setStatus(Shipping.Status.APPROVED);
        shippingRepository.save(shipping);

        Order savedOrder = orderRepository.save(order);
        createOrderEvent(savedOrder, "APPROVED", "Order approved for shipping");

        return convertToDTO(savedOrder);
    }

    public OrderDTO shipOrderWithDetails(String orderNo, String trackingNumber, String carrier,
                                         LocalDateTime estimatedDelivery, String notes) {
        Order order = orderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (order.getStatus() != Order.Status.PAID && order.getStatus() != Order.Status.APPROVED) {
            throw new BusinessException("Order cannot be shipped in current status: " + order.getStatus());
        }

        order.setStatus(Order.Status.SHIPPED);

        // Create or update shipping
        Shipping shipping = order.getShipping();
        if (shipping == null) {
            shipping = new Shipping();
            shipping.setOrder(order);
            order.setShipping(shipping);
        }
        shipping.setStatus(Shipping.Status.SHIPPED);
        shipping.setTrackingNumber(trackingNumber);
        shipping.setCarrier(carrier);
        shipping.setEstimatedDelivery(estimatedDelivery);
        shipping.setShippedAt(LocalDateTime.now());
        shipping.setNotes(notes);
        shippingRepository.save(shipping);

        Order savedOrder = orderRepository.save(order);
        createOrderEvent(savedOrder, "SHIPPED", "Order shipped with tracking: " + trackingNumber);

        return convertToDTO(savedOrder);
    }

    public OrderDTO deliverOrder(String orderNo, LocalDateTime deliveredDate, String notes) {
        Order order = orderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (order.getStatus() != Order.Status.SHIPPED) {
            throw new BusinessException("Only shipped orders can be marked as delivered");
        }

        Shipping shipping = order.getShipping();
        if (shipping == null) {
            throw new BusinessException("Shipping information not found");
        }

        shipping.setStatus(Shipping.Status.DELIVERED);
        shipping.setDeliveredAt(deliveredDate != null ? deliveredDate : LocalDateTime.now());
        if (notes != null) {
            shipping.setNotes(shipping.getNotes() != null ? shipping.getNotes() + "\n" + notes : notes);
        }
        shippingRepository.save(shipping);

        createOrderEvent(order, "DELIVERED", "Order delivered successfully");

        return convertToDTO(order);
    }

    private String generateOrderNo() {
        return "ORD-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private OrderDTO convertToDTO(Order order) {
        OrderDTO dto = new OrderDTO();
        dto.setId(order.getId());
        dto.setOrderNo(order.getOrderNo());
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

        if (order.getShipping() != null) {
            dto.setShipping(convertToShippingDTO(order.getShipping()));
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

    private ShippingDTO convertToShippingDTO(Shipping shipping) {
        ShippingDTO dto = new ShippingDTO();
        dto.setId(shipping.getId());
        dto.setOrderId(shipping.getOrder().getId());
        dto.setStatus(shipping.getStatus().name());
        dto.setTrackingNumber(shipping.getTrackingNumber());
        dto.setCarrier(shipping.getCarrier());
        dto.setEstimatedDelivery(shipping.getEstimatedDelivery());
        dto.setShippedAt(shipping.getShippedAt());
        dto.setDeliveredAt(shipping.getDeliveredAt());
        dto.setNotes(shipping.getNotes());
        return dto;
    }

    // ========== Cart Methods ==========

    @Transactional
    public OrderDTO getOrCreateCart(User user) {
        // Find existing cart
        Order cart = orderRepository.findAll().stream()
                .filter(o -> o.getCustomer().getId().equals(user.getId()))
                .filter(o -> o.getStatus() == Order.Status.CART)
                .findFirst()
                .orElseGet(() -> {
                    // Create new cart with order number
                    Order newCart = new Order();
                    newCart.setCustomer(user);
                    newCart.setStatus(Order.Status.CART);
                    newCart.setOrderNo(generateOrderNo());  // Generate order number at creation
                    newCart.setTotalAmount(BigDecimal.ZERO);
                    return orderRepository.save(newCart);
                });

        return convertToDTO(cart);
    }

    @Transactional
    public OrderDTO addToCart(User user, Long productId, Integer quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found: " + productId));

        // Validate stock
        if (product.getStock() < quantity) {
            throw new BusinessException("Insufficient stock for product: " + product.getName());
        }

        // Get or create cart
        Order cart = orderRepository.findAll().stream()
                .filter(o -> o.getCustomer().getId().equals(user.getId()))
                .filter(o -> o.getStatus() == Order.Status.CART)
                .findFirst()
                .orElseGet(() -> {
                    Order newCart = new Order();
                    newCart.setCustomer(user);
                    newCart.setStatus(Order.Status.CART);
                    newCart.setOrderNo(generateOrderNo());  // Generate order number at creation
                    newCart.setTotalAmount(BigDecimal.ZERO);
                    return orderRepository.save(newCart);
                });

        // Check if product already in cart
        OrderItem existingItem = cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(productId))
                .findFirst()
                .orElse(null);

        if (existingItem != null) {
            // Update quantity
            existingItem.setQuantity(existingItem.getQuantity() + quantity);
        } else {
            // Add new item
            OrderItem newItem = new OrderItem();
            newItem.setOrder(cart);
            newItem.setProduct(product);
            newItem.setQuantity(quantity);
            newItem.setPrice(product.getPrice());
            cart.getItems().add(newItem);
        }

        // Recalculate total
        recalculateCartTotal(cart);

        return convertToDTO(orderRepository.save(cart));
    }

    @Transactional
    public OrderDTO updateCartItem(User user, Long itemId, Integer quantity) {
        Order cart = orderRepository.findAll().stream()
                .filter(o -> o.getCustomer().getId().equals(user.getId()))
                .filter(o -> o.getStatus() == Order.Status.CART)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Cart not found"));

        OrderItem item = cart.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Cart item not found"));

        if (quantity <= 0) {
            cart.getItems().remove(item);
        } else {
            item.setQuantity(quantity);
        }

        recalculateCartTotal(cart);
        return convertToDTO(orderRepository.save(cart));
    }

    @Transactional
    public void removeCartItem(User user, Long itemId) {
        Order cart = orderRepository.findAll().stream()
                .filter(o -> o.getCustomer().getId().equals(user.getId()))
                .filter(o -> o.getStatus() == Order.Status.CART)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Cart not found"));

        // Find and remove the item
        OrderItem itemToRemove = cart.getItems().stream()
                .filter(item -> item.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Cart item not found"));

        cart.getItems().remove(itemToRemove);
        itemToRemove.setOrder(null); // Clear bidirectional relationship

        recalculateCartTotal(cart);
        orderRepository.save(cart);
    }

    @Transactional
    public OrderDTO checkoutCart(User user) {
        Order cart = orderRepository.findAll().stream()
                .filter(o -> o.getCustomer().getId().equals(user.getId()))
                .filter(o -> o.getStatus() == Order.Status.CART)
                .findFirst()
                .orElseThrow(() -> new BusinessException("Cart not found"));

        if (cart.getItems().isEmpty()) {
            throw new BusinessException("Cart is empty");
        }

        // Validate stock for all items
        for (OrderItem item : cart.getItems()) {
            Product product = item.getProduct();
            if (product.getStock() < item.getQuantity()) {
                throw new BusinessException("Insufficient stock for product: " + product.getName());
            }
        }

        // Deduct stock
        for (OrderItem item : cart.getItems()) {
            Product product = item.getProduct();
            product.setStock(product.getStock() - item.getQuantity());
            productRepository.save(product);
        }

        // Convert cart to order (CART -> CREATED)
        // Order number was already generated when cart was created
        cart.setStatus(Order.Status.CREATED);

        // Create order event
        OrderEvent event = new OrderEvent();
        event.setOrder(cart);
        event.setEventType("CREATED");
        event.setMessage("Order created from cart");
        orderEventRepository.save(event);

        return convertToDTO(orderRepository.save(cart));
    }

    private void recalculateCartTotal(Order cart) {
        BigDecimal total = cart.getItems().stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        cart.setTotalAmount(total);
    }
}