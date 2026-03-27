package com.rs.doanmonhoc.service;

import com.rs.doanmonhoc.model.LeaveType;
import com.rs.doanmonhoc.dto.LeaveTypeRequest;
import com.rs.doanmonhoc.dto.LeaveTypeResponse;
import com.rs.doanmonhoc.exception.BusinessException;
import com.rs.doanmonhoc.repository.LeaveTypeRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LeaveTypeService {

    private final LeaveTypeRepository leaveTypeRepository;

    public LeaveTypeService(LeaveTypeRepository leaveTypeRepository) {
        this.leaveTypeRepository = leaveTypeRepository;
    }

    @Transactional(readOnly = true)
    public List<LeaveTypeResponse> listAll() {
        return leaveTypeRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public LeaveTypeResponse get(Integer id) {
        return leaveTypeRepository.findById(id).map(this::toResponse).orElseThrow(() -> notFound(id));
    }

    @Transactional
    public LeaveTypeResponse create(LeaveTypeRequest req) {
        LeaveType t = new LeaveType();
        t.setName(req.name());
        t.setMaxDaysPerYear(req.maxDaysPerYear());
        t.setPaid(req.paid() == null || req.paid());
        t.setPayPercentage(resolvePayPercentage(req.paid(), req.payPercentage()));
        return toResponse(leaveTypeRepository.save(t));
    }

    @Transactional
    public LeaveTypeResponse update(Integer id, LeaveTypeRequest req) {
        LeaveType t = leaveTypeRepository.findById(id).orElseThrow(() -> notFound(id));
        t.setName(req.name());
        t.setMaxDaysPerYear(req.maxDaysPerYear());
        t.setPaid(req.paid() == null || req.paid());
        t.setPayPercentage(resolvePayPercentage(req.paid(), req.payPercentage()));
        return toResponse(leaveTypeRepository.save(t));
    }

    @Transactional
    public void delete(Integer id) {
        if (!leaveTypeRepository.existsById(id)) {
            throw notFound(id);
        }
        leaveTypeRepository.deleteById(id);
    }

    private LeaveTypeResponse toResponse(LeaveType t) {
        return new LeaveTypeResponse(
                t.getId(),
                t.getName(),
                t.getMaxDaysPerYear(),
                t.isPaid(),
                t.getPayPercentage());
    }

    private static Double resolvePayPercentage(Boolean paid, Double percentage) {
        if (percentage != null) {
            if (percentage < 0 || percentage > 1.0) {
                throw new BusinessException("payPercentage phải nằm trong khoảng 0.0 đến 1.0");
            }
            return percentage;
        }
        return Boolean.FALSE.equals(paid) ? 0.0 : 1.0;
    }

    private static BusinessException notFound(Integer id) {
        return new BusinessException("Không tìm thấy loại nghỉ: " + id);
    }
}
