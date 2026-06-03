package com.example.appointment.service;

import com.example.appointment.entity.User;
import com.example.appointment.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserManagementService {

    private final UserRepository userRepository;

    // 📌 جلب كل المستخدمين
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // 📌 جلب مستخدم واحد
    public User getUserById(String id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // 📌 تعديل مستخدم
    public User updateUser(String id, User updatedUser) {

        User user = getUserById(id);

        user.setUsername(updatedUser.getUsername());
        user.setEmail(updatedUser.getEmail());
        user.setRole(updatedUser.getRole());
        user.setPassword(updatedUser.getPassword());

        return userRepository.save(user);
    }

    // 📌 حذف مستخدم
    public void deleteUser(String id) {
        userRepository.deleteById(id);
    }
}