package com.warehouse.controller;

import com.warehouse.common.Result;
import com.warehouse.dto.PlanSaveRequest;
import com.warehouse.entity.WarehousePlan;
import com.warehouse.service.PlanService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/plan")
public class PlanController {

    private final PlanService planService;

    public PlanController(PlanService planService) {
        this.planService = planService;
    }

    @GetMapping
    public Result<WarehousePlan> getPlan() {
        return Result.ok(planService.getPlan());
    }

    @PostMapping("/save")
    public Result<PlanService.SaveResult> save(@RequestBody PlanSaveRequest request) {
        return Result.ok("保存成功", planService.savePlan(request));
    }

    @PostMapping("/import")
    public Result<WarehousePlan> importBackground(@RequestParam("file") MultipartFile file) {
        return Result.ok("导入成功", planService.importBackground(file));
    }
}
