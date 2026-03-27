package com.rs.doanmonhoc.service;

import org.springframework.stereotype.Service;

@Service
public class WorkdayCalculatorService {

    public double calculateWorkdayByLateMinutes(int lateMinutes) {
        if (lateMinutes < 15) {
            return 1.0;
        }
        if (lateMinutes <= 60) {
            return 0.9;
        }
        if (lateMinutes <= 240) {
            return 0.5;
        }
        return 0.0;
    }
}
