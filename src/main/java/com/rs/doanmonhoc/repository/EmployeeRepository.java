package com.rs.doanmonhoc.repository;

import com.rs.doanmonhoc.model.Employee;
import com.rs.doanmonhoc.model.EmployeeStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmployeeRepository extends JpaRepository<Employee, Integer> {

    Optional<Employee> findByEmployeeCode(String employeeCode);

    Optional<Employee> findByNfcUid(String nfcUid);

    List<Employee> findByStatus(EmployeeStatus status);

    @Query(
            "SELECT e.employeeCode FROM Employee e WHERE e.employeeCode LIKE CONCAT(:prefix, '%') "
                    + "ORDER BY e.employeeCode DESC")
    List<String> findCodesByPrefixDesc(@Param("prefix") String prefix);

    @Query("SELECT e FROM Employee e JOIN FETCH e.department d WHERE d.id = :departmentId ORDER BY e.id")
    List<Employee> findByDepartmentIdWithDepartment(@Param("departmentId") Integer departmentId);

    @Query("SELECT e FROM Employee e JOIN FETCH e.department d WHERE e.id = :id AND d.id = :departmentId")
    Optional<Employee> findByIdAndDepartmentIdWithDepartment(
            @Param("id") Integer id, @Param("departmentId") Integer departmentId);

    @Query("SELECT e FROM Employee e JOIN FETCH e.department WHERE e.id = :id")
    Optional<Employee> findByIdWithDepartment(@Param("id") Integer id);
}
