package com.example.appointment.controller;

import com.example.appointment.dto.WorkingScheduleRequest;
import com.example.appointment.entity.WorkingSchedule;
import com.example.appointment.service.WorkingScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/schedules")
@RequiredArgsConstructor
public class WorkingScheduleController {

    private final WorkingScheduleService service;

    @Operation(summary = "Create schedule")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public WorkingSchedule create(
            @RequestBody WorkingScheduleRequest request
    ) {
        return service.create(request);
    }

    @Operation(summary = "Get all schedules")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<WorkingSchedule> getAll() {
        return service.getAll();
    }

    @Operation(summary = "Update schedule")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public WorkingSchedule update(
            @PathVariable Long id,
            @RequestBody WorkingScheduleRequest request
    ) {
        return service.update(id, request);
    }

    @Operation(summary = "Delete schedule")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    @Operation(summary = "Update holiday")
    @PatchMapping("/{id}/holiday")
    @PreAuthorize("hasRole('ADMIN')")
    public WorkingSchedule updateHoliday(
            @PathVariable Long id,
            @RequestParam boolean holiday
    ) {
        return service.updateHoliday(id, holiday);
    }
}