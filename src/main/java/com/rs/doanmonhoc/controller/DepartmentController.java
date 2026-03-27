package com.rs.doanmonhoc.controller;

import com.rs.doanmonhoc.dto.DepartmentRequest;
import com.rs.doanmonhoc.dto.DepartmentResponse;
import com.rs.doanmonhoc.service.DepartmentService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/departments")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class DepartmentController {

    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @GetMapping
    public List<DepartmentResponse> list() {
        return departmentService.listAll();
    }

    @GetMapping("/{id}")
    public DepartmentResponse get(@PathVariable Integer id) {
        return departmentService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DepartmentResponse create(@Valid @RequestBody DepartmentRequest req) {
        return departmentService.create(req);
    }

    @PutMapping("/{id}")
    public DepartmentResponse update(@PathVariable Integer id, @Valid @RequestBody DepartmentRequest req) {
        return departmentService.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        departmentService.delete(id);
    }
}
