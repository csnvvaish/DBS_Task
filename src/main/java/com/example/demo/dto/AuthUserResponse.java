package com.example.demo.dto;

import com.example.demo.entity.Role;

public record AuthUserResponse(Long id, String username, String email, Role role) {
}