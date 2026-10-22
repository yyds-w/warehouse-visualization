package com.warehouse.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.warehouse.common.BusinessException;
import com.warehouse.entity.Shelf;
import com.warehouse.entity.SubWarehouse;
import com.warehouse.entity.Warehouse;
import com.warehouse.mapper.InventoryMapper;
import com.warehouse.mapper.LocationMapper;
import com.warehouse.mapper.ShelfMapper;
import com.warehouse.mapper.SubWarehouseMapper;
import com.warehouse.mapper.WarehouseMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {

    private final WarehouseMapper warehouseMapper;
    private final SubWarehouseMapper subWarehouseMapper;
    private final ShelfMapper shelfMapper;
    private final LocationMapper locationMapper;
    private final InventoryMapper inventoryMapper;

    public DashboardService(WarehouseMapper warehouseMapper, SubWarehouseMapper subWarehouseMapper,
                            ShelfMapper shelfMapper, LocationMapper locationMapper,
                            InventoryMapper inventoryMapper) {
        this.warehouseMapper = warehouseMapper;
        this.subWarehouseMapper = subWarehouseMapper;
        this.shelfMapper = shelfMapper;
        this.locationMapper = locationMapper;
        this.inventoryMapper = inventoryMapper;
    }

    /**
     * 仓库级全景看板：总货位、占用、利用率、库存总量 + 各子仓库聚合。
     */
    public Map<String, Object> warehouseLevel(Long warehouseId) {
        Warehouse warehouse = warehouseMapper.selectById(warehouseId);
        if (warehouse == null) {
            throw new BusinessException(404, "仓库不存在");
        }
        Map<String, Object> result = new HashMap<>();
        result.put("warehouse", warehouse);

        Map<String, Object> stat = locationMapper.statByWarehouse(warehouseId);
        long total = toLong(stat.get("total"));
        long occupied = toLong(stat.get("occupied"));
        result.put("totalLocations", total);
        result.put("occupiedLocations", occupied);
        result.put("usageRate", rate(occupied, total));
        result.put("totalInventory", inventoryMapper.sumByWarehouse(warehouseId));

        List<SubWarehouse> subs = subWarehouseMapper.selectList(new LambdaQueryWrapper<SubWarehouse>()
                .eq(SubWarehouse::getWarehouseId, warehouseId).orderByAsc(SubWarehouse::getCode));
        List<Map<String, Object>> subList = new ArrayList<>();
        for (SubWarehouse sub : subs) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", sub.getId());
            item.put("name", sub.getName());
            item.put("code", sub.getCode());
            item.put("area", sub.getArea());
            item.put("positionX", sub.getPositionX());
            item.put("positionY", sub.getPositionY());
            item.put("width", sub.getWidth());
            item.put("height", sub.getHeight());
            item.put("status", sub.getStatus());
            Map<String, Object> subStat = locationMapper.statBySubWarehouse(sub.getId());
            long subTotal = toLong(subStat.get("total"));
            long subOccupied = toLong(subStat.get("occupied"));
            item.put("totalLocations", subTotal);
            item.put("occupiedLocations", subOccupied);
            item.put("usageRate", rate(subOccupied, subTotal));
            item.put("totalInventory", inventoryMapper.sumBySubWarehouse(sub.getId()));
            subList.add(item);
        }
        result.put("subWarehouses", subList);
        return result;
    }

    /**
     * 子仓库级货架视图：货架列表 + 每货架货位明细（含库存概要），供区域视图渲染。
     */
    public Map<String, Object> subLevel(Long subWarehouseId) {
        SubWarehouse sub = subWarehouseMapper.selectById(subWarehouseId);
        if (sub == null) {
            throw new BusinessException(404, "子仓库不存在");
        }
        Map<String, Object> result = new HashMap<>();
        result.put("subWarehouse", sub);

        List<Shelf> shelves = shelfMapper.selectList(new LambdaQueryWrapper<Shelf>()
                .eq(Shelf::getSubWarehouseId, subWarehouseId).orderByAsc(Shelf::getCode));
        List<Map<String, Object>> shelfList = new ArrayList<>();
        for (Shelf shelf : shelves) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", shelf.getId());
            item.put("name", shelf.getName());
            item.put("code", shelf.getCode());
            item.put("type", shelf.getType());
            item.put("rowCount", shelf.getRowCount());
            item.put("bayCount", shelf.getBayCount());
            item.put("colCount", shelf.getColCount());
            item.put("layerCount", shelf.getLayerCount());
            item.put("positionX", shelf.getPositionX());
            item.put("positionY", shelf.getPositionY());
            item.put("width", shelf.getWidth());
            item.put("height", shelf.getHeight());
            item.put("status", shelf.getStatus());
            Map<String, Object> stat = locationMapper.statByShelf(shelf.getId());
            long total = toLong(stat.get("total"));
            long occupied = toLong(stat.get("occupied"));
            item.put("totalLocations", total);
            item.put("occupiedLocations", occupied);
            item.put("usageRate", rate(occupied, total));
            item.put("locations", locationMapper.locationsWithStock(shelf.getId()));
            shelfList.add(item);
        }
        result.put("shelves", shelfList);
        return result;
    }

    private long toLong(Object o) {
        return o == null ? 0L : ((Number) o).longValue();
    }

    private double rate(long occupied, long total) {
        return total == 0 ? 0.0 : Math.round(occupied * 1000.0 / total) / 10.0;
    }
}
