package com.example.appointment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StatisticsResponse {

    private long totalUsers;

    private long totalServices;

    private long totalAppointments;

    private long pendingAppointments;

    private long acceptedAppointments;

    private long cancelledAppointments;

    private long finishedAppointments;
}