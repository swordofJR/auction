package com.copyright.controller;

import com.copyright.entity.AuctionItems;
import com.copyright.entity.User;
import com.copyright.service.CopyrightService;
import com.copyright.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private CopyrightService copyrightService;

    @Autowired
    private UserService userService;

    /**
     * 获取所有待审核版权
     */
    @GetMapping("/copyrights/pending")
    public ResponseEntity<List<AuctionItems>> getPendingCopyrights() {
        return ResponseEntity.ok(copyrightService.getPendingCopyrights());
    }

    /**
     * 审核版权
     */
    @PostMapping("/copyrights/{id}/review")
    public ResponseEntity<AuctionItems> reviewCopyright(
            @PathVariable Long id,
            @RequestParam String status,
            @RequestParam(required = false) String reason) {

        // 只接受 APPROVED 或 REJECTED 状态
        if (!status.equals("APPROVED") && !status.equals("REJECTED")) {
            return ResponseEntity.badRequest().build();
        }

        AuctionItems copyright = copyrightService.reviewCopyright(id, status, reason);
        if (copyright != null) {
            return ResponseEntity.ok(copyright);
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * 获取所有版权
     */
    @GetMapping("/copyrights")
    public ResponseEntity<List<AuctionItems>> getAllCopyrights() {
        return ResponseEntity.ok(copyrightService.getAllCopyrights());
    }

    /**
     * 获取所有未删除的用户
     */
    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        // 获取所有用户，包括已删除的
        return ResponseEntity.ok(userService.getAllUsers());
    }

    /**
     * 获取单个用户详情
     */
    @GetMapping("/users/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        User user = userService.getActiveUserById(id);
        if (user != null) {
            return ResponseEntity.ok(user);
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * 管理员添加新用户
     */
    @PostMapping("/users")
    public ResponseEntity<?> addUser(@RequestParam String username, @RequestParam String password,
            @RequestParam(required = false) String email) {
        try {
            // 验证用户名是否已存在
            if (userService.isUsernameExists(username)) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "用户名已存在");
                return ResponseEntity.badRequest().body(error);
            }

            User user;
            // 如果提供了邮箱，使用提供的邮箱，否则使用默认邮箱
            if (email != null && !email.isEmpty()) {
                user = userService.register(username, password, email);
            } else {
                user = userService.addUser(username, password);
            }

            if (user != null) {
                return ResponseEntity.ok(user);
            } else {
                Map<String, String> error = new HashMap<>();
                error.put("message", "添加用户失败");
                return ResponseEntity.badRequest().body(error);
            }
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "服务器错误: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 逻辑删除用户
     */
    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        boolean success = userService.deleteUser(id);
        if (success) {
            Map<String, String> response = new HashMap<>();
            response.put("message", "删除用户成功");
            return ResponseEntity.ok(response);
        }
        Map<String, String> error = new HashMap<>();
        error.put("message", "删除用户失败，用户不存在");
        return ResponseEntity.notFound().build();
    }

    /**
     * 恢复已删除的用户
     */
    @PostMapping("/users/{id}/restore")
    public ResponseEntity<?> restoreUser(@PathVariable Long id) {
        boolean success = userService.restoreUser(id);
        if (success) {
            Map<String, String> response = new HashMap<>();
            response.put("message", "恢复用户成功");
            return ResponseEntity.ok(response);
        }
        Map<String, String> error = new HashMap<>();
        error.put("message", "恢复用户失败，用户不存在或未被删除");
        return ResponseEntity.notFound().build();
    }
}