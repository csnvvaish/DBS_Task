package com.example.demo.controller;

import com.example.demo.dto.RoleUpdateRequest;
import com.example.demo.dto.AdminUserResponse;
import com.example.demo.dto.DashboardResponse;
import com.example.demo.dto.AuthUserResponse;
import com.example.demo.security.AuthenticatedUser;
import com.example.demo.service.UserAccountService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class AdminController {

    private final UserAccountService userAccountService;

    public AdminController(UserAccountService userAccountService) {
        this.userAccountService = userAccountService;
    }

    @GetMapping("/api/admin/dashboard")
    public DashboardResponse adminDashboard(@AuthenticationPrincipal AuthenticatedUser user) {
        return new DashboardResponse(user.getUsername(), user.getRole(), "Admin dashboard");
    }

    @GetMapping("/api/admin/users")
    public List<AdminUserResponse> adminUsers() {
        return userAccountService.getAllUsers();
    }

    @PutMapping("/api/admin/users/{id}/role")
    public AuthUserResponse updateUserRole(
            @PathVariable Long id,
            @Valid @RequestBody RoleUpdateRequest request) {
        return userAccountService.updateRole(id, request.getRole());
    }
}