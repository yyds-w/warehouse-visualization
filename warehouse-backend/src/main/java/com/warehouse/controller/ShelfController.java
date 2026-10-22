package com.warehouse.controller;

import com.warehouse.common.PageResult;
import com.warehouse.common.Result;
import com.warehouse.dto.ShelfBatchRequest;
import com.warehouse.dto.ShelfSaveRequest;
import com.warehouse.entity.Shelf;
import com.warehouse.service.ShelfService;
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
import java.util.Map;

@RestController
@RequestMapping("/api/shelf")
@Validated
public class ShelfController {

    private final ShelfService shelfService;

    public ShelfController(ShelfService shelfService) {
        this.shelfService = shelfService;
    }

    @GetMapping("/page")
    public Result<PageResult<Shelf>> page(@RequestParam(required = false) Long warehouseId,
                                          @RequestParam(required = false) Long subWarehouseId,
                                          @RequestParam(required = false) String keyword,
                                          @RequestParam(defaultValue = "1") long pageNum,
                                          @RequestParam(defaultValue = "10") long pageSize) {
        return Result.ok(shelfService.page(warehouseId, subWarehouseId, keyword, pageNum, pageSize));
    }

    @GetMapping("/by-sub/{subWarehouseId}")
    public Result<List<Shelf>> bySub(@PathVariable Long subWarehouseId) {
        return Result.ok(shelfService.listBySub(subWarehouseId));
    }

    @GetMapping("/{id}")
    public Result<Shelf> detail(@PathVariable Long id) {
        return Result.ok(shelfService.getById(id));
    }

    @PostMapping("/batch")
    public Result<Map<String, Object>> batchCreate(@RequestBody @Valid ShelfBatchRequest req) {
        Map<String, Object> data = shelfService.batchCreate(req);
        return Result.ok("货架创建成功，已生成 " + data.get("locationCount") + " 个货位", data);
    }

    @PostMapping("/save-all")
    public Result<Map<String, Object>> saveAll(@RequestBody @Valid ShelfSaveRequest req) {
        Map<String, Object> data = shelfService.saveAll(req);
        String msg = String.format("保存成功：新增 %d 个货架，更新 %d 个，删除 %d 个",
                data.get("created"), data.get("updated"), data.get("removed"));
        return Result.ok(msg, data);
    }

    @PutMapping
    public Result<Void> update(@RequestBody @Valid Shelf shelf) {
        shelfService.update(shelf);
        return Result.ok("修改成功", null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        shelfService.delete(id);
        return Result.ok("删除成功", null);
    }
}
