package com.example.appointment.service;

import com.example.appointment.entity.Appointment;
import com.example.appointment.enums.AppointmentStatus;
import com.example.appointment.repository.AppointmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReminderService {

    private final AppointmentRepository appointmentRepository;
    private final EmailService emailService;

    // يشتغل كل دقيقة
    @Scheduled(fixedRate = 60000)
    public void sendReminders() {

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime reminderTime = now.plusHours(1);

        // كل المواعيد يلي بعد ساعة
        List<Appointment> upcomingAppointments =
                appointmentRepository.findByStartTimeBetween(
                        reminderTime.minusMinutes(1),  // قبل دقيقة
                        reminderTime.plusMinutes(1)    // بعد دقيقة
                );

        for (Appointment appointment : upcomingAppointments) {

            // نتأكد إنو الموعد لسه pending
            if (appointment.getStatus() == AppointmentStatus.PENDING) {

                String to = appointment.getCustomer().getEmail();
                String subject = "Reminder: Your appointment is in 1 hour";
                String message =
                        "Dear " + appointment.getCustomer().getUsername() + ",\n\n" +
                                "This is a reminder that your appointment for service '" +
                                appointment.getService().getName() +
                                "' is scheduled at " + appointment.getStartTime() +
                                "\n\nBest regards,\nAppointment System";

                emailService.sendMail(to, subject, message);
            }
        }
    }
}
