package com.arqly.backend.controller;

import com.arqly.backend.dto.ApiResponse;
import com.arqly.backend.dto.DashboardDtos.AdminDashboard;
import com.arqly.backend.dto.DashboardDtos.TenantDashboard;
import com.arqly.backend.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DashboardController {
    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/api/platform/dashboard")
    public ApiResponse<AdminDashboard> admin() {
        return ApiResponse.ok(dashboardService.admin());
    }

    @GetMapping("/api/tenant/dashboard")
    public ApiResponse<TenantDashboard> tenant() {
        return ApiResponse.ok(dashboardService.tenant());
    }
}
