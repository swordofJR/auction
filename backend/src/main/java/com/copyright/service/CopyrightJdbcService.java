package com.copyright.service;

import com.copyright.entity.AuctionItems;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class CopyrightJdbcService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final String uploadDir = System.getProperty("user.dir") + "/uploads";

    public CopyrightJdbcService() {
        // 创建上传目录
        File dir = new File(uploadDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    public AuctionItems uploadCopyright(MultipartFile file, String title, String description, String category,
            String ownerAddress, BigDecimal startPrice, LocalDateTime auctionStartTime, LocalDateTime auctionEndTime,
            MultipartFile[] attachments, Long userId) throws IOException {
        // 保存主图片
        String uniqueFileName = saveFile(file);

        // 保存附件（如果有）
        String attachmentPaths = "";
        if (attachments != null && attachments.length > 0) {
            StringBuilder attachmentPathsBuilder = new StringBuilder();
            for (MultipartFile attachment : attachments) {
                if (attachment != null && !attachment.isEmpty()) {
                    String attachmentPath = saveFile(attachment);
                    attachmentPathsBuilder.append(attachmentPath).append(",");
                }
            }
            if (attachmentPathsBuilder.length() > 0) {
                attachmentPaths = attachmentPathsBuilder.substring(0, attachmentPathsBuilder.length() - 1);
            }
        }

        // 当前时间
        LocalDateTime now = LocalDateTime.now();

        // 插入数据库 - 更新SQL以包含所有新字段
        String sql = "INSERT INTO auction_items (title, description, img_url, category, status, owner_address, user_id, "
                +
                "created_time, updated_time, start_price, auction_start_time, auction_end_time, attachment_paths) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        jdbcTemplate.update(sql,
                title,
                description,
                uniqueFileName,
                category,
                "PENDING",
                ownerAddress,
                userId,
                now,
                now,
                startPrice,
                auctionStartTime,
                auctionEndTime,
                attachmentPaths);

        // 获取刚刚插入的记录的ID
        Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);

        // 创建竞品对象
        AuctionItems auctionItems = new AuctionItems();
        auctionItems.setId(id);
        auctionItems.setTitle(title);
        auctionItems.setDescription(description);
        auctionItems.setImgUrl(uniqueFileName);
        auctionItems.setCategory(category);
        auctionItems.setStatus("PENDING");
        auctionItems.setOwnerAddress(ownerAddress);
        auctionItems.setUserId(userId);
        auctionItems.setCreatedTime(now);
        auctionItems.setUpdatedTime(now);
        auctionItems.setStartPrice(startPrice);
        auctionItems.setAuctionStartTime(auctionStartTime);
        auctionItems.setAuctionEndTime(auctionEndTime);
        auctionItems.setAttachmentPaths(attachmentPaths);

        return auctionItems;
    }

    private String saveFile(MultipartFile file) throws IOException {
        // 生成唯一文件名
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String uniqueFileName = UUID.randomUUID().toString() + extension;

        // 保存文件
        Path filePath = Paths.get(uploadDir, uniqueFileName);
        Files.write(filePath, file.getBytes());

        return uniqueFileName;
    }

    public List<AuctionItems> getAllCopyrights() {
        String sql = "SELECT * FROM auction_items";
        return jdbcTemplate.query(sql, new CopyrightRowMapper());
    }

    public List<Map<String, Object>> getAllCopyrightsWithUsernames() {
        String sql = "SELECT c.*, u.username FROM auction_items c LEFT JOIN users u ON c.user_id = u.id";
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Map<String, Object> result = new HashMap<>();
            AuctionItems auctionItems = new CopyrightRowMapper().mapRow(rs, rowNum);

            // 将AuctionItems对象的所有属性复制到Map中
            result.put("id", auctionItems.getId());
            result.put("title", auctionItems.getTitle());
            result.put("description", auctionItems.getDescription());
            result.put("imgUrl", auctionItems.getImgUrl());
            result.put("category", auctionItems.getCategory());
            result.put("status", auctionItems.getStatus());
            result.put("ownerAddress", auctionItems.getOwnerAddress());
            result.put("userId", auctionItems.getUserId());
            result.put("price", auctionItems.getCurrentPrice());
            result.put("startPrice", auctionItems.getStartPrice());
            result.put("reason", auctionItems.getReason());
            result.put("createdTime", auctionItems.getCreatedTime());
            result.put("updatedTime", auctionItems.getUpdatedTime());
            result.put("auctionStartTime", auctionItems.getAuctionStartTime());
            result.put("auctionEndTime", auctionItems.getAuctionEndTime());
            result.put("attachmentPaths", auctionItems.getAttachmentPaths());
            // 添加用户名
            result.put("username", rs.getString("username"));

            return result;
        });
    }

    public List<AuctionItems> getPendingCopyrights() {
        String sql = "SELECT * FROM auction_items WHERE status = 'PENDING'";
        return jdbcTemplate.query(sql, new CopyrightRowMapper());
    }

    public List<AuctionItems> getUserCopyrights(String ownerAddress) {
        String sql = "SELECT * FROM auction_items WHERE owner_address = ?";
        return jdbcTemplate.query(sql, new CopyrightRowMapper(), ownerAddress);
    }

    public List<Map<String, Object>> getUserCopyrightsWithUsername(String ownerAddress) {
        String sql = "SELECT c.*, u.username FROM auction_items c LEFT JOIN users u ON c.user_id = u.id WHERE c.owner_address = ?";
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Map<String, Object> result = new HashMap<>();
            AuctionItems auctionItems = new CopyrightRowMapper().mapRow(rs, rowNum);

            // 将AuctionItems对象的所有属性复制到Map中
            result.put("id", auctionItems.getId());
            result.put("title", auctionItems.getTitle());
            result.put("description", auctionItems.getDescription());
            result.put("imgUrl", auctionItems.getImgUrl());
            result.put("category", auctionItems.getCategory());
            result.put("status", auctionItems.getStatus());
            result.put("ownerAddress", auctionItems.getOwnerAddress());
            result.put("userId", auctionItems.getUserId());
            result.put("price", auctionItems.getCurrentPrice());
            result.put("startPrice", auctionItems.getStartPrice());
            result.put("reason", auctionItems.getReason());
            result.put("createdTime", auctionItems.getCreatedTime());
            result.put("updatedTime", auctionItems.getUpdatedTime());
            result.put("auctionStartTime", auctionItems.getAuctionStartTime());
            result.put("auctionEndTime", auctionItems.getAuctionEndTime());
            result.put("attachmentPaths", auctionItems.getAttachmentPaths());
            // 添加用户名
            result.put("username", rs.getString("username"));

            return result;
        }, ownerAddress);
    }

    public List<AuctionItems> getUserCopyrightsByUserId(Long userId) {
        String sql = "SELECT * FROM auction_items WHERE user_id = ?";
        return jdbcTemplate.query(sql, new CopyrightRowMapper(), userId);
    }

    public List<Map<String, Object>> getUserCopyrightsByUserIdWithUsername(Long userId) {
        String sql = "SELECT c.*, u.username FROM auction_items c LEFT JOIN users u ON c.user_id = u.id WHERE c.user_id = ?";
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Map<String, Object> result = new HashMap<>();
            AuctionItems auctionItems = new CopyrightRowMapper().mapRow(rs, rowNum);

            // 将AuctionItems对象的所有属性复制到Map中
            result.put("id", auctionItems.getId());
            result.put("title", auctionItems.getTitle());
            result.put("description", auctionItems.getDescription());
            result.put("imgUrl", auctionItems.getImgUrl());
            result.put("category", auctionItems.getCategory());
            result.put("status", auctionItems.getStatus());
            result.put("ownerAddress", auctionItems.getOwnerAddress());
            result.put("userId", auctionItems.getUserId());
            result.put("price", auctionItems.getCurrentPrice());
            result.put("startPrice", auctionItems.getStartPrice());
            result.put("reason", auctionItems.getReason());
            result.put("createdTime", auctionItems.getCreatedTime());
            result.put("updatedTime", auctionItems.getUpdatedTime());
            result.put("auctionStartTime", auctionItems.getAuctionStartTime());
            result.put("auctionEndTime", auctionItems.getAuctionEndTime());
            result.put("attachmentPaths", auctionItems.getAttachmentPaths());
            // 添加用户名
            result.put("username", rs.getString("username"));

            return result;
        }, userId);
    }

    public List<AuctionItems> getMarketplaceCopyrights() {
        String sql = "SELECT * FROM auction_items WHERE status = 'LISTED'";
        return jdbcTemplate.query(sql, new CopyrightRowMapper());
    }

    public List<Map<String, Object>> getMarketplaceCopyrightsWithUsernames() {
        String sql = "SELECT c.*, u.username FROM auction_items c LEFT JOIN users u ON c.user_id = u.id WHERE c.status = 'LISTED'";
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Map<String, Object> result = new HashMap<>();
            AuctionItems auctionItems = new CopyrightRowMapper().mapRow(rs, rowNum);

            // 将AuctionItems对象的所有属性复制到Map中
            result.put("id", auctionItems.getId());
            result.put("title", auctionItems.getTitle());
            result.put("description", auctionItems.getDescription());
            result.put("imgUrl", auctionItems.getImgUrl());
            result.put("category", auctionItems.getCategory());
            result.put("status", auctionItems.getStatus());
            result.put("ownerAddress", auctionItems.getOwnerAddress());
            result.put("userId", auctionItems.getUserId());
            result.put("price", auctionItems.getCurrentPrice());
            result.put("startPrice", auctionItems.getStartPrice());
            result.put("reason", auctionItems.getReason());
            result.put("createdTime", auctionItems.getCreatedTime());
            result.put("updatedTime", auctionItems.getUpdatedTime());
            result.put("auctionStartTime", auctionItems.getAuctionStartTime());
            result.put("auctionEndTime", auctionItems.getAuctionEndTime());
            result.put("attachmentPaths", auctionItems.getAttachmentPaths());
            // 添加用户名
            result.put("username", rs.getString("username"));

            return result;
        });
    }

    public AuctionItems getCopyright(Long id) {
        String sql = "SELECT * FROM auction_items WHERE id = ?";
        List<AuctionItems> auctionItems = jdbcTemplate.query(sql, new CopyrightRowMapper(), id);
        return auctionItems.isEmpty() ? null : auctionItems.get(0);
    }

    public AuctionItems reviewCopyright(Long id, String status, String reason) {
        String sql = "UPDATE auction_items SET status = ?, reason = ?, updated_time = ? WHERE id = ?";
        jdbcTemplate.update(sql, status, reason, LocalDateTime.now(), id);
        return getCopyright(id);
    }

    public AuctionItems listCopyright(Long id, BigDecimal price) {
        String sql = "UPDATE auction_items SET status = 'LISTED', current_price = ?, updated_time = ? WHERE id = ? AND status = 'APPROVED'";
        int updated = jdbcTemplate.update(sql, price, LocalDateTime.now(), id);
        return updated > 0 ? getCopyright(id) : null;
    }

    public AuctionItems purchaseCopyright(Long id, String newOwnerAddress, Long newUserId) {
        String sql = "UPDATE auction_items SET status = 'SOLD', owner_address = ?, user_id = ?, updated_time = ? WHERE id = ? AND status = 'LISTED'";
        int updated = jdbcTemplate.update(sql, newOwnerAddress, newUserId, LocalDateTime.now(), id);
        return updated > 0 ? getCopyright(id) : null;
    }

    public AuctionItems delistCopyright(Long id, String newOwnerAddress, BigDecimal finalPrice, String transactionHash,
            Long newOwnerId, boolean forceEnd) {
        // 获取拍卖物品信息
        AuctionItems auctionItem = getCopyright(id);
        if (auctionItem == null) {
            throw new RuntimeException("拍卖物品不存在");
        }

        // 检查拍卖是否可以结束
        LocalDateTime now = LocalDateTime.now();
        boolean isExpired = now.isAfter(auctionItem.getAuctionEndTime());

        // 如果不是强制结束且拍卖未到期，则不允许结束拍卖
        if (!forceEnd && !isExpired) {
            throw new RuntimeException("拍卖未到期，无法结束");
        }

        // 如果是拍卖到期自然结束，需要查询最高出价信息
        if (newOwnerAddress == null || "unknown".equals(newOwnerAddress)) {
            String bidSql = "SELECT bidder_id, bidder_address, bid_amount FROM bid_history " +
                    "WHERE auction_id = ? ORDER BY bid_amount DESC LIMIT 1";
            List<Map<String, Object>> bids = jdbcTemplate.query(bidSql, (rs, rowNum) -> {
                Map<String, Object> bid = new HashMap<>();
                bid.put("bidderId", rs.getLong("bidder_id"));
                bid.put("bidderAddress", rs.getString("bidder_address"));
                bid.put("bidAmount", rs.getBigDecimal("bid_amount"));
                return bid;
            }, id);

            // 如果有人出价，则更新所有者为最高出价者
            if (!bids.isEmpty()) {
                Map<String, Object> highestBid = bids.get(0);
                newOwnerAddress = (String) highestBid.get("bidderAddress");
                finalPrice = (BigDecimal) highestBid.get("bidAmount");
                newOwnerId = (Long) highestBid.get("bidderId");
            } else {
                // 如果没有人出价，则不改变所有权
                newOwnerAddress = auctionItem.getOwnerAddress();
                newOwnerId = auctionItem.getUserId();
            }
        }

        // 记录交易历史
        if (finalPrice != null && !auctionItem.getOwnerAddress().equals(newOwnerAddress)) {
            String historySQL = "INSERT INTO transaction_history (auction_id, seller_id, buyer_id, final_price, transaction_hash, transaction_time) VALUES (?, ?, ?, ?, ?, ?)";
            jdbcTemplate.update(historySQL, id, auctionItem.getUserId(), newOwnerId, finalPrice, transactionHash, now);
        }

        // 更新拍卖状态和所有者信息
        String sql = "UPDATE auction_items SET status = ?, owner_address = ?, user_id = ?, current_price = ?, updated_time = ? WHERE id = ? AND status = 'LISTED'";
        int updated = jdbcTemplate.update(sql, "SOLD", newOwnerAddress, newOwnerId, finalPrice, now, id);

        return updated > 0 ? getCopyright(id) : null;
    }

    public AuctionItems placeBid(Long id, Long userId, BigDecimal bidAmount, String bidderAddress,
            String transactionHash) {
        // 获取拍卖物品信息
        AuctionItems auctionItem = getCopyright(id);
        if (auctionItem == null) {
            throw new RuntimeException("拍卖物品不存在");
        }

        // 检查拍卖状态
        if (!"LISTED".equals(auctionItem.getStatus())) {
            throw new RuntimeException("拍卖已结束或未开始");
        }

        // 检查拍卖时间
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(auctionItem.getAuctionStartTime())) {
            throw new RuntimeException("拍卖尚未开始");
        }
        if (now.isAfter(auctionItem.getAuctionEndTime())) {
            throw new RuntimeException("拍卖已结束");
        }

        // 检查出价是否高于当前价格
        if (auctionItem.getCurrentPrice() != null && bidAmount.compareTo(auctionItem.getCurrentPrice()) <= 0) {
            return null;
        }

        // 更新拍卖物品价格
        String updateSql = "UPDATE auction_items SET current_price = ?, updated_time = ? WHERE id = ? AND status = 'LISTED'";
        int updated = jdbcTemplate.update(updateSql, bidAmount, now, id);
        if (updated == 0) {
            throw new RuntimeException("更新拍卖价格失败");
        }

        // 记录出价历史
        String insertBidSql = "INSERT INTO bid_history (auction_id, bidder_id, bidder_address, bid_amount, transaction_hash) VALUES (?, ?, ?, ?, ?)";
        jdbcTemplate.update(insertBidSql, id, userId, bidderAddress, bidAmount, transactionHash);

        return getCopyright(id);
    }

    public List<Map<String, Object>> getBidHistory(Long auctionId) {
        String sql = "SELECT bh.*, u.username as bidder_name FROM bid_history bh " +
                "LEFT JOIN users u ON bh.bidder_id = u.id " +
                "WHERE bh.auction_id = ? " +
                "ORDER BY bh.bid_time DESC";

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Map<String, Object> result = new HashMap<>();
            result.put("id", rs.getLong("id"));
            result.put("auctionId", rs.getLong("auction_id"));
            result.put("bidderId", rs.getLong("bidder_id"));
            result.put("bidderAddress", rs.getString("bidder_address"));
            result.put("bidAmount", rs.getBigDecimal("bid_amount"));
            result.put("transactionHash", rs.getString("transaction_hash"));
            result.put("bidTime", rs.getTimestamp("bid_time").toLocalDateTime());
            result.put("bidderName", rs.getString("bidder_name"));
            return result;
        }, auctionId);
    }

    private static class CopyrightRowMapper implements RowMapper<AuctionItems> {
        @Override
        public AuctionItems mapRow(ResultSet rs, int rowNum) throws SQLException {
            AuctionItems auctionItems = new AuctionItems();
            auctionItems.setId(rs.getLong("id"));
            auctionItems.setTitle(rs.getString("title"));
            auctionItems.setDescription(rs.getString("description"));
            auctionItems.setImgUrl(rs.getString("img_url"));
            auctionItems.setCategory(rs.getString("category"));
            auctionItems.setStatus(rs.getString("status"));
            auctionItems.setOwnerAddress(rs.getString("owner_address"));
            auctionItems.setUserId(rs.getLong("user_id"));
            auctionItems.setCreatedTime(rs.getTimestamp("created_time").toLocalDateTime());
            auctionItems.setUpdatedTime(rs.getTimestamp("updated_time").toLocalDateTime());

            // 添加新字段
            try {
                auctionItems.setStartPrice(rs.getBigDecimal("start_price"));
            } catch (SQLException e) {
                auctionItems.setStartPrice(BigDecimal.ZERO);
            }

            try {
                auctionItems.setCurrentPrice(rs.getBigDecimal("current_price"));
            } catch (SQLException e) {
                auctionItems.setCurrentPrice(BigDecimal.ZERO);
            }

            try {
                java.sql.Timestamp startTime = rs.getTimestamp("auction_start_time");
                if (startTime != null) {
                    auctionItems.setAuctionStartTime(startTime.toLocalDateTime());
                }
            } catch (SQLException e) {
                // 忽略错误，保持默认值
            }

            try {
                java.sql.Timestamp endTime = rs.getTimestamp("auction_end_time");
                if (endTime != null) {
                    auctionItems.setAuctionEndTime(endTime.toLocalDateTime());
                }
            } catch (SQLException e) {
                // 忽略错误，保持默认值
            }

            try {
                auctionItems.setAttachmentPaths(rs.getString("attachment_paths"));
            } catch (SQLException e) {
                // 忽略错误，使用默认值null
            }

            try {
                auctionItems.setReason(rs.getString("reason"));
            } catch (SQLException e) {
                // 忽略错误，使用默认值null
            }

            return auctionItems;
        }
    }
}