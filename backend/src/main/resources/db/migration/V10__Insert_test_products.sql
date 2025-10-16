-- Copyright (c) 2025 Etrex Kuo. All rights reserved.

-- Insert test 3C products (15 products for testing RAG)
INSERT INTO products (name, description, price, stock, status) VALUES
-- Laptops
('華碩 ZenBook 14', '輕薄商務筆電，搭載 Intel Core i7 處理器、16GB RAM、512GB SSD，14吋 FHD 螢幕，重量僅 1.2kg，適合商務人士攜帶使用', 32900, 50, 'ACTIVE'),
('聯想 ThinkPad X1 Carbon', '頂級商務筆電，14吋 2K 觸控螢幕，Intel Core i7-1360P、32GB RAM、1TB SSD，通過軍規測試，耐用可靠', 54900, 30, 'ACTIVE'),
('MSI GS66 電競筆電', '專業電競筆電，搭載 RTX 4060 顯卡、Intel i7-13700H 處理器、16GB DDR5 RAM、512GB PCIe SSD，240Hz 電競螢幕', 58900, 20, 'ACTIVE'),

-- Smartphones
('三星 Galaxy S24 Ultra', '旗艦級智慧型手機，6.8吋 Dynamic AMOLED 2X 螢幕、200MP 主鏡頭、S Pen、5000mAh 大電池，支援 45W 快充', 42900, 100, 'ACTIVE'),
('iPhone 15 Pro Max', 'Apple 最新旗艦機，A17 Pro 晶片、6.7吋 ProMotion 螢幕、48MP 主鏡頭支援 5 倍光學變焦、鈦金屬機身', 43900, 80, 'ACTIVE'),
('小米 14 Pro', '性價比旗艦機，徠卡三鏡頭系統、Snapdragon 8 Gen 3 處理器、120W 快充、2K AMOLED 螢幕', 28900, 120, 'ACTIVE'),

-- Tablets
('iPad Air M2', 'Apple 平板電腦，搭載 M2 晶片，10.9吋 Liquid Retina 顯示器，支援 Apple Pencil 第二代，適合繪圖與筆記', 19900, 75, 'ACTIVE'),
('Samsung Galaxy Tab S9', 'Android 旗艦平板，11吋 Dynamic AMOLED 2X 螢幕、Snapdragon 8 Gen 2、附贈 S Pen、IP68 防水防塵', 24900, 60, 'ACTIVE'),

-- Headphones
('Sony WH-1000XM5', '頂級降噪耳機，業界領先的降噪技術、LDAC 高音質編碼、30小時續航、多點連線，適合通勤與長途飛行', 10990, 200, 'ACTIVE'),
('AirPods Pro 第三代', 'Apple 無線降噪耳機，H2 晶片、自適應降噪、空間音訊、USB-C 充電、IP54 防塵防水', 7990, 250, 'ACTIVE'),

-- Smart Watches
('Apple Watch Ultra 2', '專業運動智慧手錶，49mm 鈦金屬錶殼、雙頻 GPS、100米防水、最長 72 小時續航、適合極限運動', 28900, 80, 'ACTIVE'),
('Garmin Fenix 7 Pro', '專業運動手錶，支援超過 100 種運動模式、太陽能充電、衛星地圖導航、心率血氧監測', 26900, 50, 'ACTIVE'),

-- Accessories
('羅技 MX Master 3S', '專業無線滑鼠，支援多裝置連線、快速滾輪、人體工學設計、充電一次可用 70 天', 3290, 300, 'ACTIVE'),
('Apple Magic Keyboard', 'Apple 無線鍵盤，巧控鍵盤配備 Touch ID，可快速解鎖 Mac、鍵程舒適、充電式設計', 4790, 250, 'ACTIVE'),
('Anker 氮化鎵 65W 充電器', '小巧快充充電器，支援 PD 3.0、QC 4.0，可同時充 3 台裝置，體積比原廠充電器小 50%', 1290, 500, 'ACTIVE');
