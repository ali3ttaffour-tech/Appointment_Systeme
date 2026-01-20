package com.example.appointment.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalTime;

@Entity
@Table(name = "working_schedules")
@Data
public class WorkingSchedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private boolean isHoliday;
}
