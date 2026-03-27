package com.rs.doanmonhoc.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "leave_types")
public class LeaveType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(name = "max_days")
    private Integer maxDaysPerYear;

    @Column(name = "is_paid", nullable = false)
    private boolean paid = true;

    @Column(name = "pay_percentage", nullable = false)
    private Double payPercentage = 1.0;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getMaxDaysPerYear() {
        return maxDaysPerYear;
    }

    public void setMaxDaysPerYear(Integer maxDaysPerYear) {
        this.maxDaysPerYear = maxDaysPerYear;
    }

    public boolean isPaid() {
        return paid;
    }

    public void setPaid(boolean paid) {
        this.paid = paid;
    }

    public Double getPayPercentage() {
        return payPercentage;
    }

    public void setPayPercentage(Double payPercentage) {
        this.payPercentage = payPercentage;
    }
}
