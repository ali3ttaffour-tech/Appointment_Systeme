package com.example.appointment;

import com.example.appointment.entity.WorkingSchedule;
import com.example.appointment.repository.WorkingScheduleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalTime;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private WorkingScheduleRepository workingScheduleRepository;

    @Override
    public void run(String... args) throws Exception {
        if (workingScheduleRepository.count() == 0) {
            for (int i = 1; i <= 7; i++) {
                WorkingSchedule schedule = new WorkingSchedule();
                schedule.setDayOfWeek(i);
                schedule.setStartTime(LocalTime.of(9, 0));
                schedule.setEndTime(LocalTime.of(17, 0));
                schedule.setHoliday(i == 5 || i == 6);
                workingScheduleRepository.save(schedule);
            }
        }
    }
}
