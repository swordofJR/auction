package com.copyright.service;

import com.copyright.entity.User;
import com.copyright.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    public User login(String username, String password) {
        User user = userRepository.findByUsernameAndDeleted(username, 0);
        if (user != null && user.getPassword().equals(password)) {
            return user;
        }
        return null;
    }

    public boolean isUsernameExists(String username) {
        User user = userRepository.findByUsernameAndDeleted(username, 0);
        return user != null;
    }

    public User register(String username, String password, String email) {
        // 创建新用户对象
        User newUser = new User();
        newUser.setUsername(username);
        newUser.setPassword(password);
        newUser.setEmail(email);
        // 设置默认钱包地址
        newUser.setAddress("0x0000000000000000000000000000000000000000");
        // 设置删除标记为0（未删除）
        newUser.setDeleted(0);
        // 设置创建时间和更新时间
        LocalDateTime now = LocalDateTime.now();
        newUser.setCreatedTime(now);
        newUser.setUpdatedTime(now);

        // 保存新用户到数据库
        return userRepository.save(newUser);
    }

    // 获取所有未删除的用户
    public List<User> getAllActiveUsers() {
        return userRepository.findAllByDeleted(0);
    }

    // 获取所有用户，包括删除的和未删除的
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // 获取单个用户详情（包括已删除的用户）
    public User getUserById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    // 获取单个未删除用户
    public User getActiveUserById(Long id) {
        return userRepository.findByIdAndDeleted(id, 0);
    }

    // 管理员添加新用户
    public User addUser(String username, String password) {
        // 检查用户名是否已存在
        if (isUsernameExists(username)) {
            return null;
        }
        // 为管理员添加的用户生成默认邮箱
        String email = username + "@default.com";
        return register(username, password, email);
    }

    // 逻辑删除用户
    public boolean deleteUser(Long id) {
        User user = getUserById(id);
        if (user != null) {
            // 设置删除标记为1（已删除）
            user.setDeleted(1);
            user.setUpdatedTime(LocalDateTime.now());
            userRepository.save(user);
            return true;
        }
        return false;
    }

    // 恢复已删除的用户
    public boolean restoreUser(Long id) {
        User user = getUserById(id);
        if (user != null && user.getDeleted() == 1) {
            // 设置删除标记为0（未删除）
            user.setDeleted(0);
            user.setUpdatedTime(LocalDateTime.now());
            userRepository.save(user);
            return true;
        }
        return false;
    }
}