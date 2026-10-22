package com.warehouse.controller;

import com.warehouse.common.PageResult;
import com.warehouse.common.Result;
import com.warehouse.dto.InboundRequest;
import com.warehouse.entity.Inventory;
import com.warehouse.service.InventoryService;
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

import java.util.Map;

@RestController
@RequestMapping("/api/inventory")
@Validated
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/page")
    public Result<PageResult<Map<String, Object>>> page(@RequestParam(required = false) String keyword,
                                                        @RequestParam(defaultValue = "1") long pageNum,
                                                        @RequestParam(defaultValue = "10") long pageSize) {
        return Result.ok(inventoryService.page(keyword, pageNum, pageSize));
    }

    @PostMapping("/inbound")
    public Result<Inventory> inbound(@RequestBody @Valid InboundRequest req) {
        return Result.ok("入库成功", inventoryService.inbound(req));
    }

    @PutMapping("/{id}")
    public Result<Void> adjust(@PathVariable Long id, @RequestParam Integer quantity) {
        inventoryService.adjust(id, quantity);
        return Result.ok("调整成功", null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        inventoryService.delete(id);
        return Result.ok("删除成功", null);
    }
}
