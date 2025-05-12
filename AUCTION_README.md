# 竞品拍卖系统设置指南

## 数据库错误修复

当您遇到 `java.sql.SQLSyntaxErrorException: Table 'auction_db.copyrights' doesn't exist` 错误时，这表明系统正在尝试访问一个不存在的数据库表。这是因为系统已从版权保护系统升级为竞品拍卖系统，但数据库结构尚未更新。

### 解决方案

1. 在您的数据库中执行以下SQL脚本来创建所需的表结构：

```sql
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
CREATE INDEX idx_auctionItems_status ON auctionItems(status);
CREATE INDEX idx_auctionItems_owner ON auctionItems(owner_address);
CREATE INDEX idx_auctionItems_user ON auctionItems(user_id);
```

2. 如果您有旧的版权数据需要迁移，可以使用以下SQL脚本：

```sql
-- 数据迁移脚本（如果有需要）
INSERT INTO auctionItems (
  title, description, img_url, category, status, 
  owner_address, user_id, current_price, start_price,
  created_time, updated_time, auction_start_time, auction_end_time
)
SELECT 
  title, description, img_url, category, status,
  owner_address, user_id, price, price,
  created_time, updated_time, 
  DATE_ADD(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 7 DAY)
FROM copyrights;
```

3. 此外，在项目的backend/src/main/resources/schema.sql文件中已经添加了自动创建表结构和迁移数据的脚本。如果您的项目配置为启动时执行此脚本，则只需重启应用程序即可。

## 竞品拍卖功能使用说明

### 1. 上传竞品

1. 登录系统后，点击导航栏中的"上传竞品"链接
2. 填写竞品信息：
   - 标题：竞品的名称
   - 描述：详细描述竞品的特点和价值
   - 照片：上传竞品的图片（支持JPG、JPEG、PNG格式，最大2MB）
   - 类别：选择竞品所属类别
   - 起拍价：设置竞品的起拍价（ETH）
   - 拍卖开始时间：设置拍卖开始的日期和时间
   - 拍卖结束时间：设置拍卖结束的日期和时间
   - 附带文件：（可选）上传与竞品相关的附件文件（最大10MB）

3. 点击"提交竞品信息"按钮完成上传

### 2. 查看我的竞品

1. 登录后，点击导航栏中的"我的竞品"链接
2. 系统将显示您上传的所有竞品及其状态
3. 对于已审核通过的竞品，可以点击"发布拍卖"按钮将其发布到拍卖市场

### 3. 参与拍卖

1. 在"拍卖市场"页面，浏览所有可拍卖的竞品
2. 点击感兴趣的竞品查看详情
3. 输入您的出价金额（必须大于或等于起拍价）
4. 确认出价并完成支付

### 4. 竞品状态说明

- **待审核(PENDING)**：您的竞品已上传，正在等待管理员审核
- **已审核(APPROVED)**：竞品已通过审核，可以发布到拍卖市场
- **已拒绝(REJECTED)**：竞品未通过审核，请查看拒绝原因
- **拍卖中(LISTED)**：竞品已在拍卖市场公开展示
- **已售出(SOLD)**：竞品已被拍卖成功
- **已下架(DELISTED)**：竞品已从拍卖市场下架

## 技术支持

如有任何问题或需要技术支持，请联系系统管理员。 