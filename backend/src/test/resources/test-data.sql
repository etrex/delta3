-- 清空現有資料
DELETE FROM order_events;
DELETE FROM payments;
DELETE FROM order_items;
DELETE FROM orders;
DELETE FROM products;
DELETE FROM users;

-- 插入測試用戶 (密碼都是 bcrypt 編碼後的 "password123")
INSERT INTO users (id, username, password, email, role, status) VALUES
(1, 'admin', '$2a$10$cEfMbZfPhCJmFGGgp/qUAu2MrjyLlG9jF4Lh3vVLKA4zOKAa9pKda', 'admin@example.com', 'ADMIN', 'ACTIVE'),
(2, 'customer1', '$2a$10$cEfMbZfPhCJmFGGgp/qUAu2MrjyLlG9jF4Lh3vVLKA4zOKAa9pKda', 'customer1@example.com', 'CUSTOMER', 'ACTIVE'),
(3, 'customer2', '$2a$10$cEfMbZfPhCJmFGGgp/qUAu2MrjyLlG9jF4Lh3vVLKA4zOKAa9pKda', 'customer2@example.com', 'CUSTOMER', 'ACTIVE');

-- 插入測試商品
INSERT INTO products (id, name, description, price, stock, status) VALUES
(1, '測試商品1', '這是測試商品1的描述', 199.99, 100, 'ACTIVE'),
(2, '測試商品2', '這是測試商品2的描述', 299.99, 50, 'ACTIVE'),
(3, '測試商品3', '這是測試商品3的描述', 399.99, 30, 'ACTIVE'),
(4, '下架商品', '這是下架商品的描述', 99.99, 10, 'INACTIVE');

-- 插入測試訂單
INSERT INTO orders (id, customer_id, total_amount, status, created_at) VALUES
(1, 2, 199.99, 'CREATED', CURRENT_TIMESTAMP),
(2, 2, 299.99, 'PAID', CURRENT_TIMESTAMP),
(3, 3, 399.99, 'SHIPPED', CURRENT_TIMESTAMP);

-- 插入訂單明細
INSERT INTO order_items (id, order_id, product_id, quantity, price) VALUES
(1, 1, 1, 1, 199.99),
(2, 2, 2, 1, 299.99),
(3, 3, 3, 1, 399.99);

-- 插入支付記錄
INSERT INTO payments (id, order_id, payment_method, amount, status, created_at) VALUES
(1, 2, 'CREDIT_CARD', 299.99, 'SUCCESS', CURRENT_TIMESTAMP),
(2, 3, 'CREDIT_CARD', 399.99, 'SUCCESS', CURRENT_TIMESTAMP);

-- 插入訂單事件
INSERT INTO order_events (id, order_id, event_type, message, created_at) VALUES
(1, 1, 'CREATED', '訂單已建立', CURRENT_TIMESTAMP),
(2, 2, 'CREATED', '訂單已建立', CURRENT_TIMESTAMP),
(3, 2, 'PAID', '訂單已付款', CURRENT_TIMESTAMP),
(4, 3, 'CREATED', '訂單已建立', CURRENT_TIMESTAMP),
(5, 3, 'PAID', '訂單已付款', CURRENT_TIMESTAMP),
(6, 3, 'SHIPPED', '訂單已出貨', CURRENT_TIMESTAMP);