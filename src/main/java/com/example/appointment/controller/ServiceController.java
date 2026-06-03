package com.example.appointment.controller;

import com.example.appointment.entity.ServiceEntity;
import com.example.appointment.repository.ServiceRepository;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/services")
@RequiredArgsConstructor
public class ServiceController {

    private final ServiceRepository serviceRepository;

    @Operation(summary = "Create new service")
    @PostMapping
    public ServiceEntity create(@RequestBody ServiceEntity service) {
        return serviceRepository.save(service);
    }

    @Operation(summary = "Update service")
    @PutMapping("/{id}")
    public ServiceEntity update(@PathVariable String id, @RequestBody ServiceEntity service) {

        ServiceEntity existing = serviceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Service not found"));

        existing.setName(service.getName());
        existing.setDescription(service.getDescription());
        existing.setPrice(service.getPrice());
        existing.setDurationMinutes(service.getDurationMinutes());

        return serviceRepository.save(existing);
    }

    @Operation(summary = "Delete service")
    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) {
        serviceRepository.deleteById(id);
    }

    @Operation(summary = "Get all services")
    @GetMapping
    public List<ServiceEntity> getAllServices() {
        return serviceRepository.findAll();
    }
}