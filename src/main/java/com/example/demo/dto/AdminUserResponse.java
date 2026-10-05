package com.example.demo.dto;

import com.example.demo.entity.Role;

import java.time.LocalDateTime;

public record AdminUserResponse(Long id, String username, String email, Role role, LocalDateTime createdAt) {
}