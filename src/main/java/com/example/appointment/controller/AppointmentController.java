package com.example.appointment.controller;

import com.example.appointment.dto.AppointmentRequest;
import com.example.appointment.entity.Appointment;
import com.example.appointment.enums.AppointmentStatus;
import com.example.appointment.service.AppointmentService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;



@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;

    @Operation(summary = "Create appointment")
    @PostMapping
    public Appointment createAppointment(@RequestBody AppointmentRequest req) {
        return appointmentService.createAppointment(req.getServiceId(), req.getStartTime());
    }

    @Operation(summary = "Get all appointments")
    @GetMapping
    public List<Appointment> getAllAppointments() {
        return appointmentService.getAllAppointments();
    }

    @Operation(summary = "Update appointment status")
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(
            @PathVariable String id,
            @RequestParam AppointmentStatus status
    ) {
        try {
            return ResponseEntity.ok(
                    appointmentService.updateStatus(id, status)
            );
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
