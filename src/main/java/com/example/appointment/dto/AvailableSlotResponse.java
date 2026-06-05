package com.example.appointment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class AvailableSlotResponse {

    private LocalDateTime startTime;
    private LocalDateTime endTime;
}