-- 添加管理员用户（用户名为root，密码为root）
-- INSERT INTO users (username, password, address, email, deleted, created_time, updated_time)
-- VALUES ('root', 'root', '0x0000000000000000000000000000000000000000', 'root@example.com', 0, NOW(), NOW());

-- -- 添加普通用户
-- INSERT INTO users (username, password, address, email, deleted, created_time, updated_time)
-- VALUES ('jr', '123456', '0x0000000000000000000000000000000000000000', 'jr@example.com', 0, NOW(), NOW());

-- 添加测试数据
INSERT INTO auction_items (title, description, img_url, category, status, owner_address, user_id, start_price, current_price, created_time, updated_time, auction_start_time, auction_end_time)
VALUES ('jp2', '111', 'test-image-1.jpg', 'art', 'APPROVED', '0x0000000000000000000000000000000000000000', 1, 100.00, 100.00, NOW(), NOW(), NOW(), NOW());

-- INSERT INTO auction_items (title, description, img_url, category, status, owner_address, user_id, start_price, current_price, created_time, updated_time, auction_start_time, auction_end_time)
-- VALUES ('测试竞品2', '这是另一个测试竞品描述', 'test-image-2.jpg', '音乐', 'PENDING', '0x0000000000000000000000000000000000000000', 1, 100.00, 100.00, NOW(), NOW(), NOW(), NOW());

-- INSERT INTO auction_items (title, description, img_url, category, status, owner_address, user_id, start_price, current_price, created_time, updated_time, auction_start_time, auction_end_time)
-- VALUES ('测试竞品3', '这是一个已上架的测试竞品', 'test-image-3.jpg', '视频', 'LISTED', '0x0000000000000000000000000000000000000000', 1, 200.00, 200.00, NOW(), NOW(), NOW(), NOW());