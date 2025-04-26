package com.otbs.medVisit.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Entity
@Table(name = "medical_visit")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicalVisit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String doctorName;
    private LocalDate visitDate;
    private LocalTime startTime;
    private LocalTime endTime;
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
    @OneToMany(mappedBy = "medicalVisit", cascade = CascadeType.ALL)
    private List<Appointment> appointments;

    public MedicalVisit(String doctorName, LocalDate visitDate, LocalTime startTime, LocalTime endTime) {
        this.doctorName = doctorName;
        this.visitDate = visitDate;
        this.startTime = startTime;
        this.endTime = endTime;
    }
}
