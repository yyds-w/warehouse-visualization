package com.warehouse.controller;

import com.warehouse.common.Result;
import com.warehouse.service.LocationService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/location")
@Validated
public class LocationController {

    private final LocationService locationService;

    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    @GetMapping("/by-shelf/{shelfId}")
    public Result<List<Map<String, Object>>> byShelf(@PathVariable Long shelfId) {
        return Result.ok(locationService.listByShelf(shelfId));
    }

    @GetMapping("/by-sub/{subWarehouseId}")
    public Result<List<Map<String, Object>>> bySub(@PathVariable Long subWarehouseId) {
        return Result.ok(locationService.listBySub(subWarehouseId));
    }

    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        return Result.ok(locationService.detail(id));
    }
}
