package com.rs.doanmonhoc.repository;

import com.rs.doanmonhoc.model.Department;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartmentRepository extends JpaRepository<Department, Integer> {
}
