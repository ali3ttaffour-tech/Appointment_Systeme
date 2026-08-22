package com.example.appointment.dto;

import com.example.appointment.enums.AppointmentStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class AppointmentResponse {
    private String id;
    private String customerId;
    private String customerName;
    private String serviceId;
    private String serviceName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private AppointmentStatus status;
}
