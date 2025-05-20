package com.copyright.controller;

import com.copyright.entity.AuctionItems;
import com.copyright.service.CopyrightJdbcService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/jdbc/copyright")
public class CopyrightJdbcController {

    @Autowired
    private CopyrightJdbcService copyrightJdbcService;

    /**
     * 上传竞品信息
     */
    @PostMapping("/upload")
    public ResponseEntity<AuctionItems> uploadCopyright(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam("description") String description,
            @RequestParam("category") String category,
            @RequestParam("ownerAddress") String ownerAddress,
            @RequestParam(value = "startPrice", required = false, defaultValue = "0.01") BigDecimal startPrice,
            @RequestParam(value = "auctionStartTime", required = false) String auctionStartTimeStr,
            @RequestParam(value = "auctionEndTime", required = false) String auctionEndTimeStr,
            @RequestParam(value = "attachments", required = false) MultipartFile[] attachments,
            @RequestParam(value = "userId", required = false) Long userId) {
        try {
            LocalDateTime auctionStartTime = null;
            LocalDateTime auctionEndTime = null;

            if (auctionStartTimeStr != null && !auctionStartTimeStr.isEmpty()) {
                auctionStartTime = LocalDateTime.parse(auctionStartTimeStr.replace("Z", ""));
            } else {
                auctionStartTime = LocalDateTime.now().plusDays(1); // 默认为明天
            }

            if (auctionEndTimeStr != null && !auctionEndTimeStr.isEmpty()) {
                auctionEndTime = LocalDateTime.parse(auctionEndTimeStr.replace("Z", ""));
            } else {
                auctionEndTime = LocalDateTime.now().plusDays(7); // 默认为一周后
            }

            AuctionItems auctionItem = copyrightJdbcService.uploadCopyright(
                    file,
                    title,
                    description,
                    category,
                    ownerAddress,
                    startPrice,
                    auctionStartTime,
                    auctionEndTime,
                    attachments,
                    userId);

            return ResponseEntity.ok(auctionItem);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 获取所有版权信息
     */
    @GetMapping("/all")
    public ResponseEntity<List<AuctionItems>> getAllCopyrights() {
        return ResponseEntity.ok(copyrightJdbcService.getAllCopyrights());
    }

    /**
     * 获取所有版权信息（包含用户名）
     */
    @GetMapping("/all-with-users")
    public ResponseEntity<List<Map<String, Object>>> getAllCopyrightsWithUsers() {
        return ResponseEntity.ok(copyrightJdbcService.getAllCopyrightsWithUsernames());
    }

    /**
     * 获取待审核的版权信息
     */
    @GetMapping("/pending")
    public ResponseEntity<List<AuctionItems>> getPendingCopyrights() {
        return ResponseEntity.ok(copyrightJdbcService.getPendingCopyrights());
    }

    /**
     * 获取用户的版权信息
     */
    @GetMapping("/user/{address}")
    public ResponseEntity<List<AuctionItems>> getUserCopyrights(@PathVariable String address) {
        return ResponseEntity.ok(copyrightJdbcService.getUserCopyrights(address));
    }

    /**
     * 获取用户的版权信息，包含用户名
     */
    @GetMapping("/user/{address}/with-username")
    public ResponseEntity<List<Map<String, Object>>> getUserCopyrightsWithUsername(@PathVariable String address) {
        return ResponseEntity.ok(copyrightJdbcService.getUserCopyrightsWithUsername(address));
    }

    /**
     * 获取用户的版权信息 (通过用户ID)
     */
    @GetMapping("/user-id/{userId}")
    public ResponseEntity<List<AuctionItems>> getUserCopyrightsByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(copyrightJdbcService.getUserCopyrightsByUserId(userId));
    }

    /**
     * 获取用户的版权信息 (通过用户ID)，包含用户名
     */
    @GetMapping("/user-id/{userId}/with-username")
    public ResponseEntity<List<Map<String, Object>>> getUserCopyrightsByUserIdWithUsername(@PathVariable Long userId) {
        return ResponseEntity.ok(copyrightJdbcService.getUserCopyrightsByUserIdWithUsername(userId));
    }

    /**
     * 获取市场上架的版权信息
     */
    @GetMapping("/marketplace")
    public ResponseEntity<List<AuctionItems>> getMarketplaceCopyrights() {
        return ResponseEntity.ok(copyrightJdbcService.getMarketplaceCopyrights());
    }

    /**
     * 获取市场上架的版权信息（包含用户名）
     */
    @GetMapping("/marketplace-with-usernames")
    public ResponseEntity<List<Map<String, Object>>> getMarketplaceCopyrightsWithUsernames() {
        return ResponseEntity.ok(copyrightJdbcService.getMarketplaceCopyrightsWithUsernames());
    }

    /**
     * 审核版权
     */
    @PostMapping("/{id}/review")
    public ResponseEntity<AuctionItems> reviewCopyright(
            @PathVariable Long id,
            @RequestParam String status,
            @RequestParam(required = false) String reason) {
        AuctionItems copyright = copyrightJdbcService.reviewCopyright(id, status, reason);
        if (copyright != null) {
            return ResponseEntity.ok(copyright);
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * 上架版权
     */
    @PostMapping("/{id}/list")
    public ResponseEntity<AuctionItems> listCopyright(
            @PathVariable Long id,
            @RequestParam BigDecimal price) {
        AuctionItems copyright = copyrightJdbcService.listCopyright(id, price);
        if (copyright != null) {
            return ResponseEntity.ok(copyright);
        }
        return ResponseEntity.badRequest().build();
    }

    /**
     * 购买版权
     */
    @PostMapping("/{id}/purchase")
    public ResponseEntity<AuctionItems> purchaseCopyright(
            @PathVariable Long id,
            @RequestParam String newOwnerAddress,
            @RequestParam(required = false) Long newUserId) {
        AuctionItems copyright = copyrightJdbcService.purchaseCopyright(id, newOwnerAddress, newUserId);
        if (copyright != null) {
            return ResponseEntity.ok(copyright);
        }
        return ResponseEntity.badRequest().build();
    }

    /**
     * 下架版权
     */
    @PostMapping("/{id}/delist")
    public ResponseEntity<?> delistCopyright(@PathVariable Long id,
            @RequestParam(required = false) String newOwnerAddress,
            @RequestParam(required = false) BigDecimal finalPrice,
            @RequestParam(required = false) String transactionHash, @RequestParam(required = false) Long newOwnerId,
            @RequestParam(defaultValue = "false") boolean forceEnd) {
        try {
            AuctionItems copyright = copyrightJdbcService.delistCopyright(id, newOwnerAddress, finalPrice,
                    transactionHash, newOwnerId, forceEnd);
            if (copyright != null) {
                return ResponseEntity.ok(copyright);
            }
            return ResponseEntity.badRequest().body(null);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    /**
     * 下载附件
     */
    @GetMapping("/download/attachment/{fileName}")
    public ResponseEntity<byte[]> downloadAttachment(@PathVariable String fileName) {
        try {
            // 获取文件路径
            String uploadDir = System.getProperty("user.dir") + "/uploads";
            Path filePath = Paths.get(uploadDir, fileName);

            // 检查文件是否存在
            if (!Files.exists(filePath)) {
                return ResponseEntity.notFound().build();
            }

            // 读取文件内容
            byte[] fileContent = Files.readAllBytes(filePath);

            // 获取文件MIME类型
            String mimeType = Files.probeContentType(filePath);
            if (mimeType == null) {
                mimeType = "application/octet-stream";
            }

            // 构建响应
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(mimeType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .body(fileContent);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 出价
     */
    @PostMapping("/{id}/bid")
    public ResponseEntity<?> placeBid(
            @PathVariable Long id,
            @RequestParam Long userId,
            @RequestParam BigDecimal bidAmount,
            @RequestParam String bidderAddress,
            @RequestParam(required = false) String transactionHash) {
        try {
            AuctionItems auctionItem = copyrightJdbcService.placeBid(id, userId, bidAmount, bidderAddress,
                    transactionHash);
            if (auctionItem != null) {
                return ResponseEntity.ok(auctionItem);
            }
            return ResponseEntity.badRequest().body("出价失败：价格必须高于当前价格");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * 获取出价历史
     */
    @GetMapping("/{id}/bid-history")
    public ResponseEntity<?> getBidHistory(@PathVariable Long id) {
        try {
            List<Map<String, Object>> bidHistory = copyrightJdbcService.getBidHistory(id);
            return ResponseEntity.ok(bidHistory);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * 获取交易历史
     */
    @GetMapping("/{id}/transaction-history")
    public ResponseEntity<?> getTransactionHistory(@PathVariable Long id) {
        try {
            List<Map<String, Object>> transactionHistory = copyrightJdbcService.getTransactionHistory(id);
            return ResponseEntity.ok(transactionHistory);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * 获取用户交易历史
     */
    @GetMapping("/user/{userId}/transaction-history")
    public ResponseEntity<?> getUserTransactionHistory(@PathVariable Long userId) {
        try {
            List<Map<String, Object>> transactionHistory = copyrightJdbcService.getUserTransactionHistory(userId);
            return ResponseEntity.ok(transactionHistory);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * 根据关键词搜索竞品
     */
    @GetMapping("/search")
    public ResponseEntity<List<AuctionItems>> searchByKeyword(@RequestParam String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(copyrightJdbcService.searchByKeyword(keyword));
    }

    /**
     * 获取版权详情
     * 注意：这个端点必须放在最后，避免与其他路径冲突
     */
    @GetMapping("/{id}")
    public ResponseEntity<AuctionItems> getCopyright(@PathVariable Long id) {
        AuctionItems copyright = copyrightJdbcService.getCopyright(id);
        if (copyright != null) {
            return ResponseEntity.ok(copyright);
        }
        return ResponseEntity.notFound().build();
    }

}