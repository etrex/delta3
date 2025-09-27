-- 清空現有資料
DELETE FROM order_events;
DELETE FROM payments;
DELETE FROM order_items;
DELETE FROM orders;
DELETE FROM products;
DELETE FROM users;

-- 重設自增ID
ALTER TABLE users ALTER COLUMN id RESTART WITH 1;
ALTER TABLE products ALTER COLUMN id RESTART WITH 5;  -- 避免與測試資料衝突
ALTER TABLE orders ALTER COLUMN id RESTART WITH 1;
ALTER TABLE order_items ALTER COLUMN id RESTART WITH 1;
ALTER TABLE payments ALTER COLUMN id RESTART WITH 1;
ALTER TABLE order_events ALTER COLUMN id RESTART WITH 1;

-- 插入測試用戶 (密碼都是 bcrypt 編碼後的 "password123")
INSERT INTO users (id, username, password, email, role, status) VALUES
(100, 'admin', '$2a$10$7o7PiYZwHwOrLp1b85WLk.hn603ncs5xWnfLovBtSj6IJ4DQ6p.u2', 'admin@example.com', 'ADMIN', 'ACTIVE'),
(101, 'customer1', '$2a$10$7o7PiYZwHwOrLp1b85WLk.hn603ncs5xWnfLovBtSj6IJ4DQ6p.u2', 'customer1@example.com', 'CUSTOMER', 'ACTIVE'),
(102, 'customer2', '$2a$10$7o7PiYZwHwOrLp1b85WLk.hn603ncs5xWnfLovBtSj6IJ4DQ6p.u2', 'customer2@example.com', 'CUSTOMER', 'ACTIVE');

-- 插入測試商品
INSERT INTO products (id, name, description, price, stock, status) VALUES
(1, '測試商品1', '這是測試商品1的描述', 199.99, 100, 'ACTIVE'),
(2, '測試商品2', '這是測試商品2的描述', 299.99, 50, 'ACTIVE'),
(3, '測試商品3', '這是測試商品3的描述', 399.99, 30, 'ACTIVE'),
(4, '下架商品', '這是下架商品的描述', 99.99, 10, 'INACTIVE');

