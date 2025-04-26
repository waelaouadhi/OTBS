package com.otbs.medVisit.service;

import com.otbs.medVisit.dto.AppointmentRequest;
import com.otbs.medVisit.dto.AppointmentResponse;
import com.otbs.medVisit.model.Appointment;

import java.util.List;

public interface AppointmentService {
    void createAppointment(AppointmentRequest appointment);

    void updateAppointment(AppointmentRequest appointment, Long id);

    void deleteAppointment(Long id);
    AppointmentResponse getAppointmentById(Long id);
    List<AppointmentResponse> getAllAppointments();
    List<AppointmentResponse> getAppointmentsByPatientId(String patientId);
    List<AppointmentResponse> getAppointmentsByMedVisitId(String medVisitId);
}
