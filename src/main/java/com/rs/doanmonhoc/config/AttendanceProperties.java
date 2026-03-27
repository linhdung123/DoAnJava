package com.rs.doanmonhoc.config;

import java.time.LocalTime;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Quy tắc chấm công kiểu văn phòng: không cần cấu hình "ca" trong DB — đủ {@link #dailyTargetHours}
 * giờ có mặt/ngày là đạt; phần vượt dùng để hiển thị tăng ca (ước tính) trên lịch.
 */
@ConfigurationProperties(prefix = "app.attendance")
public class AttendanceProperties {

    /** Giờ vào làm kỳ vọng — chỉ để gắn trạng thái đi muộn (lần check-in đầu trong ngày). */
    private LocalTime expectedStart = LocalTime.of(8, 0);

    /** Phút ân hạn sau {@link #expectedStart} vẫn tính đúng giờ. */
    private int lateGraceMinutes = 15;

    /** Số giờ có mặt mục tiêu mỗi ngày (thường 8). */
    private double dailyTargetHours = 8.0;

    public LocalTime getExpectedStart() {
        return expectedStart;
    }

    public void setExpectedStart(LocalTime expectedStart) {
        this.expectedStart = expectedStart;
    }

    public int getLateGraceMinutes() {
        return lateGraceMinutes;
    }

    public void setLateGraceMinutes(int lateGraceMinutes) {
        this.lateGraceMinutes = lateGraceMinutes;
    }

    public double getDailyTargetHours() {
        return dailyTargetHours;
    }

    public void setDailyTargetHours(double dailyTargetHours) {
        this.dailyTargetHours = dailyTargetHours;
    }

    public long dailyTargetSeconds() {
        return Math.round(dailyTargetHours * 3600.0);
    }
}
