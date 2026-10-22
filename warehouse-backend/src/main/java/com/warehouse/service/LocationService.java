package com.warehouse.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.warehouse.common.BusinessException;
import com.warehouse.entity.Goods;
import com.warehouse.entity.Inventory;
import com.warehouse.entity.Location;
import com.warehouse.entity.Shelf;
import com.warehouse.entity.SubWarehouse;
import com.warehouse.entity.Warehouse;
import com.warehouse.mapper.GoodsMapper;
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
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class LocationService {

    private final LocationMapper locationMapper;
    private final InventoryMapper inventoryMapper;
    private final GoodsMapper goodsMapper;
    private final ShelfMapper shelfMapper;
    private final SubWarehouseMapper subWarehouseMapper;
    private final WarehouseMapper warehouseMapper;

    public LocationService(LocationMapper locationMapper, InventoryMapper inventoryMapper,
                           GoodsMapper goodsMapper, ShelfMapper shelfMapper,
                           SubWarehouseMapper subWarehouseMapper, WarehouseMapper warehouseMapper) {
        this.locationMapper = locationMapper;
        this.inventoryMapper = inventoryMapper;
        this.goodsMapper = goodsMapper;
        this.shelfMapper = shelfMapper;
        this.subWarehouseMapper = subWarehouseMapper;
        this.warehouseMapper = warehouseMapper;
    }

    public List<Map<String, Object>> listByShelf(Long shelfId) {
        return locationMapper.locationsWithStock(shelfId);
    }

    public List<Map<String, Object>> listBySub(Long subWarehouseId) {
        return locationMapper.locationsWithStockBySub(subWarehouseId);
    }

    public Map<String, Object> detail(Long id) {
        Location loc = locationMapper.selectById(id);
        if (loc == null) {
            throw new BusinessException(404, "货位不存在");
        }
        Map<String, Object> result = new HashMap<>();
        result.put("location", loc);

        Shelf shelf = shelfMapper.selectById(loc.getShelfId());
        SubWarehouse subWarehouse = loc.getSubWarehouseId() == null ? null : subWarehouseMapper.selectById(loc.getSubWarehouseId());
        Warehouse warehouse = loc.getWarehouseId() == null ? null : warehouseMapper.selectById(loc.getWarehouseId());
        result.put("shelfName", shelf == null ? null : shelf.getName());
        result.put("shelfCode", shelf == null ? null : shelf.getCode());
        result.put("subWarehouseName", subWarehouse == null ? null : subWarehouse.getName());
        result.put("warehouseName", warehouse == null ? null : warehouse.getName());

        List<Inventory> inventories = inventoryMapper.selectList(
                new LambdaQueryWrapper<Inventory>().eq(Inventory::getLocationId, id));
        List<Map<String, Object>> stockList = new ArrayList<>();
        if (!inventories.isEmpty()) {
            List<Long> goodsIds = inventories.stream().map(Inventory::getGoodsId).distinct().collect(Collectors.toList());
            Map<Long, Goods> goodsMap = goodsMapper.selectBatchIds(goodsIds).stream()
                    .collect(Collectors.toMap(Goods::getId, Function.identity()));
            for (Inventory inv : inventories) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", inv.getId());
                item.put("goodsId", inv.getGoodsId());
                item.put("quantity", inv.getQuantity());
                item.put("batchNo", inv.getBatchNo());
                item.put("status", inv.getStatus());
                item.put("updatedAt", inv.getUpdatedAt());
                Goods goods = goodsMap.get(inv.getGoodsId());
                if (goods != null) {
                    item.put("goodsName", goods.getName());
                    item.put("skuCode", goods.getSkuCode());
                    item.put("unit", goods.getUnit());
                }
                stockList.add(item);
            }
        }
        result.put("inventories", stockList);
        return result;
    }
}
