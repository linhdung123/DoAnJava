package com.rs.doanmonhoc.service;

import com.rs.doanmonhoc.model.OvertimeLog;
import com.rs.doanmonhoc.model.OvertimeType;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class OvertimeService {

    public double calculateTotalOvertimePay(
            double baseSalary, int standardWorkDays, List<OvertimeLog> overtimeLogs) {
        if (standardWorkDays <= 0 || baseSalary <= 0 || overtimeLogs == null || overtimeLogs.isEmpty()) {
            return 0.0;
        }
        double hourlyRate = baseSalary / standardWorkDays / 8.0;
        return overtimeLogs.stream()
                .mapToDouble(log -> hourlyRate * safeHours(log.getHours()) * overtimeMultiplier(log.getType()))
                .sum();
    }

    private static double safeHours(Double hours) {
        if (hours == null || hours <= 0) {
            return 0.0;
        }
        return hours;
    }

    private static double overtimeMultiplier(OvertimeType type) {
        if (type == null) {
            return 1.0;
        }
        return switch (type) {
            case NORMAL -> 1.5;
            case WEEKEND -> 2.0;
            case HOLIDAY -> 3.0;
        };
    }
}
