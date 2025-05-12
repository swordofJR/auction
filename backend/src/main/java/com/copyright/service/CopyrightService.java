package com.copyright.service;

import com.copyright.entity.AuctionItems;
import com.copyright.repository.CopyrightRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class CopyrightService {

    @Autowired
    private CopyrightRepository copyrightRepository;

    private final String uploadDir = System.getProperty("user.dir") + "/uploads";

    public CopyrightService() {
        // 创建上传目录
        File dir = new File(uploadDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    public AuctionItems uploadCopyright(MultipartFile file, String title, String description, String category,
            String ownerAddress, Long userId) throws IOException {
        // 保存文件
        String uniqueFileName = saveFile(file);

        // 创建版权对象
        AuctionItems copyright = new AuctionItems();
        copyright.setTitle(title);
        copyright.setDescription(description);
        copyright.setImgUrl(uniqueFileName);
        copyright.setCategory(category);
        copyright.setStatus("PENDING"); // 默认状态为待审核
        copyright.setOwnerAddress(ownerAddress);
        copyright.setUserId(userId);

        // 保存到数据库
        return copyrightRepository.save(copyright);
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
        return copyrightRepository.findAll();
    }

    public List<AuctionItems> getPendingCopyrights() {
        return copyrightRepository.findByStatus("PENDING");
    }

    public List<AuctionItems> getUserCopyrights(String ownerAddress) {
        return copyrightRepository.findByOwnerAddress(ownerAddress);
    }

    public List<AuctionItems> getMarketplaceCopyrights() {
        return copyrightRepository.findByStatus("LISTED");
    }

    public AuctionItems getCopyright(Long id) {
        return copyrightRepository.findById(id).orElse(null);
    }

    public AuctionItems reviewCopyright(Long id, String status, String reason) {
        AuctionItems copyright = getCopyright(id);
        if (copyright != null) {
            copyright.setStatus(status);
            copyright.setReason(reason);
            return copyrightRepository.save(copyright);
        }
        return null;
    }

    public AuctionItems listCopyright(Long id, BigDecimal price) {
        AuctionItems copyright = getCopyright(id);
        if (copyright != null && "APPROVED".equals(copyright.getStatus())) {
            copyright.setStatus("LISTED");
            copyright.setCurrentPrice(price);
            return copyrightRepository.save(copyright);
        }
        return null;
    }

    public AuctionItems purchaseCopyright(Long id, String newOwnerAddress, Long newUserId) {
        AuctionItems copyright = getCopyright(id);
        if (copyright != null && "LISTED".equals(copyright.getStatus())) {
            copyright.setStatus("SOLD");
            copyright.setOwnerAddress(newOwnerAddress);
            copyright.setUserId(newUserId);
            return copyrightRepository.save(copyright);
        }
        return null;
    }
}