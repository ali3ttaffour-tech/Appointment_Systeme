package com.example.appointment.repository;

import com.example.appointment.entity.ServiceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceRepository extends JpaRepository<ServiceEntity,String> {
}
