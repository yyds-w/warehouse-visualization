package com.warehouse.controller;

import com.warehouse.common.Result;
import com.warehouse.service.DashboardService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@Validated
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/warehouse/{id}")
    public Result<Map<String, Object>> warehouseLevel(@PathVariable Long id) {
        return Result.ok(dashboardService.warehouseLevel(id));
    }

    @GetMapping("/sub/{id}")
    public Result<Map<String, Object>> subLevel(@PathVariable Long id) {
        return Result.ok(dashboardService.subLevel(id));
    }
}
