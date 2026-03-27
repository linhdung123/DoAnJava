package com.rs.doanmonhoc.service;

import com.rs.doanmonhoc.model.AttendanceLog;
import com.rs.doanmonhoc.exception.BusinessException;
import com.rs.doanmonhoc.repository.AttendanceLogRepository;
import com.rs.doanmonhoc.security.AuthPrincipal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    private final AttendanceLogRepository attendanceLogRepository;

    public ReportService(AttendanceLogRepository attendanceLogRepository) {
        this.attendanceLogRepository = attendanceLogRepository;
    }

    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> monthlyAttendanceCsv(int year, int month, AuthPrincipal principal) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate from = ym.atDay(1);
        LocalDate to = ym.atEndOfMonth();
        List<AttendanceLog> rows;
        if (principal.hasRole("ROLE_ADMIN")) {
            rows = attendanceLogRepository.findByDateBetween(from, to);
        } else if (principal.hasRole("ROLE_MANAGER")) {
            if (principal.departmentId() == null) {
                throw new BusinessException("Manager chưa gắn phòng ban, không thể xuất báo cáo");
            }
            rows =
                    attendanceLogRepository.findByDateBetweenAndDepartmentId(
                            from, to, principal.departmentId());
        } else {
            throw new BusinessException("Không có quyền xuất báo cáo chấm công");
        }

        StringBuilder sb = new StringBuilder();
        sb.append('\uFEFF');
        sb.append("employee_code,full_name,date,check_in,check_out,status,verify_method,location\n");
        for (AttendanceLog a : rows) {
            var e = a.getEmployee();
            sb.append(csv(e.getEmployeeCode()))
                    .append(',')
                    .append(csv(e.getFullName()))
                    .append(',')
                    .append(a.getDate().format(ISO_DATE))
                    .append(',')
                    .append(a.getCheckIn() != null ? a.getCheckIn().atZone(ZONE).toLocalDateTime().toString() : "")
                    .append(',')
                    .append(a.getCheckOut() != null ? a.getCheckOut().atZone(ZONE).toLocalDateTime().toString() : "")
                    .append(',')
                    .append(a.getStatus() != null ? a.getStatus().name() : "")
                    .append(',')
                    .append(a.getVerifyMethod() != null ? a.getVerifyMethod().name() : "")
                    .append(',')
                    .append(csv(a.getLocation()))
                    .append('\n');
        }

        byte[] body = sb.toString().getBytes(StandardCharsets.UTF_8);
        String filename = String.format("bang-cong-%04d-%02d.csv", year, month);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("text", "csv", StandardCharsets.UTF_8));
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
        return ResponseEntity.ok().headers(headers).body(body);
    }

    private static String csv(String s) {
        if (s == null) {
            return "";
        }
        String x = s.replace("\"", "\"\"");
        if (x.contains(",") || x.contains("\"") || x.contains("\n")) {
            return "\"" + x + "\"";
        }
        return x;
    }
}
