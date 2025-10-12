-- Copyright (c) 2025 Etrex Kuo. All rights reserved.

-- Insert sample users (password: password123)
INSERT INTO users (username, password, email, role, status) VALUES
('admin', '$2a$10$8bmLWDLqjDShmzWzECfKCuAbkQkx7EGifa04vLP1XXqba7lH8Y7P6', 'admin@oms.com', 'ADMIN', 'ACTIVE'),
('customer1', '$2a$10$8bmLWDLqjDShmzWzECfKCuAbkQkx7EGifa04vLP1XXqba7lH8Y7P6', 'customer1@example.com', 'CUSTOMER', 'ACTIVE'),
('customer2', '$2a$10$8bmLWDLqjDShmzWzECfKCuAbkQkx7EGifa04vLP1XXqba7lH8Y7P6', 'customer2@example.com', 'CUSTOMER', 'ACTIVE');

-- Insert sample products (prices in TWD)
INSERT INTO products (name, description, price, stock, status) VALUES
('測試商品1', '這是測試用商品1的描述', 500, 10, 'ACTIVE'),
('測試商品2', '這是測試用商品2的描述', 800, 15, 'ACTIVE'),
('MacBook Pro M4', '14-inch laptop with M4 Pro chip', 65000, 50, 'ACTIVE'),
('iPhone 15 Pro', 'Latest iPhone with A17 Pro chip', 36900, 100, 'ACTIVE'),
('AirPods Pro', 'Wireless earbuds with noise cancellation', 7990, 200, 'ACTIVE'),
('iPad Air', '10.9-inch tablet with M1 chip', 19900, 75, 'ACTIVE'),
('Apple Watch Series 9', 'Smart watch with health tracking', 13900, 150, 'ACTIVE'),
('Magic Keyboard', 'Wireless keyboard with Touch ID', 4790, 300, 'ACTIVE'),
('Magic Mouse', 'Wireless multi-touch mouse', 2590, 250, 'ACTIVE'),
('USB-C Cable', 'Fast charging cable 2m', 590, 500, 'ACTIVE'),
('MagSafe Charger', 'Wireless charger for iPhone', 1290, 400, 'ACTIVE'),
('Studio Display', '27-inch 5K Retina display', 49900, 30, 'ACTIVE');