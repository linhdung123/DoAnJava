package com.rs.doanmonhoc.config;

import com.rs.doanmonhoc.model.Employee;
import com.rs.doanmonhoc.model.EmployeeRole;
import com.rs.doanmonhoc.model.EmployeeStatus;
import com.rs.doanmonhoc.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class BootstrapAdminConfig {

    @Bean
    CommandLineRunner seedAdminAccount(
            EmployeeRepository employeeRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.security.bootstrap-admin.employee-code:admin}") String adminCode,
            @Value("${app.security.bootstrap-admin.password:admin123456}") String adminPassword,
            @Value("${app.security.bootstrap-admin.full-name:System Admin}") String adminFullName,
            @Value("${app.security.bootstrap-admin.email:admin@local}") String adminEmail) {
        return args -> {
            if (employeeRepository.findByEmployeeCode(adminCode).isPresent()) {
                return;
            }
            Employee admin = new Employee();
            admin.setEmployeeCode(adminCode);
            admin.setFullName(adminFullName);
            admin.setEmail(adminEmail);
            admin.setStatus(EmployeeStatus.ACTIVE);
            admin.setRole(EmployeeRole.ROLE_ADMIN);
            admin.setPasswordHash(passwordEncoder.encode(adminPassword));
            employeeRepository.save(admin);
        };
    }
}
