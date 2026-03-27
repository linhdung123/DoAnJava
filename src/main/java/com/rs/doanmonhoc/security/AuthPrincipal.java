package com.rs.doanmonhoc.security;

import java.io.Serializable;
import java.util.Set;

public record AuthPrincipal(
        Integer employeeId,
        Integer departmentId,
        Set<String> roles
) implements Serializable {

    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }
}
