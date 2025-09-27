-- Copyright (c) 2025 Etrex Kuo. All rights reserved.

-- Insert sample users
INSERT INTO users (username, password, email, role, status) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 'admin@oms.com', 'ADMIN', 'ACTIVE'),
('customer1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 'customer1@example.com', 'CUSTOMER', 'ACTIVE'),
('customer2', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 'customer2@example.com', 'CUSTOMER', 'ACTIVE');

-- Insert sample products
INSERT INTO products (name, description, price, stock, status) VALUES
('MacBook Pro M4', '14-inch laptop with M4 Pro chip', 1999.99, 50, 'ACTIVE'),
('iPhone 15 Pro', 'Latest iPhone with A17 Pro chip', 999.99, 100, 'ACTIVE'),
('AirPods Pro', 'Wireless earbuds with noise cancellation', 249.99, 200, 'ACTIVE'),
('iPad Air', '10.9-inch tablet with M1 chip', 599.99, 75, 'ACTIVE'),
('Apple Watch Series 9', 'Smart watch with health tracking', 399.99, 150, 'ACTIVE'),
('Magic Keyboard', 'Wireless keyboard with Touch ID', 149.99, 300, 'ACTIVE'),
('Magic Mouse', 'Wireless multi-touch mouse', 79.99, 250, 'ACTIVE'),
('USB-C Cable', 'Fast charging cable 2m', 29.99, 500, 'ACTIVE'),
('MagSafe Charger', 'Wireless charger for iPhone', 39.99, 400, 'ACTIVE'),
('Studio Display', '27-inch 5K Retina display', 1599.99, 30, 'ACTIVE');