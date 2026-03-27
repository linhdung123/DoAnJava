package com.rs.doanmonhoc.repository;

import com.rs.doanmonhoc.model.OvertimeLog;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OvertimeLogRepository extends JpaRepository<OvertimeLog, Long> {

    @Query(
            "SELECT o FROM OvertimeLog o "
                    + "JOIN FETCH o.employee e "
                    + "LEFT JOIN FETCH e.department d "
                    + "LEFT JOIN FETCH o.approvedBy ab "
                    + "WHERE o.employee.id = :employeeId AND o.date BETWEEN :from AND :to "
                    + "ORDER BY o.date, e.employeeCode")
    List<OvertimeLog> findByEmployeeIdAndDateBetween(
            @Param("employeeId") Integer employeeId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    @Query(
            "SELECT o FROM OvertimeLog o "
                    + "JOIN FETCH o.employee e "
                    + "LEFT JOIN FETCH e.department d "
                    + "LEFT JOIN FETCH o.approvedBy ab "
                    + "WHERE o.date BETWEEN :from AND :to "
                    + "ORDER BY o.date, e.employeeCode")
    List<OvertimeLog> findByDateBetweenWithEmployeeAndApprovedBy(
            @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query(
            "SELECT o FROM OvertimeLog o "
                    + "JOIN FETCH o.employee e "
                    + "LEFT JOIN FETCH e.department d "
                    + "LEFT JOIN FETCH o.approvedBy ab "
                    + "WHERE o.date BETWEEN :from AND :to "
                    + "AND d.id = :departmentId "
                    + "ORDER BY o.date, e.employeeCode")
    List<OvertimeLog> findByDateBetweenAndDepartmentIdWithEmployeeAndApprovedBy(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("departmentId") Integer departmentId);
}
