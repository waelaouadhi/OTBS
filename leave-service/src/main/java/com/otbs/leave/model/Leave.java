package com.otbs.leave.model;

import jakarta.persistence.*;
import jakarta.persistence.Table;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.*;

import java.time.*;

@Entity
@Table(name = "leaves")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "attachment")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Leave {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, length = 100)
    @NotBlank(message = "User DN cannot be empty")
    private String userDn;

    @Column(nullable = false)
    @NotNull(message = "Start date is required")
    @FutureOrPresent(message = "Start date must be in the present or future")
    private LocalDate startDate;

    @Column(nullable = false)
    @NotNull(message = "End date is required")
    @Future(message = "End date must be in the future")
    private LocalDate endDate;

    private LocalTime startTime;
    private LocalTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull(message = "Leave type is required")
    private ELeaveType leaveType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull(message = "Status is required")
    private EStatus status;

    @Lob
    @Column(name = "attachment")
    private byte[] attachment;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}