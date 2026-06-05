package com.example.appointment.controller;

import com.example.appointment.dto.StatisticsResponse;
import com.example.appointment.service.StatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;

    @Operation(summary = "Dashboard statistics")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public StatisticsResponse getStatistics() {
        return statisticsService.getStatistics();
    }
}