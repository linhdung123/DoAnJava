package com.rs.doanmonhoc.service;

import com.rs.doanmonhoc.model.Department;
import com.rs.doanmonhoc.model.Employee;
import com.rs.doanmonhoc.model.FaceApprovalStatus;
import com.rs.doanmonhoc.model.EmployeeRole;
import com.rs.doanmonhoc.model.EmployeeStatus;
import com.rs.doanmonhoc.dto.FaceEnrollmentPendingItemResponse;
import com.rs.doanmonhoc.dto.FaceEnrollmentReviewRequest;
import com.rs.doanmonhoc.dto.FaceEnrollmentStatusResponse;
import com.rs.doanmonhoc.dto.FaceEnrollmentSubmitRequest;
import com.rs.doanmonhoc.dto.EmployeeRequest;
import com.rs.doanmonhoc.dto.EmployeeResponse;
import com.rs.doanmonhoc.dto.FaceRegisterRequest;
import com.rs.doanmonhoc.dto.FaceStatusResponse;
import com.rs.doanmonhoc.dto.NfcLinkRequest;
import com.rs.doanmonhoc.exception.BusinessException;
import com.rs.doanmonhoc.repository.DepartmentRepository;
import com.rs.doanmonhoc.repository.EmployeeRepository;
import com.rs.doanmonhoc.security.AuthPrincipal;
import com.rs.doanmonhoc.service.face.FaceEmbeddingExtractor;
import com.rs.doanmonhoc.util.FaceEmbeddingUtils;
import java.text.Normalizer;
import java.util.List;
import java.security.SecureRandom;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmployeeService {

    private static final String PREFIX_MANAGER = "QL";
    private static final String PREFIX_EMPLOYEE = "NV";
    private static final String RESET_PASSWORD_CHARS =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789@#$%";
    private static final int RESET_PASSWORD_LENGTH = 10;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectProvider<FaceEmbeddingExtractor> faceEmbeddingExtractorProvider;

    public EmployeeService(
            EmployeeRepository employeeRepository,
            DepartmentRepository departmentRepository,
            PasswordEncoder passwordEncoder,
            ObjectProvider<FaceEmbeddingExtractor> faceEmbeddingExtractorProvider) {
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
        this.passwordEncoder = passwordEncoder;
        this.faceEmbeddingExtractorProvider = faceEmbeddingExtractorProvider;
    }

    @Transactional(readOnly = true)
    public List<EmployeeResponse> listAll() {
        return employeeRepository.findAll().stream().map(e -> toResponse(e, null)).toList();
    }

    @Transactional(readOnly = true)
    public EmployeeResponse get(Integer id) {
        Employee e =
                employeeRepository.findByIdWithDepartment(id).orElseThrow(() -> notFound(id));
        return toResponse(e, null);
    }

    @Transactional(readOnly = true)
    public List<EmployeeResponse> listManagedMembers(AuthPrincipal actor) {
        ensureManager(actor);
        Integer departmentId = actor.departmentId();
        return employeeRepository.findByDepartmentIdWithDepartment(departmentId).stream()
                .map(e -> toResponse(e, null))
                .toList();
    }

    @Transactional(readOnly = true)
    public EmployeeResponse getManagedMember(AuthPrincipal actor, Integer id) {
        ensureManager(actor);
        Employee e =
                employeeRepository
                        .findByIdAndDepartmentIdWithDepartment(id, actor.departmentId())
                        .orElseThrow(() -> new BusinessException("Nhân viên không thuộc phòng ban bạn quản lý"));
        return toResponse(e, null);
    }

    @Transactional
    public EmployeeResponse create(EmployeeRequest req) {
        EmployeeRole role = req.role() != null ? req.role() : EmployeeRole.ROLE_EMPLOYEE;
        Employee e = new Employee();
        String temporaryPassword = null;
        if (role == EmployeeRole.ROLE_MANAGER || role == EmployeeRole.ROLE_EMPLOYEE) {
            String prefix = role == EmployeeRole.ROLE_MANAGER ? PREFIX_MANAGER : PREFIX_EMPLOYEE;
            String generatedCode = generateNextEmployeeCode(prefix);
            temporaryPassword = buildInitialPassword(req.fullName(), generatedCode);
            e.setEmployeeCode(generatedCode);
            e.setPasswordHash(passwordEncoder.encode(temporaryPassword));
            e.setMustChangePassword(true);
        } else {
            String employeeCode = safeTrim(req.employeeCode());
            if (employeeCode == null) {
                throw new BusinessException("Cần nhập mã nhân viên cho tài khoản admin");
            }
            if (employeeRepository.findByEmployeeCode(employeeCode).isPresent()) {
                throw new BusinessException("Mã nhân viên đã tồn tại");
            }
            e.setEmployeeCode(employeeCode);
            if (req.password() != null && !req.password().isBlank()) {
                e.setPasswordHash(passwordEncoder.encode(req.password()));
                e.setMustChangePassword(false);
            } else {
                throw new BusinessException("Cần nhập mật khẩu cho tài khoản admin");
            }
        }
        e.setFullName(req.fullName());
        e.setEmail(req.email());
        e.setStatus(req.status() != null ? req.status() : EmployeeStatus.ACTIVE);
        e.setRole(role);
        if (req.departmentId() != null) {
            Department d = departmentRepository.findById(req.departmentId()).orElseThrow(this::departmentNotFound);
            e.setDepartment(d);
        }
        e.setBaseSalary(req.baseSalary() != null ? req.baseSalary() : 0.0);
        e.setAllowance(req.allowance() != null ? req.allowance() : 0.0);
        e.setStandardWorkDays(req.standardWorkDays() != null ? req.standardWorkDays() : 26);
        return toResponse(employeeRepository.save(e), temporaryPassword);
    }

    @Transactional
    public EmployeeResponse update(Integer id, EmployeeRequest req) {
        Employee e =
                employeeRepository.findByIdWithDepartment(id).orElseThrow(() -> notFound(id));
        String employeeCode = safeTrim(req.employeeCode());
        if (employeeCode == null) {
            throw new BusinessException("Mã nhân viên không được để trống");
        }
        employeeRepository
                .findByEmployeeCode(employeeCode)
                .filter(other -> !other.getId().equals(id))
                .ifPresent(x -> {
                    throw new BusinessException("Mã nhân viên đã tồn tại");
                });
        e.setEmployeeCode(employeeCode);
        e.setFullName(req.fullName());
        e.setEmail(req.email());
        if (req.status() != null) {
            e.setStatus(req.status());
        }
        if (req.role() != null) {
            e.setRole(req.role());
        }
        if (req.password() != null && !req.password().isBlank()) {
            e.setPasswordHash(passwordEncoder.encode(req.password()));
            e.setMustChangePassword(false);
        }
        if (req.departmentId() != null) {
            Department d = departmentRepository.findById(req.departmentId()).orElseThrow(this::departmentNotFound);
            e.setDepartment(d);
        } else {
            e.setDepartment(null);
        }
        if (req.baseSalary() != null) {
            e.setBaseSalary(req.baseSalary());
        }
        if (req.allowance() != null) {
            e.setAllowance(req.allowance());
        }
        if (req.standardWorkDays() != null) {
            e.setStandardWorkDays(req.standardWorkDays());
        }
        return toResponse(employeeRepository.save(e), null);
    }

    @Transactional
    public EmployeeResponse updateManagedMember(AuthPrincipal actor, Integer id, EmployeeRequest req) {
        ensureManager(actor);
        Employee e =
                employeeRepository
                        .findByIdAndDepartmentIdWithDepartment(id, actor.departmentId())
                        .orElseThrow(() -> new BusinessException("Nhân viên không thuộc phòng ban bạn quản lý"));
        if (e.getRole() == EmployeeRole.ROLE_ADMIN) {
            throw new BusinessException("Manager không được sửa tài khoản admin");
        }
        String employeeCode = safeTrim(req.employeeCode());
        if (employeeCode == null) {
            throw new BusinessException("Mã nhân viên không được để trống");
        }
        employeeRepository
                .findByEmployeeCode(employeeCode)
                .filter(other -> !other.getId().equals(id))
                .ifPresent(x -> {
                    throw new BusinessException("Mã nhân viên đã tồn tại");
                });
        e.setEmployeeCode(employeeCode);
        e.setFullName(req.fullName());
        e.setEmail(req.email());
        if (req.status() != null) {
            e.setStatus(req.status());
        }
        // Manager không được đổi role/department của nhân viên.
        if (req.password() != null && !req.password().isBlank()) {
            e.setPasswordHash(passwordEncoder.encode(req.password()));
            e.setMustChangePassword(false);
        }
        return toResponse(employeeRepository.save(e), null);
    }

    @Transactional
    public EmployeeResponse registerFace(Integer id, FaceRegisterRequest req) {
        FaceEmbeddingUtils.validateEmbedding(req.faceEmbeddingJson(), 0);
        Employee e = employeeRepository.findById(id).orElseThrow(() -> notFound(id));
        e.setFaceTemplate(req.faceEmbeddingJson());
        return toResponse(employeeRepository.save(e), null);
    }

    @Transactional
    public EmployeeResponse linkNfc(Integer id, NfcLinkRequest req) {
        String normalizedUid = normalizeNfcUid(req.nfcUid());
        if (normalizedUid == null) {
            throw new BusinessException("UID thẻ NFC không hợp lệ");
        }
        employeeRepository
                .findByNfcUid(normalizedUid)
                .filter(other -> !other.getId().equals(id))
                .ifPresent(x -> {
                    throw new BusinessException("Thẻ NFC đã gán cho nhân viên khác");
                });
        Employee e = employeeRepository.findById(id).orElseThrow(() -> notFound(id));
        e.setNfcUid(normalizedUid);
        return toResponse(employeeRepository.save(e), null);
    }

    @Transactional
    public EmployeeResponse reportMyLostNfc(AuthPrincipal principal) {
        if (principal == null || principal.employeeId() == null) {
            throw new BusinessException("Thiếu thông tin đăng nhập");
        }
        Employee e =
                employeeRepository
                        .findById(principal.employeeId())
                        .orElseThrow(() -> new BusinessException("Không tìm thấy nhân viên"));
        if (e.getNfcUid() == null || e.getNfcUid().isBlank()) {
            throw new BusinessException("Tài khoản hiện chưa liên kết thẻ NFC");
        }
        e.setNfcUid(null);
        return toResponse(employeeRepository.save(e), null);
    }

    @Transactional
    public EmployeeResponse reportLostNfcByAdmin(Integer employeeId) {
        Employee e = employeeRepository.findById(employeeId).orElseThrow(() -> notFound(employeeId));
        if (e.getNfcUid() == null || e.getNfcUid().isBlank()) {
            throw new BusinessException("Nhân viên hiện chưa liên kết thẻ NFC");
        }
        e.setNfcUid(null);
        return toResponse(employeeRepository.save(e), null);
    }

    @Transactional
    public void delete(Integer id) {
        if (!employeeRepository.existsById(id)) {
            throw notFound(id);
        }
        employeeRepository.deleteById(id);
    }

    /** Xóa face template của nhân viên (hủy đăng ký khuôn mặt). */
    @Transactional
    public void removeFace(Integer id) {
        Employee e = employeeRepository.findById(id).orElseThrow(() -> notFound(id));
        e.setFaceTemplate(null);
        employeeRepository.save(e);
    }

    @Transactional
    public FaceEnrollmentStatusResponse submitFaceEnrollment(
            AuthPrincipal principal, FaceEnrollmentSubmitRequest req) {
        if (principal == null || principal.employeeId() == null) {
            throw new BusinessException("Thiếu thông tin đăng nhập");
        }
        Employee e =
                employeeRepository
                        .findById(principal.employeeId())
                        .orElseThrow(() -> new BusinessException("Không tìm thấy nhân viên"));
        if (e.getRole() == EmployeeRole.ROLE_ADMIN) {
            throw new BusinessException("Admin không cần gửi ảnh duyệt khuôn mặt");
        }
        String imageBase64 = safeTrim(req.faceImageBase64());
        if (imageBase64 == null || imageBase64.length() < 100) {
            throw new BusinessException("Ảnh khuôn mặt không hợp lệ");
        }
        e.setFaceImagePending(imageBase64);
        e.setFaceApprovalStatus(FaceApprovalStatus.PENDING);
        e.setFaceRejectReason(null);
        employeeRepository.save(e);
        return toFaceEnrollmentStatus(e);
    }

    @Transactional(readOnly = true)
    public FaceEnrollmentStatusResponse myFaceEnrollmentStatus(AuthPrincipal principal) {
        if (principal == null || principal.employeeId() == null) {
            throw new BusinessException("Thiếu thông tin đăng nhập");
        }
        Employee e =
                employeeRepository
                        .findById(principal.employeeId())
                        .orElseThrow(() -> new BusinessException("Không tìm thấy nhân viên"));
        return toFaceEnrollmentStatus(e);
    }

    @Transactional(readOnly = true)
    public List<FaceEnrollmentPendingItemResponse> pendingFaceEnrollments() {
        return employeeRepository.findAll().stream()
                .filter(e -> e.getFaceApprovalStatus() == FaceApprovalStatus.PENDING)
                .map(
                        e ->
                                new FaceEnrollmentPendingItemResponse(
                                        e.getId(), e.getEmployeeCode(), e.getFullName(), e.getFaceImagePending()))
                .toList();
    }

    @Transactional
    public FaceEnrollmentStatusResponse reviewFaceEnrollment(
            Integer employeeId, FaceEnrollmentReviewRequest req) {
        Employee e = employeeRepository.findById(employeeId).orElseThrow(() -> notFound(employeeId));
        if (e.getFaceApprovalStatus() != FaceApprovalStatus.PENDING) {
            throw new BusinessException("Nhân viên này không ở trạng thái chờ duyệt ảnh khuôn mặt");
        }
        if (Boolean.TRUE.equals(req.approved())) {
            String embedding = safeTrim(req.faceEmbeddingJson());
            if (embedding == null) {
                String pendingImage = safeTrim(e.getFaceImagePending());
                if (pendingImage == null) {
                    throw new BusinessException("Không tìm thấy ảnh pending để sinh embedding tự động");
                }
                FaceEmbeddingExtractor extractor = faceEmbeddingExtractorProvider.getIfAvailable();
                if (extractor == null) {
                    throw new BusinessException(
                            "Chưa bật service sinh embedding (app.face-embedding.provider=http + URL /embed). "
                                    + "Hoặc trên trang Admin «Duyệt ảnh khuôn mặt», dán vector JSON vào ô «Embedding tùy chọn» rồi bấm Duyệt.");
                }
                embedding = extractor.extractEmbeddingJson(pendingImage);
            }
            FaceEmbeddingUtils.validateEmbedding(embedding, 0);
            e.setFaceTemplate(embedding);
            e.setFaceApprovalStatus(FaceApprovalStatus.APPROVED);
            e.setFaceRejectReason(null);
            e.setFaceImagePending(null);
        } else {
            e.setFaceApprovalStatus(FaceApprovalStatus.REJECTED);
            e.setFaceRejectReason(safeTrim(req.rejectReason()));
        }
        employeeRepository.save(e);
        return toFaceEnrollmentStatus(e);
    }

    /**
     * Manager reset mật khẩu cho nhân viên cùng phòng ban.
     * Trả về mật khẩu mới dạng plaintext để manager gửi lại cho nhân viên.
     */
    @Transactional
    public String resetEmployeePassword(AuthPrincipal actor, Integer employeeId) {
        if (actor == null || (!actor.hasRole("ROLE_MANAGER") && !actor.hasRole("MANAGER"))) {
            throw new BusinessException("Chỉ Manager mới có quyền reset mật khẩu nhân viên");
        }
        if (employeeId == null) {
            throw new BusinessException("Thiếu employeeId cần reset");
        }
        Integer managerDepartmentId = actor.departmentId();
        if (managerDepartmentId == null) {
            throw new BusinessException("Manager chưa được gắn phòng ban");
        }

        Employee target =
                employeeRepository.findByIdWithDepartment(employeeId).orElseThrow(() -> notFound(employeeId));
        Integer targetDepartmentId = target.getDepartment() != null ? target.getDepartment().getId() : null;
        if (!managerDepartmentId.equals(targetDepartmentId)) {
            throw new BusinessException("Chỉ được reset mật khẩu nhân viên trong cùng phòng ban");
        }

        String newPassword = generateRandomPassword();
        target.setPasswordHash(passwordEncoder.encode(newPassword));
        target.setMustChangePassword(true);
        employeeRepository.save(target);
        return newPassword;
    }

    /**
     * Admin reset mật khẩu cho bất kỳ nhân viên nào.
     * Trả về mật khẩu mới dạng plaintext để admin gửi lại cho người dùng.
     */
    @Transactional
    public String resetEmployeePasswordByAdmin(Integer employeeId) {
        if (employeeId == null) {
            throw new BusinessException("Thiếu employeeId cần reset");
        }
        Employee target =
                employeeRepository.findByIdWithDepartment(employeeId).orElseThrow(() -> notFound(employeeId));
        String newPassword = generateRandomPassword();
        target.setPasswordHash(passwordEncoder.encode(newPassword));
        target.setMustChangePassword(true);
        employeeRepository.save(target);
        return newPassword;
    }

    /** Kiểm tra trạng thái đăng ký khuôn mặt của nhân viên. */
    @Transactional(readOnly = true)
    public FaceStatusResponse getFaceStatus(Integer id) {
        Employee e = employeeRepository.findById(id).orElseThrow(() -> notFound(id));
        boolean registered = e.getFaceTemplate() != null && !e.getFaceTemplate().isBlank();
        return new FaceStatusResponse(e.getId(), e.getEmployeeCode(), e.getFullName(), registered);
    }

    private EmployeeResponse toResponse(Employee e, String temporaryPassword) {
        return new EmployeeResponse(
                e.getId(),
                e.getEmployeeCode(),
                e.getFullName(),
                e.getEmail(),
                e.getDepartment() != null ? e.getDepartment().getId() : null,
                e.getDepartment() != null ? e.getDepartment().getName() : null,
                e.getNfcUid(),
                e.getFaceTemplate() != null && !e.getFaceTemplate().isBlank(),
                e.getStatus(),
                e.getRole(),
                e.isMustChangePassword(),
                temporaryPassword,
                e.getBaseSalary(),
                e.getAllowance(),
                e.getStandardWorkDays());
    }

    private String generateNextEmployeeCode(String prefix) {
        List<String> codes = employeeRepository.findCodesByPrefixDesc(prefix);
        int max = 0;
        for (String code : codes) {
            if (code != null && code.startsWith(prefix) && code.length() > prefix.length()) {
                String numPart = code.substring(prefix.length());
                try {
                    int n = Integer.parseInt(numPart);
                    if (n > max) {
                        max = n;
                    }
                } catch (NumberFormatException ignored) {
                    // bỏ qua mã không theo format PREFIX + số
                }
            }
        }
        return prefix + String.format("%03d", max + 1);
    }

    private static String buildInitialPassword(String fullName, String code) {
        String nameKey = normalizeNameKey(fullName);
        return nameKey + "@" + code;
    }

    private static String normalizeNameKey(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "user";
        }
        String normalized =
                Normalizer.normalize(fullName, Normalizer.Form.NFD)
                        .replaceAll("\\p{M}", "")
                        .replace('đ', 'd')
                        .replace('Đ', 'D')
                        .toLowerCase()
                        .replaceAll("[^a-z0-9 ]", " ")
                        .trim()
                        .replaceAll("\\s+", " ");
        if (normalized.isBlank()) {
            return "user";
        }
        String[] parts = normalized.split(" ");
        return parts[parts.length - 1];
    }

    private static String safeTrim(String value) {
        if (value == null) {
            return null;
        }
        String out = value.trim();
        return out.isEmpty() ? null : out;
    }

    private static String normalizeNfcUid(String value) {
        String trimmed = safeTrim(value);
        if (trimmed == null) return null;
        return trimmed.replace(":", "").replace(" ", "").toUpperCase();
    }

    private static FaceEnrollmentStatusResponse toFaceEnrollmentStatus(Employee e) {
        boolean registered = e.getFaceTemplate() != null && !e.getFaceTemplate().isBlank();
        return new FaceEnrollmentStatusResponse(
                e.getId(),
                e.getEmployeeCode(),
                e.getFullName(),
                e.getFaceApprovalStatus(),
                e.getFaceRejectReason(),
                registered);
    }

    private static String generateRandomPassword() {
        StringBuilder sb = new StringBuilder(RESET_PASSWORD_LENGTH);
        for (int i = 0; i < RESET_PASSWORD_LENGTH; i++) {
            int idx = SECURE_RANDOM.nextInt(RESET_PASSWORD_CHARS.length());
            sb.append(RESET_PASSWORD_CHARS.charAt(idx));
        }
        return sb.toString();
    }

    private static void ensureManager(AuthPrincipal actor) {
        if (actor == null || (!actor.hasRole("ROLE_MANAGER") && !actor.hasRole("MANAGER"))) {
            throw new BusinessException("Chỉ Manager mới có quyền thao tác chức năng này");
        }
        if (actor.departmentId() == null) {
            throw new BusinessException("Manager chưa được gắn phòng ban");
        }
    }

    private static BusinessException notFound(Integer id) {
        return new BusinessException("Không tìm thấy nhân viên: " + id);
    }

    private BusinessException departmentNotFound() {
        return new BusinessException("Không tìm thấy phòng ban");
    }
}
