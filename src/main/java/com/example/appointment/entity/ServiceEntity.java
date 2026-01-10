package com.example.appointment.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.UUID;

@Entity
@Table(name = "services")
@Data
public class ServiceEntity {
    @Id

    private String id;

    private String name;
    private String description;
    private Double price;
    private Integer durationMinutes;




    @PrePersist
    public void generateId() {
        this.id = UUID.randomUUID().toString();
    }

}
