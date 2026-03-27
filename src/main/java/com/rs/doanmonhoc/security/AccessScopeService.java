package com.rs.doanmonhoc.security;

import com.rs.doanmonhoc.exception.BusinessException;
import com.rs.doanmonhoc.model.Employee;
import com.rs.doanmonhoc.repository.EmployeeRepository;
import org.springframework.stereotype.Service;

@Service
public class AccessScopeService {

    private final EmployeeRepository employeeRepository;

    public AccessScopeService(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    public Integer resolveHistoryEmployeeId(AuthPrincipal principal, Integer requestedEmployeeId) {
        if (principal == null) {
            throw new BusinessException("Thiếu thông tin xác thực");
        }
        if (principal.hasRole("ROLE_ADMIN")) {
            if (requestedEmployeeId == null) {
                throw new BusinessException("Admin cần truyền employeeId khi xem lịch sử");
            }
            return requestedEmployeeId;
        }
        if (principal.hasRole("ROLE_MANAGER")) {
            if (requestedEmployeeId == null) {
                throw new BusinessException("Manager cần truyền employeeId khi xem lịch sử");
            }
            Employee target =
                    employeeRepository
                            .findByIdWithDepartment(requestedEmployeeId)
                            .orElseThrow(() -> new BusinessException("Không tìm thấy nhân viên"));
            Integer managerDepartmentId = principal.departmentId();
            Integer targetDepartmentId =
                    target.getDepartment() != null ? target.getDepartment().getId() : null;
            if (managerDepartmentId == null || !managerDepartmentId.equals(targetDepartmentId)) {
                throw new BusinessException("Không có quyền xem lịch sử ngoài phòng ban quản lý");
            }
            return requestedEmployeeId;
        }
        return principal.employeeId();
    }
}
