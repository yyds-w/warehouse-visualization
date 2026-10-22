package com.warehouse.controller;

import com.warehouse.common.PageResult;
import com.warehouse.common.Result;
import com.warehouse.entity.SubWarehouse;
import com.warehouse.service.SubWarehouseService;
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
@RequestMapping("/api/sub-warehouse")
@Validated
public class SubWarehouseController {

    private final SubWarehouseService subWarehouseService;

    public SubWarehouseController(SubWarehouseService subWarehouseService) {
        this.subWarehouseService = subWarehouseService;
    }

    @GetMapping("/page")
    public Result<PageResult<SubWarehouse>> page(@RequestParam(required = false) Long warehouseId,
                                                 @RequestParam(required = false) String keyword,
                                                 @RequestParam(defaultValue = "1") long pageNum,
                                                 @RequestParam(defaultValue = "10") long pageSize) {
        return Result.ok(subWarehouseService.page(warehouseId, keyword, pageNum, pageSize));
    }

    @GetMapping("/by-warehouse/{warehouseId}")
    public Result<List<SubWarehouse>> byWarehouse(@PathVariable Long warehouseId) {
        return Result.ok(subWarehouseService.listByWarehouse(warehouseId));
    }

    @GetMapping("/{id}")
    public Result<SubWarehouse> detail(@PathVariable Long id) {
        return Result.ok(subWarehouseService.getById(id));
    }

    @PostMapping
    public Result<SubWarehouse> create(@RequestBody @Valid SubWarehouse subWarehouse) {
        return Result.ok("新增成功", subWarehouseService.create(subWarehouse));
    }

    @PutMapping
    public Result<Void> update(@RequestBody @Valid SubWarehouse subWarehouse) {
        subWarehouseService.update(subWarehouse);
        return Result.ok("修改成功", null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        subWarehouseService.delete(id);
        return Result.ok("删除成功", null);
    }
}
