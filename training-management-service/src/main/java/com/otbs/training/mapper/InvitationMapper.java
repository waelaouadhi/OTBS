package com.otbs.training.mapper;

import com.otbs.feign.client.EmployeeClient;
import com.otbs.feign.dto.EmployeeResponse;
import com.otbs.training.dto.InvitationResponseDTO;
import com.otbs.training.model.Invitation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InvitationMapper {

    private final EmployeeClient employeeClient;

    public InvitationResponseDTO toResponseDTO(Invitation invitation) {
        EmployeeResponse employee = employeeClient.getEmployeeByDn(invitation.getEmployeeId());
        return new InvitationResponseDTO(
                invitation.getId(),
                employee.firstName() + " " + employee.lastName(),
                invitation.getStatus(),
                employee.id()
        );
    }


}
