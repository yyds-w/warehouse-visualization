package com.warehouse.dto;

import com.warehouse.entity.Shelf;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ShelfSaveRequest {

    @NotNull(message = "所属仓库不能为空")
    private Long warehouseId;

    @NotNull(message = "所属子仓库不能为空")
    private Long subWarehouseId;

    @Valid
    private List<Shelf> shelves = new ArrayList<>();

    private List<Long> removedIds = new ArrayList<>();
}
