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

    public AuctionItems delistCopyright(Long id) {
        String sql = "UPDATE auction_items SET status = 'DELISTED', updated_time = ? WHERE id = ? AND status = 'LISTED'";
        int updated = jdbcTemplate.update(sql, LocalDateTime.now(), id);
        return updated > 0 ? getCopyright(id) : null;
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