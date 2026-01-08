package com.example.appointment.repository;

import com.example.appointment.entity.WorkingSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface WorkingScheduleRepository extends JpaRepository<WorkingSchedule, Long> {
    Optional<WorkingSchedule> findByDayOfWeek(Integer dayOfWeek);
}
