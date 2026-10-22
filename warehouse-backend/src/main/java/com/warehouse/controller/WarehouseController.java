package com.warehouse.controller;

import com.warehouse.common.PageResult;
import com.warehouse.common.Result;
import com.warehouse.entity.Warehouse;
import com.warehouse.service.WarehouseService;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/warehouse")
@Validated
public class WarehouseController {

    private final WarehouseService warehouseService;

    public WarehouseController(WarehouseService warehouseService) {
        this.warehouseService = warehouseService;
    }

    @GetMapping("/page")
    public Result<PageResult<Warehouse>> page(@RequestParam(required = false) String keyword,
                                              @RequestParam(required = false) Integer status,
                                              @RequestParam(defaultValue = "1") long pageNum,
                                              @RequestParam(defaultValue = "10") long pageSize) {
        return Result.ok(warehouseService.page(keyword, status, pageNum, pageSize));
    }

    @GetMapping("/all")
    public Result<List<Warehouse>> all() {
        return Result.ok(warehouseService.listAll());
    }

    @GetMapping("/{id}")
    public Result<Warehouse> detail(@PathVariable Long id) {
        return Result.ok(warehouseService.getById(id));
    }

    @PostMapping
    public Result<Warehouse> create(@RequestBody @Valid Warehouse warehouse) {
        return Result.ok("新增成功", warehouseService.create(warehouse));
    }

    @PutMapping
    public Result<Void> update(@RequestBody @Valid Warehouse warehouse) {
        warehouseService.update(warehouse);
        return Result.ok("修改成功", null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        warehouseService.delete(id);
        return Result.ok("删除成功", null);
    }
}
