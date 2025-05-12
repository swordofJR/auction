package com.copyright.entity;

import lombok.Data;
import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "auction_items")
public class AuctionItems {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private String imgUrl;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private String status; // PENDING, APPROVED, REJECTED, LISTED, SOLD

    @Column(nullable = false)
    private String ownerAddress;

    @Column
    private Long userId;

    @Column
    private BigDecimal startPrice;

    @Column
    private BigDecimal currentPrice;

    @Column
    private String reason; // 审核拒绝原因

    @Column
    private String attachmentPaths; // 附件文件路径，多个路径用逗号分隔

    @Column(nullable = false)
    private LocalDateTime createdTime;

    @Column(nullable = false)
    private LocalDateTime updatedTime;

    @Column(nullable = false)
    private LocalDateTime auctionStartTime;

    @Column(nullable = false)
    private LocalDateTime auctionEndTime;

    @PrePersist
    protected void onCreate() {
        createdTime = LocalDateTime.now();
        updatedTime = LocalDateTime.now();
        if (auctionStartTime == null) {
            auctionStartTime = LocalDateTime.now().plusDays(1);
        }
        if (auctionEndTime == null) {
            auctionEndTime = LocalDateTime.now().plusDays(7);
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedTime = LocalDateTime.now();
    }
}