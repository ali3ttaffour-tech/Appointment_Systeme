package com.example.appointment.service;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    public void sendToUser(String username, String message) {

        messagingTemplate.convertAndSendToUser(
                username,
                "/queue/notifications",
                message
        );
    }
}