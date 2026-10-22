package com.warehouse.controller;

import com.warehouse.common.PageResult;
import com.warehouse.common.Result;
import com.warehouse.entity.WarehouseUser;
import com.warehouse.service.UserService;
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
@RequestMapping("/api/user")
@Validated
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/page")
    public Result<PageResult<WarehouseUser>> page(@RequestParam(required = false) String keyword,
                                                  @RequestParam(required = false) Integer role,
                                                  @RequestParam(defaultValue = "1") long pageNum,
                                                  @RequestParam(defaultValue = "10") long pageSize) {
        return Result.ok(userService.page(keyword, role, pageNum, pageSize));
    }

    @GetMapping("/all")
    public Result<List<WarehouseUser>> all() {
        return Result.ok(userService.listAll());
    }

    @PostMapping
    public Result<WarehouseUser> create(@RequestBody @Valid WarehouseUser user) {
        return Result.ok("新增成功", userService.create(user));
    }

    @PutMapping
    public Result<Void> update(@RequestBody @Valid WarehouseUser user) {
        userService.update(user);
        return Result.ok("修改成功", null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return Result.ok("删除成功", null);
    }
}
