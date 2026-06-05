package com.example.appointment.dto;

import lombok.Data;

import java.time.LocalTime;

@Data
public class WorkingScheduleRequest {

    private Integer dayOfWeek;

    private LocalTime startTime;

    private LocalTime endTime;

    private boolean holiday;
}