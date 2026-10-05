package com.example.demo.dto;

import com.example.demo.entity.Role;

public record DashboardResponse(String username, Role role, String message) {
}