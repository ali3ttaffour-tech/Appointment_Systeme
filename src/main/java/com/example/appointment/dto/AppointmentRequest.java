package com.example.appointment.dto;

import lombok.Data;
import java.time.LocalDateTime;
@Data
public class AppointmentRequest {
    private Long serviceId;
    private Long userId;
    private LocalDateTime startTime;
}
