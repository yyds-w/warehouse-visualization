package com.warehouse.controller;

import com.warehouse.common.Result;
import com.warehouse.service.PanoramaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/panorama")
public class PanoramaController {

    private final PanoramaService panoramaService;

    public PanoramaController(PanoramaService panoramaService) {
        this.panoramaService = panoramaService;
    }

    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        return Result.ok(panoramaService.overview());
    }

    @GetMapping("/warehouse/{id}")
    public Result<Map<String, Object>> warehouseBoard(@PathVariable Long id) {
        return Result.ok(panoramaService.warehouseBoard(id));
    }
}
