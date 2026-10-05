package com.example.demo.dto;

import com.example.demo.entity.Role;
import jakarta.validation.constraints.NotNull;

public class RoleUpdateRequest {

    @NotNull
    private Role role;

    public RoleUpdateRequest() {
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }
}