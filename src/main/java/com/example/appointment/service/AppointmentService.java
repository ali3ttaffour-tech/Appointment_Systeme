package com.example.appointment.service;

import com.example.appointment.entity.Appointment;
import com.example.appointment.entity.ServiceEntity;
import com.example.appointment.entity.User;
import com.example.appointment.entity.WorkingSchedule;
import com.example.appointment.enums.AppointmentStatus;
import com.example.appointment.repository.AppointmentRepository;
import com.example.appointment.repository.ServiceRepository;
import com.example.appointment.repository.UserRepository;
import com.example.appointment.repository.WorkingScheduleRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AppointmentService {

    @Autowired
    private EmailService emailService;


    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private WorkingScheduleRepository workingScheduleRepository;

    @Transactional
    public Appointment createAppointment(Long serviceId, LocalDateTime startTime) {

        // 1) جيب اسم المستخدم من التوكن
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 2) جلب الخدمة
        ServiceEntity service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new RuntimeException("Service not found"));

        LocalDateTime endTime = startTime.plusMinutes(service.getDurationMinutes());

        // 3) Working hours
        WorkingSchedule schedule = workingScheduleRepository
                .findByDayOfWeek(startTime.getDayOfWeek().getValue())
                .orElseThrow(() -> new RuntimeException("No working schedule"));

        if (schedule.isHoliday()) {
            throw new RuntimeException("Holiday");
        }

        if (startTime.toLocalTime().isBefore(schedule.getStartTime())
                || endTime.toLocalTime().isAfter(schedule.getEndTime())) {
            throw new RuntimeException("Outside working hours");
        }

        // 4) Conflict check
        boolean conflict = !appointmentRepository
                .findOverlappingAppointmentsForService(serviceId, startTime, endTime)
                .isEmpty();

        if (conflict) {
            throw new RuntimeException("Service already booked at this time");
        }

        // 5) إنشاء الموعد
        Appointment appointment = new Appointment();
        appointment.setCustomer(user);
        appointment.setService(service);
        appointment.setStartTime(startTime);
        appointment.setEndTime(endTime);
        appointment.setStatus(AppointmentStatus.PENDING);


        emailService.sendMail(
                user.getEmail(),
                "Appointment Confirmed",
                "Your appointment for service " + service.getName() +
                        " has been booked at " + startTime
        );


        return appointmentRepository.save(appointment);
    }







    public Appointment updateStatus(Long id, AppointmentStatus status) {

        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

        appointment.setStatus(status);
        return appointmentRepository.save(appointment);
    }



    public List<Appointment> getAllAppointments() {
        return appointmentRepository.findAll();
    }


}