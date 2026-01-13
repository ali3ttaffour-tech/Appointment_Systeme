package com.example.appointment.controller;

import com.example.appointment.entity.ServiceEntity;
import com.example.appointment.repository.ServiceRepository;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/services")
public class ServiceController {

    @Autowired
    private ServiceRepository serviceRepository;
<<<<<<< HEAD
    @Operation(summary = "Create new service", description = "Only admin can add new service11")
=======
    @Operation(summary = "Create new service", description = "Only admin can add new service1")
>>>>>>> main1
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ServiceEntity create(@RequestBody ServiceEntity service) {
        return serviceRepository.save(service);
    }


    @Operation(summary = "Update new service", description = "Only admin can Update new service")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ServiceEntity update(@PathVariable String id, @RequestBody ServiceEntity service) {
        ServiceEntity existing = serviceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Service not found"));

        existing.setName(service.getName());
        existing.setDescription(service.getDescription());
        existing.setPrice(service.getPrice());
        existing.setDurationMinutes(service.getDurationMinutes());

        return serviceRepository.save(existing);
    }


    @Operation(summary = "Delete new service", description = "Only admin can delete new service")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable String id) {
        serviceRepository.deleteById(id);
    }


    @Operation(summary = "Get ALl service", description = "Only (admin,customer,staff) can get all service")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','STAFF','CUSTOMER')")
    public List<ServiceEntity> getAllServices() {
        return serviceRepository.findAll();
    }


}
