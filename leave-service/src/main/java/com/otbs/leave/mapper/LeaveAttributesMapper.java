package com.otbs.leave.mapper;

import com.otbs.leave.dto.LeaveRequest;
import com.otbs.leave.dto.LeaveResponse;
import com.otbs.leave.model.EStatus;
import com.otbs.leave.model.Leave;
import org.springframework.stereotype.Component;

@Component
public class LeaveAttributesMapper {
    public Leave toEntity(LeaveRequest leaveRequest) {

        return Leave.builder()
                .startDate(leaveRequest.startDate())
                .endDate(leaveRequest.endDate())
                .leaveType(leaveRequest.leaveType())
                .status(EStatus.EN_ATTENTE)
                .startTime(leaveRequest.startHOURLY())
                .endTime(leaveRequest.endHOURLY())
                .build();
    }

    public LeaveResponse toDto(Leave leave) {
        return new LeaveResponse(leave.getId(),leave.getUserDn().split(",")[0].split("=")[1], leave.getUserDn().split(",")[1].split("=")[1], leave.getStartDate()
                ,leave.getEndDate(), leave.getLeaveType(), leave.getStatus());
    }

    public void updateEntity(Leave leave, LeaveRequest leaveRequest) {
        leave.setStartDate(leaveRequest.startDate());
        leave.setEndDate(leaveRequest.endDate());
        leave.setLeaveType(leaveRequest.leaveType());
        leave.setStartTime(leaveRequest.startHOURLY());
        leave.setEndTime(leaveRequest.endHOURLY());
    }
}
