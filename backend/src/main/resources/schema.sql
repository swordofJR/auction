-- 创建数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS auction_db;

-- 使用数据库
USE auction_db;

-- 创建用户表（如果不存在）
CREATE TABLE IF NOT EXISTS users (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(255) NOT NULL UNIQUE,
  password VARCHAR(255) NOT NULL,
  address VARCHAR(255) NOT NULL,
  email VARCHAR(255) NOT NULL,
  deleted INT NOT NULL DEFAULT 0,
  created_time DATETIME NOT NULL,
  updated_time DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 创建竞品拍卖表
CREATE TABLE IF NOT EXISTS auctionItems (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  title VARCHAR(255) NOT NULL,
  description TEXT NOT NULL,
  img_url VARCHAR(255) NOT NULL,
  category VARCHAR(50) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  owner_address VARCHAR(255) NOT NULL,
  user_id BIGINT,
  start_price DECIMAL(20, 8) DEFAULT 0.01,
  current_price DECIMAL(20, 8),
  reason VARCHAR(255),
  attachment_paths TEXT,
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  auction_start_time TIMESTAMP,
  auction_end_time TIMESTAMP
);

-- 添加索引
CREATE INDEX IF NOT EXISTS idx_auctionItems_status ON auctionItems(status);
CREATE INDEX IF NOT EXISTS idx_auctionItems_owner ON auctionItems(owner_address);
CREATE INDEX IF NOT EXISTS idx_auctionItems_user ON auctionItems(user_id);

-- 如果copyrights表存在，将数据从copyrights表迁移到auctionItems表（如果有需要）
-- 注意：需要根据实际情况调整字段
CREATE PROCEDURE IF NOT EXISTS migrate_data()
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE v_id BIGINT;
    DECLARE v_title VARCHAR(255); 
    DECLARE v_description TEXT;
    DECLARE v_img_url VARCHAR(255);
    DECLARE v_category VARCHAR(50);
    DECLARE v_status VARCHAR(20);
    DECLARE v_owner_address VARCHAR(255);
    DECLARE v_user_id BIGINT;
    DECLARE v_price DECIMAL(20, 8);
    DECLARE v_reason VARCHAR(255);
    DECLARE v_created_time TIMESTAMP;
    DECLARE v_updated_time TIMESTAMP;
    
    -- 检查copyrights表是否存在
    DECLARE copyrights_exists INT DEFAULT 0;
    SELECT COUNT(*) INTO copyrights_exists FROM information_schema.tables 
    WHERE table_schema = DATABASE() AND table_name = 'copyrights';
    
    -- 如果copyrights表存在且auctionItems表为空，则迁移数据
    IF copyrights_exists > 0 AND (SELECT COUNT(*) FROM auctionItems) = 0 THEN
        DECLARE cur CURSOR FOR 
            SELECT id, title, description, img_url, category, status, owner_address, user_id, 
                   price, reason, created_time, updated_time 
            FROM copyrights;
        DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
        
        OPEN cur;
        
        read_loop: LOOP
            FETCH cur INTO v_id, v_title, v_description, v_img_url, v_category, v_status, 
                           v_owner_address, v_user_id, v_price, v_reason, v_created_time, v_updated_time;
            IF done THEN
                LEAVE read_loop;
            END IF;
            
            -- 插入到新表，设置拍卖开始和结束时间为当前时间加1天和7天
            INSERT INTO auctionItems (id, title, description, img_url, category, status, 
                                     owner_address, user_id, current_price, start_price, reason, 
                                     created_time, updated_time, auction_start_time, auction_end_time)
            VALUES (v_id, v_title, v_description, v_img_url, v_category, v_status, 
                   v_owner_address, v_user_id, v_price, v_price, v_reason, 
                   v_created_time, v_updated_time, 
                   DATE_ADD(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 7 DAY));
        END LOOP;
        
        CLOSE cur;
    END IF;
END;

-- 调用存储过程
CALL migrate_data();

-- 删除存储过程（避免重复执行）
DROP PROCEDURE IF EXISTS migrate_data; 