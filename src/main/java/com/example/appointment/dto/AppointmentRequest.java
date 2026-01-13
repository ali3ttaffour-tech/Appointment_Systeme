package com.example.appointment.dto;

import lombok.Data;
import java.time.LocalDateTime;
@Data
public class AppointmentRequest {
    private String serviceId;
    private Long userId;
    private LocalDateTime startTime;
}
