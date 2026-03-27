package com.rs.doanmonhoc.service;

import com.rs.doanmonhoc.dto.AuthLoginRequest;
import com.rs.doanmonhoc.dto.AuthLoginResponse;
import com.rs.doanmonhoc.dto.AuthChangeInitialPasswordRequest;
import com.rs.doanmonhoc.exception.BusinessException;
import com.rs.doanmonhoc.model.Employee;
import com.rs.doanmonhoc.model.EmployeeRole;
import com.rs.doanmonhoc.model.EmployeeStatus;
import com.rs.doanmonhoc.repository.EmployeeRepository;
import com.rs.doanmonhoc.security.AuthPrincipal;
import com.rs.doanmonhoc.security.JwtTokenService;
import java.util.HashSet;
import java.util.Set;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;

    public AuthService(
            EmployeeRepository employeeRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService) {
        this.employeeRepository = employeeRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
    }

    @Transactional(readOnly = true)
    public AuthLoginResponse login(AuthLoginRequest req) {
        Employee employee =
                employeeRepository
                        .findByEmployeeCode(req.employeeCode())
                        .orElseThrow(() -> new BusinessException("Sai tài khoản hoặc mật khẩu"));
        if (employee.getStatus() != EmployeeStatus.ACTIVE) {
            throw new BusinessException("Tài khoản đã bị khóa hoặc ngừng hoạt động");
        }
        if (employee.getPasswordHash() == null || employee.getPasswordHash().isBlank()) {
            throw new BusinessException("Tài khoản chưa được thiết lập mật khẩu");
        }
        if (!passwordEncoder.matches(req.password(), employee.getPasswordHash())) {
            throw new BusinessException("Sai tài khoản hoặc mật khẩu");
        }
        Set<String> roles = resolveRoles(employee.getRole());
        Integer departmentId = employee.getDepartment() != null ? employee.getDepartment().getId() : null;
        AuthPrincipal principal = new AuthPrincipal(employee.getId(), departmentId, roles);
        String token = jwtTokenService.generateToken(principal);
        return new AuthLoginResponse(
                token,
                "Bearer",
                employee.getId(),
                employee.getEmployeeCode(),
                employee.getFullName(),
                departmentId,
                roles,
                employee.isMustChangePassword());
    }

    @Transactional
    public void changeInitialPassword(AuthChangeInitialPasswordRequest req) {
        Employee employee =
                employeeRepository
                        .findByEmployeeCode(req.employeeCode())
                        .orElseThrow(() -> new BusinessException("Không tìm thấy tài khoản"));
        if (!employee.isMustChangePassword()) {
            throw new BusinessException("Tài khoản này không ở trạng thái bắt buộc đổi mật khẩu lần đầu");
        }
        if (employee.getPasswordHash() == null || employee.getPasswordHash().isBlank()) {
            throw new BusinessException("Tài khoản chưa có mật khẩu tạm");
        }
        if (!passwordEncoder.matches(req.currentPassword(), employee.getPasswordHash())) {
            throw new BusinessException("Mật khẩu hiện tại không đúng");
        }
        if (req.newPassword().equals(req.currentPassword())) {
            throw new BusinessException("Mật khẩu mới phải khác mật khẩu tạm");
        }
        employee.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        employee.setMustChangePassword(false);
        employeeRepository.save(employee);
    }

    private static Set<String> resolveRoles(EmployeeRole role) {
        Set<String> roles = new HashSet<>();
        if (role == EmployeeRole.ROLE_ADMIN) {
            roles.add("ROLE_ADMIN");
        } else if (role == EmployeeRole.ROLE_MANAGER) {
            roles.add("ROLE_MANAGER");
        } else {
            roles.add("ROLE_EMPLOYEE");
            roles.add("ROLE_USER");
        }
        return roles;
    }
}
