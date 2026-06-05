package com.example.appointment.service;

import com.example.appointment.dto.StatisticsResponse;
import com.example.appointment.enums.AppointmentStatus;
import com.example.appointment.repository.AppointmentRepository;
import com.example.appointment.repository.ServiceRepository;
import com.example.appointment.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final UserRepository userRepository;
    private final ServiceRepository serviceRepository;
    private final AppointmentRepository appointmentRepository;

    public StatisticsResponse getStatistics() {

        return new StatisticsResponse(

                userRepository.count(),

                serviceRepository.count(),

                appointmentRepository.count(),

                appointmentRepository.countByStatus(AppointmentStatus.PENDING),

                appointmentRepository.countByStatus(AppointmentStatus.ACCEPTED),

                appointmentRepository.countByStatus(AppointmentStatus.CANCELLED),

                appointmentRepository.countByStatus(AppointmentStatus.FINISHED)
        );
    }
}
