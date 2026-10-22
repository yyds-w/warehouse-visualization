package com.warehouse.controller;

import com.warehouse.common.PageResult;
import com.warehouse.common.Result;
import com.warehouse.entity.Goods;
import com.warehouse.service.GoodsService;
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
@RequestMapping("/api/goods")
@Validated
public class GoodsController {

    private final GoodsService goodsService;

    public GoodsController(GoodsService goodsService) {
        this.goodsService = goodsService;
    }

    @GetMapping("/page")
    public Result<PageResult<Goods>> page(@RequestParam(required = false) String keyword,
                                          @RequestParam(required = false) String category,
                                          @RequestParam(defaultValue = "1") long pageNum,
                                          @RequestParam(defaultValue = "10") long pageSize) {
        return Result.ok(goodsService.page(keyword, category, pageNum, pageSize));
    }

    @GetMapping("/all")
    public Result<List<Goods>> all() {
        return Result.ok(goodsService.listAll());
    }

    @PostMapping
    public Result<Goods> create(@RequestBody @Valid Goods goods) {
        return Result.ok("新增成功", goodsService.create(goods));
    }

    @PutMapping
    public Result<Void> update(@RequestBody @Valid Goods goods) {
        goodsService.update(goods);
        return Result.ok("修改成功", null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        goodsService.delete(id);
        return Result.ok("删除成功", null);
    }
}
