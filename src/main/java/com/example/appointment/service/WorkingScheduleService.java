package com.example.appointment.service;

import com.example.appointment.dto.WorkingScheduleRequest;
import com.example.appointment.entity.WorkingSchedule;
import com.example.appointment.repository.WorkingScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkingScheduleService {

    private final WorkingScheduleRepository repository;

    public WorkingSchedule create(WorkingScheduleRequest req) {

        WorkingSchedule schedule = new WorkingSchedule();

        schedule.setDayOfWeek(req.getDayOfWeek());
        schedule.setStartTime(req.getStartTime());
        schedule.setEndTime(req.getEndTime());
        schedule.setHoliday(req.isHoliday());

        return repository.save(schedule);
    }

    public List<WorkingSchedule> getAll() {
        return repository.findAll();
    }

    public WorkingSchedule update(Long id, WorkingScheduleRequest req) {

        WorkingSchedule schedule = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Schedule not found"));

        schedule.setDayOfWeek(req.getDayOfWeek());
        schedule.setStartTime(req.getStartTime());
        schedule.setEndTime(req.getEndTime());
        schedule.setHoliday(req.isHoliday());

        return repository.save(schedule);
    }

    public void delete(Long id) {

        WorkingSchedule schedule = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Schedule not found"));

        repository.delete(schedule);
    }

    public WorkingSchedule updateHoliday(Long id, boolean holiday) {

        WorkingSchedule schedule = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Schedule not found"));

        schedule.setHoliday(holiday);

        return repository.save(schedule);
    }
}