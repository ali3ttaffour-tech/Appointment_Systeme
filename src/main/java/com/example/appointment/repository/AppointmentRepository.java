package com.example.appointment.repository;

import com.example.appointment.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDateTime;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    @Query("""
SELECT a FROM Appointment a
WHERE a.service.id = :serviceId
AND a.startTime < :endTime
AND a.endTime > :startTime
AND a.status != 'CANCELLED'
""")
    List<Appointment> findOverlappingAppointmentsForService(
            Long serviceId,
            LocalDateTime startTime,
            LocalDateTime endTime
    );

}
