package com.otbs.medVisit.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record MedicalVisitResponse (
    Long id,
    String doctorName,
    LocalDate visitDate,
    LocalTime startTime,
    LocalTime endTime,
    Integer numberOfAppointments
)
{}