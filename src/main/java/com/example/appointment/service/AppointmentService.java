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
import java.time.LocalTime;
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

    private boolean isValidSlot(LocalDateTime startTime, LocalTime workStart, LocalTime workEnd, int duration) {

        // نحدد أول فتحة زمنية في اليوم
        LocalDateTime slot = startTime.withHour(workStart.getHour())
                .withMinute(workStart.getMinute())
                .withSecond(0)
                .withNano(0);

        // نهاية الدوام
        LocalDateTime endOfDay = startTime.withHour(workEnd.getHour())
                .withMinute(workEnd.getMinute())
                .withSecond(0)
                .withNano(0);

        // نولّد كل الفترات الممكنة ونقارن مع وقت الحجز
        while (!slot.isAfter(endOfDay)) {

            if (slot.equals(startTime)) {
                return true;   // وقت الحجز صحيح
            }

            slot = slot.plusMinutes(duration);  // نولّد الفتحة التالية
        }

        return false;  // وقت غير صحيح
    }











    @Transactional
    public Appointment createAppointment(String serviceId, LocalDateTime startTime) {

        // 1) جلب الاسم من التوكن
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 2) الخدمة
        ServiceEntity service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new RuntimeException("Service not found"));

        int duration = service.getDurationMinutes();
        LocalDateTime endTime = startTime.plusMinutes(duration);

        // 3) دوام اليوم
        WorkingSchedule schedule = workingScheduleRepository
                .findByDayOfWeek(startTime.getDayOfWeek().getValue())
                .orElseThrow(() -> new RuntimeException("No working schedule"));

        if (schedule.isHoliday()) {
            throw new RuntimeException("Holiday");
        }

        // ————————————————————————
        // 🟢 4) التحقق من أن الفتحة الزمنية صحيحة
        boolean validSlot = isValidSlot(
                startTime,
                schedule.getStartTime(),
                schedule.getEndTime(),
                duration
        );

        if (!validSlot) {
            throw new RuntimeException("Invalid time slot for this service duration");
        }
        // ————————————————————————

        // 5) التحقق من التداخل
        boolean conflict = !appointmentRepository
                .findOverlappingAppointmentsForService(serviceId, startTime, endTime)
                .isEmpty();

        if (conflict) {
            throw new RuntimeException("Service already booked at this time");
        }

        // 6) إنشاء الموعد
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










    public Appointment updateStatus(String id, AppointmentStatus status) {

        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

        appointment.setStatus(status);
        return appointmentRepository.save(appointment);
    }



    public List<Appointment> getAllAppointments() {
        return appointmentRepository.findAll();
    }


}