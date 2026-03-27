package com.rs.doanmonhoc.repository;

import com.rs.doanmonhoc.model.LeaveRequest;
import com.rs.doanmonhoc.model.LeaveRequestStatus;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Integer> {

    @Query(
            "SELECT r FROM LeaveRequest r JOIN FETCH r.employee JOIN FETCH r.leaveType "
                    + "WHERE r.status = :status ORDER BY r.createdAt DESC")
    List<LeaveRequest> findByStatusOrderByCreatedAtDesc(@Param("status") LeaveRequestStatus status);

    @Query(
            "SELECT r FROM LeaveRequest r JOIN FETCH r.employee e JOIN FETCH r.leaveType "
                    + "JOIN e.department d WHERE r.status = :status AND d.id = :departmentId "
                    + "ORDER BY r.createdAt DESC")
    List<LeaveRequest> findByStatusAndDepartmentIdOrderByCreatedAtDesc(
            @Param("status") LeaveRequestStatus status, @Param("departmentId") Integer departmentId);

    @Query(
            "SELECT r FROM LeaveRequest r JOIN FETCH r.employee JOIN FETCH r.leaveType "
                    + "WHERE r.employee.id = :employeeId ORDER BY r.createdAt DESC")
    List<LeaveRequest> findByEmployeeIdOrderByCreatedAtDesc(@Param("employeeId") Integer employeeId);

    @Query(
            "SELECT r FROM LeaveRequest r WHERE r.status = :status "
                    + "AND r.startDate <= :day AND r.endDate >= :day")
    List<LeaveRequest> findApprovedCoveringDate(
            @Param("status") LeaveRequestStatus status, @Param("day") LocalDate day);

    @Query(
            "SELECT r FROM LeaveRequest r JOIN FETCH r.leaveType WHERE r.employee.id = :employeeId "
                    + "AND r.startDate <= :to AND r.endDate >= :from ORDER BY r.startDate")
    List<LeaveRequest> findByEmployeeIdOverlapping(
            @Param("employeeId") Integer employeeId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);
}
