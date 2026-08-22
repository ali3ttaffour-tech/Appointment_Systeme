package com.example.appointment.service;

import com.example.appointment.dto.AppointmentResponse;
import com.example.appointment.dto.AvailableSlotResponse;
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
import org.springframework.security.core.Authentication;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AppointmentService {

    @Autowired
    private NotificationService notificationService;

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

    private AppointmentResponse toResponse(Appointment appointment) {
        return new AppointmentResponse(
                appointment.getId(),
                appointment.getCustomer().getId(),
                appointment.getCustomer().getUsername(),
                appointment.getService().getId(),
                appointment.getService().getName(),
                appointment.getStartTime(),
                appointment.getEndTime(),
                appointment.getStatus()
        );
    }

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


        public void deleteAppointment(String id) {
        appointmentRepository.deleteById(id);
        }

    @Transactional
    public AppointmentResponse createAppointment(String serviceId, LocalDateTime startTime) {

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

        Appointment saved = appointmentRepository.save(appointment);
        return toResponse(saved);
    }

    public AppointmentResponse updateStatus(String id, AppointmentStatus status) {

        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

        appointment.setStatus(status);

        notificationService.sendToUser(
                appointment.getCustomer().getUsername(),
                "Your appointment status changed to " + status
        );
        Appointment saved = appointmentRepository.save(appointment);
        return toResponse(saved);
    }

    public List<AppointmentResponse> getAllAppointments() {
        return appointmentRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<AvailableSlotResponse> getAvailableSlots(
            String serviceId,
            LocalDate date
    ) {

        ServiceEntity service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new RuntimeException("Service not found"));

        int duration = service.getDurationMinutes();

        WorkingSchedule schedule = workingScheduleRepository
                .findByDayOfWeek(date.getDayOfWeek().getValue())
                .orElseThrow(() -> new RuntimeException("No working schedule"));

        if (schedule.isHoliday()) {
            return List.of();
        }

        LocalDateTime dayStart = LocalDateTime.of(date, schedule.getStartTime());
        LocalDateTime dayEnd = LocalDateTime.of(date, schedule.getEndTime());

        List<Appointment> bookedAppointments =
                appointmentRepository.findAppointmentsForDay(
                        serviceId,
                        dayStart,
                        dayEnd
                );

        List<AvailableSlotResponse> availableSlots = new ArrayList<>();

        LocalDateTime slotStart = dayStart;

        while (!slotStart.plusMinutes(duration).isAfter(dayEnd)) {

            LocalDateTime currentSlotStart = slotStart;
            LocalDateTime slotEnd = currentSlotStart.plusMinutes(duration);

            boolean conflict = bookedAppointments.stream()
                    .anyMatch(a ->
                            a.getStartTime().isBefore(slotEnd)
                                    &&
                                    a.getEndTime().isAfter(currentSlotStart)
                    );

            if (!conflict) {
                availableSlots.add(
                        new AvailableSlotResponse(
                                currentSlotStart,
                                slotEnd
                        )
                );
            }

slotStart = slotStart.plusMinutes(duration);
        }

        return availableSlots;
    }

    public List<AppointmentResponse> getMyAppointments() {

        String username = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        return appointmentRepository.findByCustomerUsername(username)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public AppointmentResponse cancelAppointment(String appointmentId) {

        String username = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        boolean isAdmin = auth.getAuthorities()
                .stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        boolean isOwner = appointment.getCustomer()
                .getUsername()
                .equals(username);

        if (!isAdmin && !isOwner) {
            throw new RuntimeException("You are not allowed to cancel this appointment");
        }

        if (appointment.getStatus() == AppointmentStatus.FINISHED) {
            throw new RuntimeException("Finished appointment cannot be cancelled");
        }

        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new RuntimeException("Appointment already cancelled");
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);

        notificationService.sendToUser(
                appointment.getCustomer().getUsername(),
                "Your appointment has been cancelled"
        );

        emailService.sendMail(
                appointment.getCustomer().getEmail(),
                "Appointment Cancelled",
                "Your appointment has been cancelled"
        );

        Appointment saved = appointmentRepository.save(appointment);
        return toResponse(saved);
    }
}
