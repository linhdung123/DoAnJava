package com.rs.doanmonhoc.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "employees",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_employee_code", columnNames = "employee_code"),
                @UniqueConstraint(name = "uk_nfc_uid", columnNames = "nfc_uid")
        }
)
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "employee_code", nullable = false, length = 20)
    private String employeeCode;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(length = 100)
    private String email;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @Column(name = "nfc_uid", length = 50)
    private String nfcUid;

    @Column(name = "face_template", columnDefinition = "TEXT")
    private String faceTemplate;

    @Column(name = "face_image_pending", columnDefinition = "LONGTEXT")
    private String faceImagePending;

    @Enumerated(EnumType.STRING)
    @Column(name = "face_approval_status", nullable = false, length = 20)
    private FaceApprovalStatus faceApprovalStatus = FaceApprovalStatus.NONE;

    @Column(name = "face_reject_reason", length = 255)
    private String faceRejectReason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EmployeeStatus status = EmployeeStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EmployeeRole role = EmployeeRole.ROLE_EMPLOYEE;

    @Column(name = "password_hash", length = 100)
    private String passwordHash;

    @Column(name = "must_change_password", nullable = false)
    private boolean mustChangePassword = false;

    @Column(name = "base_salary", nullable = false)
    private Double baseSalary = 0.0;

    @Column(name = "allowance", nullable = false)
    private Double allowance = 0.0;

    @Column(name = "standard_work_days", nullable = false)
    private Integer standardWorkDays = 26;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getEmployeeCode() {
        return employeeCode;
    }

    public void setEmployeeCode(String employeeCode) {
        this.employeeCode = employeeCode;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public String getNfcUid() {
        return nfcUid;
    }

    public void setNfcUid(String nfcUid) {
        this.nfcUid = nfcUid;
    }

    public String getFaceTemplate() {
        return faceTemplate;
    }

    public void setFaceTemplate(String faceTemplate) {
        this.faceTemplate = faceTemplate;
    }

    public String getFaceImagePending() {
        return faceImagePending;
    }

    public void setFaceImagePending(String faceImagePending) {
        this.faceImagePending = faceImagePending;
    }

    public FaceApprovalStatus getFaceApprovalStatus() {
        return faceApprovalStatus;
    }

    public void setFaceApprovalStatus(FaceApprovalStatus faceApprovalStatus) {
        this.faceApprovalStatus = faceApprovalStatus;
    }

    public String getFaceRejectReason() {
        return faceRejectReason;
    }

    public void setFaceRejectReason(String faceRejectReason) {
        this.faceRejectReason = faceRejectReason;
    }

    public EmployeeStatus getStatus() {
        return status;
    }

    public void setStatus(EmployeeStatus status) {
        this.status = status;
    }

    public EmployeeRole getRole() {
        return role;
    }

    public void setRole(EmployeeRole role) {
        this.role = role;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public boolean isMustChangePassword() {
        return mustChangePassword;
    }

    public void setMustChangePassword(boolean mustChangePassword) {
        this.mustChangePassword = mustChangePassword;
    }

    public Double getBaseSalary() {
        return baseSalary;
    }

    public void setBaseSalary(Double baseSalary) {
        this.baseSalary = baseSalary;
    }

    public Double getAllowance() {
        return allowance;
    }

    public void setAllowance(Double allowance) {
        this.allowance = allowance;
    }

    public Integer getStandardWorkDays() {
        return standardWorkDays;
    }

    public void setStandardWorkDays(Integer standardWorkDays) {
        this.standardWorkDays = standardWorkDays;
    }
}
