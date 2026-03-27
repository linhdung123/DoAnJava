package com.rs.doanmonhoc.service;

import com.rs.doanmonhoc.model.Employee;
import com.rs.doanmonhoc.model.LeaveRequest;
import com.rs.doanmonhoc.model.LeaveRequestStatus;
import com.rs.doanmonhoc.model.LeaveType;
import com.rs.doanmonhoc.dto.LeaveRequestCreate;
import com.rs.doanmonhoc.dto.LeaveRequestResponse;
import com.rs.doanmonhoc.exception.BusinessException;
import com.rs.doanmonhoc.repository.EmployeeRepository;
import com.rs.doanmonhoc.repository.LeaveRequestRepository;
import com.rs.doanmonhoc.repository.LeaveTypeRepository;
import com.rs.doanmonhoc.security.AuthPrincipal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LeaveRequestService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final EmployeeRepository employeeRepository;
    private final LeaveTypeRepository leaveTypeRepository;

    public LeaveRequestService(
            LeaveRequestRepository leaveRequestRepository,
            EmployeeRepository employeeRepository,
            LeaveTypeRepository leaveTypeRepository) {
        this.leaveRequestRepository = leaveRequestRepository;
        this.employeeRepository = employeeRepository;
        this.leaveTypeRepository = leaveTypeRepository;
    }

    @Transactional
    public LeaveRequestResponse create(LeaveRequestCreate req, Integer employeeId) {
        if (req.startDate().isAfter(req.endDate())) {
            throw new BusinessException("Ngày bắt đầu không được sau ngày kết thúc");
        }
        Employee emp = employeeRepository.findById(employeeId).orElseThrow(this::employeeNotFound);
        LeaveType type = leaveTypeRepository.findById(req.leaveTypeId()).orElseThrow(this::typeNotFound);

        LeaveRequest r = new LeaveRequest();
        r.setEmployee(emp);
        r.setLeaveType(type);
        r.setStartDate(req.startDate());
        r.setEndDate(req.endDate());
        r.setReason(req.reason());
        r.setStatus(LeaveRequestStatus.PENDING);
        return toResponse(leaveRequestRepository.save(r));
    }

    @Transactional(readOnly = true)
    public List<LeaveRequestResponse> listPending(AuthPrincipal principal) {
        if (principal.hasRole("ROLE_ADMIN")) {
            return leaveRequestRepository.findByStatusOrderByCreatedAtDesc(LeaveRequestStatus.PENDING).stream()
                    .map(this::toResponse)
                    .toList();
        }
        if (!principal.hasRole("ROLE_MANAGER") || principal.departmentId() == null) {
            throw new BusinessException("Không có quyền xem danh sách chờ duyệt");
        }
        return leaveRequestRepository
                .findByStatusAndDepartmentIdOrderByCreatedAtDesc(
                        LeaveRequestStatus.PENDING, principal.departmentId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LeaveRequestResponse> listByEmployee(Integer employeeId) {
        return leaveRequestRepository.findByEmployeeIdOrderByCreatedAtDesc(employeeId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public LeaveRequestResponse approve(Integer id, AuthPrincipal principal) {
        LeaveRequest r = leaveRequestRepository.findById(id).orElseThrow(() -> notFound(id));
        if (r.getStatus() != LeaveRequestStatus.PENDING) {
            throw new BusinessException("Đơn không ở trạng thái chờ duyệt");
        }
        ensureCanModerate(r, principal);
        r.setStatus(LeaveRequestStatus.APPROVED);
        return toResponse(leaveRequestRepository.save(r));
    }

    @Transactional
    public LeaveRequestResponse reject(Integer id, AuthPrincipal principal) {
        LeaveRequest r = leaveRequestRepository.findById(id).orElseThrow(() -> notFound(id));
        if (r.getStatus() != LeaveRequestStatus.PENDING) {
            throw new BusinessException("Đơn không ở trạng thái chờ duyệt");
        }
        ensureCanModerate(r, principal);
        r.setStatus(LeaveRequestStatus.REJECTED);
        return toResponse(leaveRequestRepository.save(r));
    }

    private void ensureCanModerate(LeaveRequest request, AuthPrincipal principal) {
        if (principal.hasRole("ROLE_ADMIN")) {
            return;
        }
        if (!principal.hasRole("ROLE_MANAGER") || principal.departmentId() == null) {
            throw new BusinessException("Không có quyền duyệt đơn");
        }
        Employee owner =
                employeeRepository
                        .findByIdWithDepartment(request.getEmployee().getId())
                        .orElseThrow(this::employeeNotFound);
        Integer ownerDepartmentId = owner.getDepartment() != null ? owner.getDepartment().getId() : null;
        if (!principal.departmentId().equals(ownerDepartmentId)) {
            throw new BusinessException("Chỉ được duyệt đơn thuộc phòng ban đang quản lý");
        }
    }

    private LeaveRequestResponse toResponse(LeaveRequest r) {
        return new LeaveRequestResponse(
                r.getId(),
                r.getEmployee().getId(),
                r.getEmployee().getFullName(),
                r.getLeaveType().getId(),
                r.getLeaveType().getName(),
                r.getStartDate(),
                r.getEndDate(),
                r.getReason(),
                r.getStatus(),
                r.getCreatedAt());
    }

    private static BusinessException notFound(Integer id) {
        return new BusinessException("Không tìm thấy đơn nghỉ: " + id);
    }

    private BusinessException employeeNotFound() {
        return new BusinessException("Không tìm thấy nhân viên");
    }

    private BusinessException typeNotFound() {
        return new BusinessException("Không tìm thấy loại nghỉ");
    }
}
