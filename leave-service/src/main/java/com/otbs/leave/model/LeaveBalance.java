package com.otbs.leave.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "leave_balance")
@Getter
@Setter
@NoArgsConstructor
public class LeaveBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String userDn;

    @Column(nullable = false)
    private Integer totalLeave;

    @Column(nullable = false)
    private Integer usedLeave;

    @Column(nullable = false)
    private Integer remainingLeave;

    @Column(nullable = false)
    private LocalDate lastUpdatedDate;

    public LeaveBalance(String userDn, Integer totalLeave, Integer usedLeave, Integer remainingLeave, LocalDate lastUpdatedDate) {
        this.userDn = userDn;
        this.totalLeave = totalLeave;
        this.usedLeave = usedLeave;
        this.remainingLeave = remainingLeave;
        this.lastUpdatedDate = lastUpdatedDate;
    }

    public void addMonthlyLeave() {
        if (lastUpdatedDate != null && lastUpdatedDate.plusMonths(1).isBefore(LocalDate.now())) {
            totalLeave += 2;
            remainingLeave = totalLeave - usedLeave;
            lastUpdatedDate = LocalDate.now();
        }
    }
}