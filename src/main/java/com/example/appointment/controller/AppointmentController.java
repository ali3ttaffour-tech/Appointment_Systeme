package com.example.appointment.controller;

import com.example.appointment.dto.AppointmentRequest;
import com.example.appointment.entity.Appointment;
import com.example.appointment.enums.AppointmentStatus;
import com.example.appointment.service.AppointmentService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    @Autowired
    private AppointmentService appointmentService;


    @Operation(summary = " Appointment new service", description = "Only customer can Appointment service")
    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public Appointment createAppointment(@RequestBody AppointmentRequest req) {
        return appointmentService.createAppointment(req.getServiceId(), req.getStartTime());
    }



    @Operation(summary = "Get ALl Appointment", description = "Only (admin,staff) get all Appointment")
    @GetMapping
   @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public List<Appointment> getAllAppointments() {
        return appointmentService.getAllAppointments();
    }




    @Operation(summary = "Update  Appointment", description = "Only (admin,staff) can update status Appointment")
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<?> updateStatus(
            @PathVariable String id,
            @RequestParam AppointmentStatus status
    ) {
        try {
            return ResponseEntity.ok(appointmentService.updateStatus(id, status));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

}
