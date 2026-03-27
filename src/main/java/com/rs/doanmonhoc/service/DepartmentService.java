package com.rs.doanmonhoc.service;

import com.rs.doanmonhoc.model.Department;
import com.rs.doanmonhoc.dto.DepartmentRequest;
import com.rs.doanmonhoc.dto.DepartmentResponse;
import com.rs.doanmonhoc.exception.BusinessException;
import com.rs.doanmonhoc.repository.DepartmentRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    public DepartmentService(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    @Transactional(readOnly = true)
    public List<DepartmentResponse> listAll() {
        return departmentRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public DepartmentResponse get(Integer id) {
        return departmentRepository.findById(id).map(this::toResponse).orElseThrow(() -> notFound(id));
    }

    @Transactional
    public DepartmentResponse create(DepartmentRequest req) {
        Department d = new Department();
        d.setName(req.name());
        d.setDescription(req.description());
        return toResponse(departmentRepository.save(d));
    }

    @Transactional
    public DepartmentResponse update(Integer id, DepartmentRequest req) {
        Department d = departmentRepository.findById(id).orElseThrow(() -> notFound(id));
        d.setName(req.name());
        d.setDescription(req.description());
        return toResponse(departmentRepository.save(d));
    }

    @Transactional
    public void delete(Integer id) {
        if (!departmentRepository.existsById(id)) {
            throw notFound(id);
        }
        departmentRepository.deleteById(id);
    }

    private DepartmentResponse toResponse(Department d) {
        return new DepartmentResponse(d.getId(), d.getName(), d.getDescription());
    }

    private static BusinessException notFound(Integer id) {
        return new BusinessException("Không tìm thấy phòng ban: " + id);
    }
}
