package com.otbs.medVisit.mapper;

import com.otbs.medVisit.dto.AppointmentResponse;
import com.otbs.medVisit.model.Appointment;
import org.springframework.stereotype.Service;

@Service
public class AppointmentMapper {

    public AppointmentResponse toDto(Appointment appointment, String employeeFullName, String employeeEmail) {
        return new AppointmentResponse(
                appointment.getId(),
                appointment.getMedicalVisit().getId(),
                appointment.getMedicalVisit().getDoctorName(),
                appointment.getTimeSlot(),
                appointment.getStatus(),
                employeeFullName,
                employeeEmail
        );
    }
}
