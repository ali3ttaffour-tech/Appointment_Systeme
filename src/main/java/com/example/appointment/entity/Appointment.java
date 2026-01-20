package com.example.appointment.entity;

import com.example.appointment.enums.AppointmentStatus;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "appointments")
@Data
public class Appointment {

    @Id
    private String id;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private User customer;

    @ManyToOne
    @JoinColumn(name = "service_id")
    private ServiceEntity service;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    private AppointmentStatus status;





    @PrePersist
    public void generateId() {
        this.id = UUID.randomUUID().toString();
    }

}
