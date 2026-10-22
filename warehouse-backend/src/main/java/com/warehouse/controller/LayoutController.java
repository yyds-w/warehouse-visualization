package com.warehouse.controller;

import com.warehouse.common.Result;
import com.warehouse.entity.WarehouseLayout;
import com.warehouse.service.LayoutService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/layout")
@Validated
public class LayoutController {

    private final LayoutService layoutService;

    public LayoutController(LayoutService layoutService) {
        this.layoutService = layoutService;
    }

    @GetMapping("/by-warehouse/{warehouseId}")
    public Result<WarehouseLayout> byWarehouse(@PathVariable Long warehouseId) {
        return Result.ok(layoutService.getByWarehouse(warehouseId));
    }

    @PostMapping("/save")
    public Result<WarehouseLayout> save(@RequestBody WarehouseLayout layout) {
        return Result.ok("保存成功", layoutService.save(layout));
    }

    @PostMapping("/import")
    public Result<WarehouseLayout> importLayout(@RequestParam Long warehouseId,
                                                @RequestParam(required = false) String name,
                                                @RequestParam(required = false) Integer sourceType,
                                                @RequestParam("file") MultipartFile file) {
        return Result.ok("导入成功", layoutService.importLayout(warehouseId, name, sourceType, file));
    }
}
