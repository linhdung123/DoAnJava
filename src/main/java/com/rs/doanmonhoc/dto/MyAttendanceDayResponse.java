package com.rs.doanmonhoc.dto;

import com.rs.doanmonhoc.model.AttendanceStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Một ngày trong lịch cá nhân: có mặt / vắng / nghỉ phép (đã duyệt hoặc chờ duyệt).
 *
 * <p>Có thể có nhiều lượt vào/ra trong ngày — xem {@link #segments()}. {@link #checkIn()}/{@link
 * #checkOut()} là tóm tắt: giờ vào sớm nhất; giờ ra muộn nhất nếu đã đóng hết lượt, hoặc null nếu
 * còn lượt chưa ra. {@link #totalPresentSeconds()} là tổng thời gian có mặt (lượt đang mở trong
 * ngày hôm nay tính đến thời điểm gọi API). {@link #overtimeSeconds()} là phần vượt mục tiêu
 * giờ/ngày (cấu hình {@code app.attendance.daily-target-hours}), ước tính từ tổng có mặt — chưa
 * đồng bộ bảng ghi tăng ca thủ công.
 */
public record MyAttendanceDayResponse(
        LocalDate date,
        /** PRESENT, ABSENT, ON_LEAVE, LEAVE_PENDING */
        String dayStatus,
        String leaveTypeName,
        /** APPROVED, PENDING hoặc null */
        String leaveRequestStatus,
        Instant checkIn,
        Instant checkOut,
        AttendanceStatus attendanceStatus,
        List<AttendanceSegmentDto> segments,
        Long totalPresentSeconds,
        Long overtimeSeconds) {}
