package com.rs.doanmonhoc.repository;

import com.rs.doanmonhoc.model.AttendanceLog;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AttendanceLogRepository extends JpaRepository<AttendanceLog, Long> {

    /**
     * Lượt đang mở trong ngày (đã vào, chưa ra). Chỉ nên có tối đa một bản ghi thỏa điều kiện.
     */
    Optional<AttendanceLog> findFirstByEmployeeIdAndDateAndCheckOutIsNullOrderByCheckInDesc(
            Integer employeeId, LocalDate date);

    List<AttendanceLog> findByDate(LocalDate date);

    @Query("SELECT a FROM AttendanceLog a JOIN FETCH a.employee e WHERE a.date = :date")
    List<AttendanceLog> findByDateWithEmployee(@Param("date") LocalDate date);

    @Query(
            "SELECT a FROM AttendanceLog a JOIN FETCH a.employee e WHERE e.id = :employeeId "
                    + "AND a.date BETWEEN :from AND :to ORDER BY a.date ASC, a.checkIn ASC")
    List<AttendanceLog> findByEmployeeIdAndDateBetween(
            @Param("employeeId") Integer employeeId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query(
            "SELECT a FROM AttendanceLog a JOIN FETCH a.employee WHERE a.date BETWEEN :from AND :to "
                    + "ORDER BY a.date, a.employee.id")
    List<AttendanceLog> findByDateBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query(
            "SELECT a FROM AttendanceLog a JOIN FETCH a.employee e JOIN e.department d "
                    + "WHERE a.date BETWEEN :from AND :to AND d.id = :departmentId "
                    + "ORDER BY a.date, a.employee.id")
    List<AttendanceLog> findByDateBetweenAndDepartmentId(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("departmentId") Integer departmentId);
}
