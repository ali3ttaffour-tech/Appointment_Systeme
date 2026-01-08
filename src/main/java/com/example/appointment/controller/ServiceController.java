package com.example.appointment.controller;

import com.example.appointment.entity.ServiceEntity;
import com.example.appointment.repository.ServiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/services")
public class ServiceController {

    @Autowired
    private ServiceRepository serviceRepository;
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ServiceEntity create(@RequestBody ServiceEntity service) {
        return serviceRepository.save(service);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ServiceEntity update(@PathVariable Long id, @RequestBody ServiceEntity service) {
        ServiceEntity existing = serviceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Service not found"));

        existing.setName(service.getName());
        existing.setDescription(service.getDescription());
        existing.setPrice(service.getPrice());
        existing.setDurationMinutes(service.getDurationMinutes());

        return serviceRepository.save(existing);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        serviceRepository.deleteById(id);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','STAFF','CUSTOMER')")
    public List<ServiceEntity> getAllServices() {
        return serviceRepository.findAll();
    }


}
