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

-- 创建竞品拍卖表 - 确保表名一致：使用 auction_items
CREATE TABLE IF NOT EXISTS auction_items (
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

-- 创建出价历史表
CREATE TABLE IF NOT EXISTS bid_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    auction_id BIGINT NOT NULL,
    bidder_id BIGINT NOT NULL,
    bidder_address VARCHAR(42) NOT NULL,
    bid_amount DECIMAL(20,8) NOT NULL,
    transaction_hash VARCHAR(66),
    bid_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (auction_id) REFERENCES auction_items(id),
    FOREIGN KEY (bidder_id) REFERENCES users(id)
);

-- 创建交易历史表
CREATE TABLE IF NOT EXISTS transaction_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    auction_id BIGINT NOT NULL,
    seller_id BIGINT NOT NULL,
    buyer_id BIGINT NOT NULL,
    final_price DECIMAL(20,8) NOT NULL,
    transaction_hash VARCHAR(66),
    transaction_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (auction_id) REFERENCES auction_items(id),
    FOREIGN KEY (seller_id) REFERENCES users(id),
    FOREIGN KEY (buyer_id) REFERENCES users(id)
);

-- 添加索引 - 确保表名一致
CREATE INDEX IF NOT EXISTS idx_auction_items_status ON auction_items(status);
CREATE INDEX IF NOT EXISTS idx_auction_items_owner ON auction_items(owner_address);
CREATE INDEX IF NOT EXISTS idx_auction_items_user ON auction_items(user_id);

-- 删除旧表名的索引
DROP INDEX IF EXISTS idx_auctionItems_status ON auctionItems;
DROP INDEX IF EXISTS idx_auctionItems_owner ON auctionItems;
DROP INDEX IF EXISTS idx_auctionItems_user ON auctionItems;

-- 如果copyrights表存在，将数据从copyrights表迁移到auction_items表（如果有需要）
-- 注意：需要根据实际情况调整字段
CREATE PROCEDURE IF NOT EXISTS migrate_data()
BEGIN
    -- 检查auctionitems表是否存在（老表名不带下划线）
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'auction_db' AND table_name = 'auctionitems') THEN
        -- 如果存在，将数据迁移到新表，并设置外键
        INSERT IGNORE INTO auction_items 
        (id, title, description, img_url, category, status, owner_address, user_id, 
         start_price, current_price, reason, attachment_paths, created_time, updated_time, 
         auction_start_time, auction_end_time)
        SELECT 
         id, title, description, img_url, category, status, owner_address, user_id, 
         start_price, current_price, reason, attachment_paths, created_time, updated_time, 
         auction_start_time, auction_end_time
        FROM auctionitems;
        
        -- 删除旧表
        SET FOREIGN_KEY_CHECKS = 0;
        DROP TABLE IF EXISTS auctionitems;
        SET FOREIGN_KEY_CHECKS = 1;
    END IF;
END;

-- 执行迁移过程
CALL migrate_data();
-- 删除迁移过程
DROP PROCEDURE IF EXISTS migrate_data; 