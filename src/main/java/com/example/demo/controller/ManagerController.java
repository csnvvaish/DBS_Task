package com.example.demo.controller;

import com.example.demo.dto.DashboardResponse;
import com.example.demo.security.AuthenticatedUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ManagerController {

    @GetMapping("/api/manager/dashboard")
    public DashboardResponse managerDashboard(@AuthenticationPrincipal AuthenticatedUser user) {
        return new DashboardResponse(user.getUsername(), user.getRole(), "Manager dashboard");
    }
}