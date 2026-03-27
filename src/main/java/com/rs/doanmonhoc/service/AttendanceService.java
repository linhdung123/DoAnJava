package com.rs.doanmonhoc.service;

import com.rs.doanmonhoc.model.AttendanceLog;
import com.rs.doanmonhoc.model.AttendanceStatus;
import com.rs.doanmonhoc.model.Employee;
import com.rs.doanmonhoc.model.EmployeeStatus;
import com.rs.doanmonhoc.model.LeaveRequest;
import com.rs.doanmonhoc.model.LeaveRequestStatus;
import com.rs.doanmonhoc.config.AttendanceProperties;
import com.rs.doanmonhoc.model.VerifyMethod;
import com.rs.doanmonhoc.dto.AnonymousFaceCheckInRequest;
import com.rs.doanmonhoc.dto.AnonymousNfcToggleRequest;
import com.rs.doanmonhoc.dto.AttendancePresenceRowResponse;
import com.rs.doanmonhoc.dto.AttendanceLogResponse;
import com.rs.doanmonhoc.dto.AttendanceSegmentDto;
import com.rs.doanmonhoc.dto.CheckInRequest;
import com.rs.doanmonhoc.dto.CheckOutRequest;
import com.rs.doanmonhoc.dto.MyAttendanceCalendarResponse;
import com.rs.doanmonhoc.dto.MyAttendanceCalendarSummary;
import com.rs.doanmonhoc.dto.MyAttendanceDayResponse;
import com.rs.doanmonhoc.dto.NfcPreviewResponse;
import com.rs.doanmonhoc.exception.BusinessException;
import com.rs.doanmonhoc.repository.AttendanceLogRepository;
import com.rs.doanmonhoc.repository.EmployeeRepository;
import com.rs.doanmonhoc.repository.LeaveRequestRepository;
import com.rs.doanmonhoc.security.AuthPrincipal;
import com.rs.doanmonhoc.service.face.FaceRecognitionService;
import com.rs.doanmonhoc.util.FaceEmbeddingUtils;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AttendanceService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final int MAX_CALENDAR_RANGE_DAYS = 120;

    private final EmployeeRepository employeeRepository;
    private final AttendanceLogRepository attendanceLogRepository;
    private final AttendanceProperties attendanceProperties;
    private final LeaveRequestRepository leaveRequestRepository;
    private final FaceRecognitionService faceRecognitionService;
    private final OvertimeApprovalService overtimeApprovalService;
    private final double faceRecognitionThreshold;

    public AttendanceService(
            EmployeeRepository employeeRepository,
            AttendanceLogRepository attendanceLogRepository,
            AttendanceProperties attendanceProperties,
            LeaveRequestRepository leaveRequestRepository,
            FaceRecognitionService faceRecognitionService,
            OvertimeApprovalService overtimeApprovalService,
            @Value("${app.face-recognition.threshold:0.82}") double faceRecognitionThreshold) {
        this.employeeRepository = employeeRepository;
        this.attendanceLogRepository = attendanceLogRepository;
        this.attendanceProperties = attendanceProperties;
        this.leaveRequestRepository = leaveRequestRepository;
        this.faceRecognitionService = faceRecognitionService;
        this.overtimeApprovalService = overtimeApprovalService;
        this.faceRecognitionThreshold = faceRecognitionThreshold;
    }

    @Transactional(readOnly = true)
    public NfcPreviewResponse previewNfc(String nfcUid, AuthPrincipal principal) {
        Employee e =
                employeeRepository
                        .findByNfcUid(nfcUid)
                        .orElseThrow(() -> new BusinessException("Không tìm thấy nhân viên với UID thẻ này"));
        if (!e.getId().equals(principal.employeeId())) {
            throw new BusinessException("Chỉ được quét thẻ NFC của chính mình");
        }
        if (e.getStatus() != EmployeeStatus.ACTIVE) {
            throw new BusinessException("Nhân viên không còn hoạt động");
        }
        boolean face = e.getFaceTemplate() != null && !e.getFaceTemplate().isBlank();
        return new NfcPreviewResponse(e.getId(), e.getEmployeeCode(), e.getFullName(), face, true);
    }

    /**
     * Xác định nhân viên và kiểm tra theo {@link VerifyMethod}: chỉ NFC, chỉ khuôn mặt (JWT), hoặc cả hai.
     */
    private Employee resolveEmployeeForCheckIn(CheckInRequest req, AuthPrincipal principal, VerifyMethod method) {
        return switch (method) {
            case FACE -> {
                Integer id = principal.employeeId();
                if (id == null) {
                    throw new BusinessException("Thiếu thông tin nhân viên trong phiên đăng nhập");
                }
                Employee e =
                        employeeRepository
                                .findById(id)
                                .orElseThrow(() -> new BusinessException("Không tìm thấy nhân viên"));
                String template = e.getFaceTemplate();
                if (template == null || template.isBlank()) {
                    throw new BusinessException("Nhân viên chưa đăng ký khuôn mặt");
                }
                if (req.faceEmbeddingJson() == null || req.faceEmbeddingJson().isBlank()) {
                    throw new BusinessException("Cần embedding khuôn mặt để xác thực");
                }
                if (!faceRecognitionService.matches(template, req.faceEmbeddingJson())) {
                    throw new BusinessException("Khuôn mặt không khớp với hồ sơ đã đăng ký");
                }
                yield e;
            }
            case NFC -> {
                String uid = req.nfcUid() != null ? req.nfcUid().trim() : "";
                if (uid.isEmpty()) {
                    throw new BusinessException("Cần UID thẻ NFC để chấm công");
                }
                Employee e =
                        employeeRepository
                                .findByNfcUid(uid)
                                .orElseThrow(() -> new BusinessException("Sai thẻ NFC"));
                if (!e.getId().equals(principal.employeeId())) {
                    throw new BusinessException("Chỉ được check-in cho chính mình");
                }
                yield e;
            }
            case NFC_FACE -> {
                String uid = req.nfcUid() != null ? req.nfcUid().trim() : "";
                if (uid.isEmpty()) {
                    throw new BusinessException("Cần UID thẻ NFC");
                }
                Employee e =
                        employeeRepository
                                .findByNfcUid(uid)
                                .orElseThrow(() -> new BusinessException("Sai thẻ NFC"));
                if (!e.getId().equals(principal.employeeId())) {
                    throw new BusinessException("Chỉ được check-in cho chính mình");
                }
                String template = e.getFaceTemplate();
                if (template == null || template.isBlank()) {
                    throw new BusinessException("Nhân viên chưa đăng ký khuôn mặt");
                }
                if (req.faceEmbeddingJson() == null || req.faceEmbeddingJson().isBlank()) {
                    throw new BusinessException("Cần embedding khuôn mặt để xác thực");
                }
                if (!faceRecognitionService.matches(template, req.faceEmbeddingJson())) {
                    throw new BusinessException("Khuôn mặt không khớp với chủ thẻ");
                }
                yield e;
            }
        };
    }

    /**
     * Nhận diện 1:N: so embedding với mọi mẫu trong DB (cùng số chiều), trả nhân viên có cosine cao nhất (≥ ngưỡng).
     */
    private Employee identifyEmployeeByFaceProbe1toN(String faceEmbeddingJson) {
        FaceEmbeddingUtils.validateEmbedding(faceEmbeddingJson, 0);
        float[] probe = FaceEmbeddingUtils.parse(faceEmbeddingJson);
        if (probe.length == 0) {
            throw new BusinessException("Embedding không hợp lệ");
        }

        Employee best = null;
        double bestSim = -2.0;
        int compared = 0;
        int skippedDim = 0;
        for (Employee e : employeeRepository.findAll()) {
            if (e.getStatus() != EmployeeStatus.ACTIVE) {
                continue;
            }
            String t = e.getFaceTemplate();
            if (t == null || t.isBlank()) {
                continue;
            }
            float[] stored = FaceEmbeddingUtils.parse(t);
            if (stored.length != probe.length) {
                skippedDim++;
                continue;
            }
            compared++;
            double sim = FaceEmbeddingUtils.cosineSimilarityVectors(stored, probe);
            if (sim > bestSim) {
                bestSim = sim;
                best = e;
            }
        }
        if (compared == 0) {
            throw new BusinessException(
                    "Không có mẫu khuôn mặt nào cùng số chiều với ảnh hiện tại ("
                            + probe.length
                            + "). Đăng ký mặt + model cùng chiều (vd. 512), hoặc dùng facenet_512 trên app.");
        }
        if (best == null || bestSim < faceRecognitionThreshold) {
            throw new BusinessException(
                    "Không nhận diện được nhân viên (điểm khớp tối đa "
                            + String.format("%.4f", bestSim)
                            + ", cần ≥ "
                            + faceRecognitionThreshold
                            + "; đã so "
                            + compared
                            + " mẫu, bỏ qua "
                            + skippedDim
                            + " mẫu khác số chiều).");
        }
        return best;
    }

    /**
     * Chấm công 1:N: không JWT — so cosine embedding với mọi {@link Employee#getFaceTemplate()} (cùng số chiều), chọn max ≥
     * ngưỡng.
     */
    @Transactional
    public AttendanceLogResponse checkInByFaceScan(AnonymousFaceCheckInRequest req) {
        if (!Boolean.TRUE.equals(req.livenessPassed())) {
            throw new BusinessException("Liveness không hợp lệ — từ chối chấm công");
        }
        Employee best = identifyEmployeeByFaceProbe1toN(req.faceEmbeddingJson());
        return persistCheckIn(best, req.locationGps(), VerifyMethod.FACE);
    }

    /**
     * Kiosk 1:N (không JWT): lần quét đầu trong ngày → vào ca; lần sau (đã vào, chưa ra) → ra ca.
     */
    @Transactional
    public AttendanceLogResponse kioskFaceToggle(AnonymousFaceCheckInRequest req) {
        if (!Boolean.TRUE.equals(req.livenessPassed())) {
            throw new BusinessException("Liveness không hợp lệ");
        }
        Employee best = identifyEmployeeByFaceProbe1toN(req.faceEmbeddingJson());
        Instant now = Instant.now();
        LocalDate today = LocalDate.ofInstant(now, ZONE);
        Optional<AttendanceLog> open =
                attendanceLogRepository.findFirstByEmployeeIdAndDateAndCheckOutIsNullOrderByCheckInDesc(
                        best.getId(), today);
        if (open.isEmpty()) {
            return persistCheckIn(best, req.locationGps(), VerifyMethod.FACE);
        }
        return persistCheckOut(best, req.locationGps());
    }

    /**
     * Kiosk NFC (không JWT): dùng UID thẻ để xác định nhân viên.
     * - Chưa có lượt mở trong ngày → check-in
     * - Đang có lượt mở → check-out
     */
    @Transactional
    public AttendanceLogResponse kioskNfcToggle(AnonymousNfcToggleRequest req) {
        String uid = req.nfcUid() != null ? req.nfcUid().trim() : "";
        if (uid.isEmpty()) {
            throw new BusinessException("Cần UID thẻ NFC");
        }
        Employee e =
                employeeRepository
                        .findByNfcUid(uid)
                        .orElseThrow(() -> new BusinessException("Không tìm thấy nhân viên với UID thẻ này"));
        if (e.getStatus() != EmployeeStatus.ACTIVE) {
            throw new BusinessException("Nhân viên không còn hoạt động");
        }
        Instant now = Instant.now();
        LocalDate today = LocalDate.ofInstant(now, ZONE);
        Optional<AttendanceLog> open =
                attendanceLogRepository.findFirstByEmployeeIdAndDateAndCheckOutIsNullOrderByCheckInDesc(
                        e.getId(), today);
        if (open.isEmpty()) {
            return persistCheckIn(e, req.locationGps(), VerifyMethod.NFC);
        }
        return persistCheckOut(e, req.locationGps());
    }

    @Transactional
    public AttendanceLogResponse checkIn(CheckInRequest req, AuthPrincipal principal) {
        if (!Boolean.TRUE.equals(req.livenessPassed())) {
            throw new BusinessException("Liveness không hợp lệ — từ chối chấm công");
        }
        VerifyMethod method = req.verifyMethod() != null ? req.verifyMethod() : VerifyMethod.NFC_FACE;

        Employee employee = resolveEmployeeForCheckIn(req, principal, method);
        return persistCheckIn(employee, req.locationGps(), method);
    }

    private AttendanceLogResponse persistCheckIn(
            Employee employee, String locationGps, VerifyMethod verifyMethod) {
        if (employee.getStatus() != EmployeeStatus.ACTIVE) {
            throw new BusinessException("Nhân viên không hoạt động");
        }
        ensureNotOnApprovedLeave(employee.getId());

        Instant now = Instant.now();
        LocalDate today = LocalDate.ofInstant(now, ZONE);
        LocalTime localTime = LocalTime.ofInstant(now, ZONE);

        attendanceLogRepository
                .findFirstByEmployeeIdAndDateAndCheckOutIsNullOrderByCheckInDesc(employee.getId(), today)
                .ifPresent(
                        log -> {
                            throw new BusinessException(
                                    "Đang trong ca — cần ra ca (check-out) trước khi vào lại");
                        });

        AttendanceLog log = new AttendanceLog();
        log.setEmployee(employee);
        log.setCheckIn(now);
        log.setDate(today);
        log.setVerifyMethod(verifyMethod);
        log.setLocation(locationGps);

        AttendanceStatus st = computeCheckInStatus(localTime, attendanceProperties);
        log.setStatus(st);

        return toResponse(attendanceLogRepository.save(log));
    }

    @Transactional
    public AttendanceLogResponse checkOut(CheckOutRequest req, AuthPrincipal principal) {
        VerifyMethod vm = req.verifyMethod() != null ? req.verifyMethod() : VerifyMethod.NFC;
        Employee employee;
        if (vm == VerifyMethod.FACE) {
            Integer id = principal.employeeId();
            if (id == null) {
                throw new BusinessException("Thiếu thông tin nhân viên trong phiên đăng nhập");
            }
            Employee e =
                    employeeRepository
                            .findById(id)
                            .orElseThrow(() -> new BusinessException("Không tìm thấy nhân viên"));
            String template = e.getFaceTemplate();
            if (template == null || template.isBlank()) {
                throw new BusinessException("Nhân viên chưa đăng ký khuôn mặt");
            }
            if (req.faceEmbeddingJson() == null || req.faceEmbeddingJson().isBlank()) {
                throw new BusinessException("Cần embedding khuôn mặt để ra ca");
            }
            if (!faceRecognitionService.matches(template, req.faceEmbeddingJson())) {
                throw new BusinessException("Khuôn mặt không khớp với hồ sơ đã đăng ký");
            }
            employee = e;
        } else {
            String uid = req.nfcUid() != null ? req.nfcUid().trim() : "";
            if (uid.isEmpty()) {
                throw new BusinessException("Cần UID thẻ NFC để ra ca (hoặc verifyMethod=FACE + embedding)");
            }
            employee =
                    employeeRepository
                            .findByNfcUid(uid)
                            .orElseThrow(() -> new BusinessException("Sai thẻ NFC"));
            if (!employee.getId().equals(principal.employeeId())) {
                throw new BusinessException("Chỉ được check-out cho chính mình");
            }
        }
        return persistCheckOut(employee, req.locationGps());
    }

    private AttendanceLogResponse persistCheckOut(Employee employee, String locationGps) {
        Instant now = Instant.now();
        LocalDate today = LocalDate.ofInstant(now, ZONE);

        AttendanceLog log =
                attendanceLogRepository
                        .findFirstByEmployeeIdAndDateAndCheckOutIsNullOrderByCheckInDesc(
                                employee.getId(), today)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                "Chưa có lượt vào ca đang mở — không thể ra ca (đã ra hết hoặc chưa vào)"));

        if (log.getCheckIn() == null) {
            throw new BusinessException("Bản ghi chấm công không hợp lệ");
        }

        log.setCheckOut(now);
        if (locationGps != null && !locationGps.isBlank()) {
            log.setLocation(locationGps);
        }
        AttendanceLog saved = attendanceLogRepository.save(log);
        // Nếu đã duyệt OT trước trong ngày, sau mỗi checkout sẽ chốt lại số giờ thực tế.
        overtimeApprovalService.finalizeApprovedHours(employee.getId(), today);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<AttendanceLogResponse> history(Integer employeeId, LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new BusinessException("Khoảng ngày không hợp lệ");
        }
        return attendanceLogRepository.findByEmployeeIdAndDateBetween(employeeId, from, to).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AttendanceLogResponse> historyByRoleScope(AuthPrincipal principal, LocalDate from, LocalDate to) {
        if (principal == null || principal.employeeId() == null) {
            throw new BusinessException("Thiếu thông tin xác thực");
        }
        if (from.isAfter(to)) {
            throw new BusinessException("Khoảng ngày không hợp lệ");
        }
        List<AttendanceLog> rows;
        if (principal.hasRole("ROLE_ADMIN")) {
            rows = attendanceLogRepository.findByDateBetween(from, to);
        } else if (principal.hasRole("ROLE_MANAGER")) {
            if (principal.departmentId() == null) {
                throw new BusinessException("Manager chưa gắn phòng ban");
            }
            rows = attendanceLogRepository.findByDateBetweenAndDepartmentId(from, to, principal.departmentId());
        } else {
            rows = attendanceLogRepository.findByEmployeeIdAndDateBetween(principal.employeeId(), from, to);
        }
        return rows.stream().map(this::toResponse).toList();
    }

    /**
     * Lịch theo từng ngày cho đúng nhân viên đang đăng nhập: có mặt / vắng / nghỉ (đã duyệt) / đơn chờ duyệt.
     */
    @Transactional(readOnly = true)
    public MyAttendanceCalendarResponse myAttendanceCalendar(AuthPrincipal principal, LocalDate from, LocalDate to) {
        if (principal == null || principal.employeeId() == null) {
            throw new BusinessException("Không xác định được nhân viên từ phiên đăng nhập");
        }
        if (from.isAfter(to)) {
            throw new BusinessException("Khoảng ngày không hợp lệ");
        }
        long days = ChronoUnit.DAYS.between(from, to) + 1;
        if (days > MAX_CALENDAR_RANGE_DAYS) {
            throw new BusinessException("Chỉ xem tối đa " + MAX_CALENDAR_RANGE_DAYS + " ngày mỗi lần");
        }
        Integer empId = principal.employeeId();
        List<AttendanceLog> logs = attendanceLogRepository.findByEmployeeIdAndDateBetween(empId, from, to);
        Map<LocalDate, List<AttendanceLog>> logsByDate =
                logs.stream().collect(Collectors.groupingBy(AttendanceLog::getDate));
        List<LeaveRequest> leaves = leaveRequestRepository.findByEmployeeIdOverlapping(empId, from, to);
        Instant now = Instant.now();

        List<MyAttendanceDayResponse> result = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            LeaveRequest approved = findLeaveCovering(leaves, d, LeaveRequestStatus.APPROVED);
            LeaveRequest pending = findLeaveCovering(leaves, d, LeaveRequestStatus.PENDING);
            List<AttendanceLog> dayLogs = sortedDayLogs(logsByDate.get(d));

            String dayStatus;
            String leaveTypeName = null;
            String leaveRequestStatus = null;

            boolean hasCheckIn = dayLogs.stream().anyMatch(l -> l.getCheckIn() != null);
            if (approved != null) {
                dayStatus = "ON_LEAVE";
                leaveTypeName = approved.getLeaveType().getName();
                leaveRequestStatus = LeaveRequestStatus.APPROVED.name();
            } else if (hasCheckIn) {
                dayStatus = "PRESENT";
            } else if (pending != null) {
                dayStatus = "LEAVE_PENDING";
                leaveTypeName = pending.getLeaveType().getName();
                leaveRequestStatus = LeaveRequestStatus.PENDING.name();
            } else {
                dayStatus = "ABSENT";
            }

            List<AttendanceSegmentDto> segments = buildSegments(dayLogs);
            DaySummary daySummary = summarizeDay(dayLogs, d, now);
            AttendanceStatus attendanceStatus = firstSegmentStatus(dayLogs);
            long targetSec = attendanceProperties.dailyTargetSeconds();
            long overtimeSec = Math.max(0, daySummary.totalPresentSeconds() - targetSec);

            result.add(
                    new MyAttendanceDayResponse(
                            d,
                            dayStatus,
                            leaveTypeName,
                            leaveRequestStatus,
                            daySummary.firstCheckIn(),
                            daySummary.summaryCheckOut(),
                            attendanceStatus,
                            segments,
                            daySummary.totalPresentSeconds(),
                            overtimeSec));
        }

        long present = 0;
        long absent = 0;
        long onLeave = 0;
        long leavePending = 0;
        for (MyAttendanceDayResponse row : result) {
            switch (row.dayStatus()) {
                case "PRESENT" -> present++;
                case "ABSENT" -> absent++;
                case "ON_LEAVE" -> onLeave++;
                case "LEAVE_PENDING" -> leavePending++;
                default -> {
                    /* không dùng */
                }
            }
        }
        long total = result.size();
        MyAttendanceCalendarSummary summary =
                new MyAttendanceCalendarSummary(total, present, absent, onLeave, leavePending);
        return new MyAttendanceCalendarResponse(result, summary);
    }

    /**
     * Lịch có mặt/vắng/nghỉ theo dạng bảng (roster) cho phạm vi quản lý.
     *
     * Mỗi nhân viên * mỗi ngày = 1 dòng, bao gồm cả ngày vắng (không có check-in).
     */
    @Transactional(readOnly = true)
    public List<AttendancePresenceRowResponse> scopePresenceCalendar(
            AuthPrincipal principal, LocalDate from, LocalDate to) {
        if (principal == null) {
            throw new BusinessException("Thiếu thông tin xác thực");
        }
        if (from.isAfter(to)) {
            throw new BusinessException("Khoảng ngày không hợp lệ");
        }
        long days = ChronoUnit.DAYS.between(from, to) + 1;
        if (days > MAX_CALENDAR_RANGE_DAYS) {
            throw new BusinessException("Chỉ xem tối đa " + MAX_CALENDAR_RANGE_DAYS + " ngày mỗi lần");
        }

        Instant now = Instant.now();
        long targetSeconds = attendanceProperties.dailyTargetSeconds();

        List<Employee> employees;
        List<AttendanceLog> logs;
        if (principal.hasRole("ROLE_ADMIN")) {
            employees = employeeRepository.findAll();
            logs = attendanceLogRepository.findByDateBetween(from, to);
        } else if (principal.hasRole("ROLE_MANAGER")) {
            if (principal.departmentId() == null) {
                throw new BusinessException("Manager chưa gắn phòng ban");
            }
            employees = employeeRepository.findByDepartmentIdWithDepartment(principal.departmentId());
            logs = attendanceLogRepository.findByDateBetweenAndDepartmentId(from, to, principal.departmentId());
        } else {
            Integer empId = principal.employeeId();
            if (empId == null) {
                throw new BusinessException("Thiếu thông tin nhân viên");
            }
            Employee emp = employeeRepository.findById(empId).orElseThrow(() -> new BusinessException("Không tìm thấy nhân viên"));
            employees = List.of(emp);
            logs = attendanceLogRepository.findByEmployeeIdAndDateBetween(empId, from, to);
        }

        Map<Integer, Map<LocalDate, List<AttendanceLog>>> logsByEmpDate =
                logs.stream()
                        .collect(
                                Collectors.groupingBy(
                                        l -> l.getEmployee().getId(),
                                        Collectors.groupingBy(AttendanceLog::getDate)));

        List<AttendancePresenceRowResponse> rows = new ArrayList<>();
        for (Employee emp : employees) {
            Integer empId = emp.getId();
            List<LeaveRequest> leaves =
                    leaveRequestRepository.findByEmployeeIdOverlapping(empId, from, to);

            for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
                List<AttendanceLog> dayLogsRaw =
                        logsByEmpDate
                                .getOrDefault(empId, Map.of())
                                .getOrDefault(d, List.of());
                List<AttendanceLog> dayLogs = sortedDayLogs(dayLogsRaw);

                boolean hasCheckIn = dayLogs.stream().anyMatch(l -> l.getCheckIn() != null);
                DaySummary summary = summarizeDay(dayLogs, d, now);
                long totalSeconds = summary.totalPresentSeconds();

                LeaveRequest approved = findLeaveCovering(leaves, d, LeaveRequestStatus.APPROVED);
                LeaveRequest pending = findLeaveCovering(leaves, d, LeaveRequestStatus.PENDING);

                String dayStatus;
                String note = "";
                if (hasCheckIn) {
                    dayStatus = "PRESENT";
                } else if (approved != null) {
                    dayStatus = "ON_LEAVE";
                    note = buildLeaveNote(approved);
                } else if (pending != null) {
                    dayStatus = "LEAVE_PENDING";
                    note = buildLeaveNote(pending);
                } else {
                    dayStatus = "ABSENT";
                }

                boolean shortPresent = hasCheckIn && totalSeconds < targetSeconds;

                rows.add(
                        new AttendancePresenceRowResponse(
                                empId,
                                emp.getEmployeeCode(),
                                emp.getFullName(),
                                d,
                                dayStatus,
                                totalSeconds,
                                shortPresent,
                                note));
            }
        }

        rows.sort(
                Comparator.comparing(AttendancePresenceRowResponse::date)
                        .thenComparing(AttendancePresenceRowResponse::employeeCode));
        return rows;
    }

    private record DaySummary(
            Instant firstCheckIn, Instant summaryCheckOut, long totalPresentSeconds) {}

    private static String buildLeaveNote(LeaveRequest r) {
        if (r == null || r.getLeaveType() == null) return "";
        String type = r.getLeaveType().getName() != null ? r.getLeaveType().getName() : "—";
        String status = r.getStatus() != null ? r.getStatus().name() : "—";
        String reason = r.getReason();
        if (reason != null && !reason.isBlank()) {
            return type + " (" + status + "): " + reason;
        }
        return type + " (" + status + ")";
    }

    private static List<AttendanceLog> sortedDayLogs(List<AttendanceLog> raw) {
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        List<AttendanceLog> copy = new ArrayList<>(raw);
        copy.sort(Comparator.comparing(AttendanceLog::getCheckIn, Comparator.nullsLast(Comparator.naturalOrder())));
        return copy;
    }

    private static List<AttendanceSegmentDto> buildSegments(List<AttendanceLog> dayLogs) {
        List<AttendanceSegmentDto> out = new ArrayList<>();
        for (AttendanceLog l : dayLogs) {
            if (l.getCheckIn() == null) {
                continue;
            }
            out.add(new AttendanceSegmentDto(l.getCheckIn(), l.getCheckOut()));
        }
        return List.copyOf(out);
    }

    private static AttendanceStatus firstSegmentStatus(List<AttendanceLog> dayLogs) {
        for (AttendanceLog l : dayLogs) {
            if (l.getCheckIn() != null && l.getStatus() != null) {
                return l.getStatus();
            }
        }
        return null;
    }

    private static DaySummary summarizeDay(List<AttendanceLog> dayLogs, LocalDate day, Instant now) {
        Instant firstIn = null;
        boolean hasOpen = false;
        Instant lastClosedOut = null;
        long totalSec = 0;
        for (AttendanceLog l : dayLogs) {
            Instant in = l.getCheckIn();
            if (in == null) {
                continue;
            }
            if (firstIn == null || in.isBefore(firstIn)) {
                firstIn = in;
            }
            Instant out = l.getCheckOut();
            if (out != null) {
                totalSec += Math.max(0, Duration.between(in, out).getSeconds());
                if (lastClosedOut == null || out.isAfter(lastClosedOut)) {
                    lastClosedOut = out;
                }
            } else {
                hasOpen = true;
                if (day.equals(LocalDate.ofInstant(now, ZONE))) {
                    totalSec += Math.max(0, Duration.between(in, now).getSeconds());
                }
            }
        }
        Instant summaryOut = hasOpen ? null : lastClosedOut;
        return new DaySummary(firstIn, summaryOut, totalSec);
    }

    private static LeaveRequest findLeaveCovering(
            List<LeaveRequest> leaves, LocalDate day, LeaveRequestStatus status) {
        return leaves.stream()
                .filter(r -> r.getStatus() == status)
                .filter(r -> !day.isBefore(r.getStartDate()) && !day.isAfter(r.getEndDate()))
                .findFirst()
                .orElse(null);
    }

    private void ensureNotOnApprovedLeave(Integer employeeId) {
        LocalDate today = LocalDate.now(ZONE);
        boolean onLeave =
                leaveRequestRepository.findApprovedCoveringDate(LeaveRequestStatus.APPROVED, today).stream()
                        .anyMatch(r -> r.getEmployee().getId().equals(employeeId));
        if (onLeave) {
            throw new BusinessException("Hôm nay đang trong đơn nghỉ phép đã duyệt — không chấm công tại công ty");
        }
    }

    private static AttendanceStatus computeCheckInStatus(LocalTime checkIn, AttendanceProperties policy) {
        LocalTime deadline = policy.getExpectedStart().plusMinutes(policy.getLateGraceMinutes());
        return checkIn.isAfter(deadline) ? AttendanceStatus.LATE : AttendanceStatus.ON_TIME;
    }

    private AttendanceLogResponse toResponse(AttendanceLog log) {
        Employee e = log.getEmployee();
        return new AttendanceLogResponse(
                log.getId(),
                e.getId(),
                e.getFullName(),
                e.getEmployeeCode(),
                log.getCheckIn(),
                log.getCheckOut(),
                log.getDate(),
                log.getVerifyMethod(),
                log.getLocation(),
                log.getStatus());
    }
}
