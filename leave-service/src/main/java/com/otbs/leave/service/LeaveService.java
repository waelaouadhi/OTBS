package com.otbs.leave.service;

import com.otbs.leave.dto.LeaveRequest;
import com.otbs.leave.dto.LeaveResponse;
import com.otbs.leave.model.Leave;
import com.otbs.leave.model.LeaveBalance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface LeaveService {
    void applyLeave(LeaveRequest leaveRequest, MultipartFile attachment) ;
    void cancelLeave(Long leaveId);
    void approveLeave(Long leaveId);
    void rejectLeave(Long leaveId);
    List<Leave> getLeaveHistory(String userDn);
    LeaveBalance getLeaveBalance(String userDn);
    List<LeaveResponse> getAllLeaves();
    Page<Leave> getAllLeavesForManager(Pageable pageable);
    void updateLeave(Long leaveId, LeaveRequest leaveRequest);
    byte[] downloadAttachment(Long leaveId);
    void addMonthlyLeaveForAllEmployees();
}
