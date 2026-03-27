package com.rs.doanmonhoc.controller;

import com.rs.doanmonhoc.dto.LeaveTypeRequest;
import com.rs.doanmonhoc.dto.LeaveTypeResponse;
import com.rs.doanmonhoc.service.LeaveTypeService;
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
@RequestMapping("/api/leave-types")
public class LeaveTypeController {

    private final LeaveTypeService leaveTypeService;

    public LeaveTypeController(LeaveTypeService leaveTypeService) {
        this.leaveTypeService = leaveTypeService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<LeaveTypeResponse> list() {
        return leaveTypeService.listAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public LeaveTypeResponse get(@PathVariable Integer id) {
        return leaveTypeService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public LeaveTypeResponse create(@Valid @RequestBody LeaveTypeRequest req) {
        return leaveTypeService.create(req);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public LeaveTypeResponse update(@PathVariable Integer id, @Valid @RequestBody LeaveTypeRequest req) {
        return leaveTypeService.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public void delete(@PathVariable Integer id) {
        leaveTypeService.delete(id);
    }
}
