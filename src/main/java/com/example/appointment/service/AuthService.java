package com.example.appointment.service;

import com.example.appointment.dto.AuthResponse;
import com.example.appointment.dto.LoginRequest;
import com.example.appointment.dto.RegisterRequest;
import com.example.appointment.entity.Role;
import com.example.appointment.entity.User;
import com.example.appointment.repository.UserRepository;
import com.example.appointment.security.JwtUtil;

import lombok.RequiredArgsConstructor;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final EmailService emailService1;

    public String register(RegisterRequest request) {

        if (userRepository.existsByUsername(request.getUsername())) {
            return "Username already exists";
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            return "Email already exists";
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        // تحويل String → Enum مع التحقق
        try {
            user.setRole(Role.valueOf(request.getRole().name() ));
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid role: must be ADMIN, STAFF, or CUSTOMER");
        }

        userRepository.save(user);



        emailService.sendMail(
                user.getEmail(),
                "Welcome to Appointment System",
                "Your account has been created successfully!"
        );

        return "User registered successfully";

    }

    // LOGIN
    public AuthResponse login(LoginRequest request) {

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        String token = jwtUtil.generateToken(
                user.getId(),
                user.getUsername(),
                user.getRole().name()   // ✔ يحول Enum إلى String
        );



        emailService1.sendMail(
                user.getEmail(),
                "Welcome to Appointment System",
                "Your account has login successfully!"
        );




        return new AuthResponse(token);
    }
}
