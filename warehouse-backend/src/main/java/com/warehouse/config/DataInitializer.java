package com.warehouse.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.warehouse.entity.Goods;
import com.warehouse.entity.Inventory;
import com.warehouse.entity.Location;
import com.warehouse.entity.Shelf;
import com.warehouse.entity.StockFlow;
import com.warehouse.entity.Warehouse;
import com.warehouse.mapper.GoodsMapper;
import com.warehouse.mapper.InventoryMapper;
import com.warehouse.mapper.LocationMapper;
import com.warehouse.mapper.ShelfMapper;
import com.warehouse.mapper.StockFlowMapper;
import com.warehouse.mapper.WarehouseMapper;
import com.warehouse.service.ShelfService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 启动时初始化演示数据：若货位表为空，则按已有货架参数批量生成货位，
 * 并将约三分之一的货位置为占用、生成库存记录，保证看板与找货功能开箱有数据。
 * 同时负责：老库存记录补齐体积/重量/保质期字段、播种近 7 日进出库流量。
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final ShelfMapper shelfMapper;
    private final LocationMapper locationMapper;
    private final GoodsMapper goodsMapper;
    private final InventoryMapper inventoryMapper;
    private final ShelfService shelfService;
    private final StockFlowMapper stockFlowMapper;
    private final WarehouseMapper warehouseMapper;

    public DataInitializer(ShelfMapper shelfMapper, LocationMapper locationMapper,
                           GoodsMapper goodsMapper, InventoryMapper inventoryMapper,
                           ShelfService shelfService, StockFlowMapper stockFlowMapper,
                           WarehouseMapper warehouseMapper) {
        this.shelfMapper = shelfMapper;
        this.locationMapper = locationMapper;
        this.goodsMapper = goodsMapper;
        this.inventoryMapper = inventoryMapper;
        this.shelfService = shelfService;
        this.stockFlowMapper = stockFlowMapper;
        this.warehouseMapper = warehouseMapper;
    }

    @Override
    public void run(String... args) {
        if (locationMapper.selectCount(null) == 0) {
            initLocationsAndInventory();
        }
        backfillInventoryMetrics();
        seedStockFlow();
    }

    private void initLocationsAndInventory() {
        List<Shelf> shelves = shelfMapper.selectList(null);
        for (Shelf shelf : shelves) {
            shelfService.generateLocations(shelf);
        }
        List<Location> locations = locationMapper.selectList(null);
        List<Goods> goodsList = goodsMapper.selectList(null);
        int occupied = 0;
        int index = 0;
        for (Location loc : locations) {
            if (index % 3 == 0 && !goodsList.isEmpty()) {
                loc.setStatus(2);
                locationMapper.updateById(loc);
                Inventory inv = new Inventory();
                inv.setLocationId(loc.getId());
                inv.setGoodsId(goodsList.get(index % goodsList.size()).getId());
                inv.setQuantity(20 + (index % 80));
                inv.setBatchNo("B2026-" + (1000 + index));
                inv.setStatus(1);
                fillMetrics(inv, index);
                inventoryMapper.insert(inv);
                occupied++;
            }
            index++;
        }
        log.info("[DataInitializer] 生成货位 {} 个，其中占用 {} 个（含库存记录）", locations.size(), occupied);
    }

    /** 老数据升级：库存记录缺少体积/重量时按确定性规则补齐 */
    private void backfillInventoryMetrics() {
        List<Inventory> list = inventoryMapper.selectList(
                new LambdaQueryWrapper<Inventory>().isNull(Inventory::getVolume));
        int index = 0;
        for (Inventory inv : list) {
            fillMetrics(inv, inv.getId() == null ? index : inv.getId().intValue());
            inventoryMapper.updateById(inv);
            index++;
        }
        if (!list.isEmpty()) {
            log.info("[DataInitializer] 补齐 {} 条库存记录的体积/重量/保质期/锁定状态", list.size());
        }
    }

    /** 确定性填充：体积、重量、保质期（部分已过期）、锁定状态（少量出货中/冻结） */
    private void fillMetrics(Inventory inv, int seed) {
        int qty = inv.getQuantity() == null ? 0 : inv.getQuantity();
        inv.setVolume(BigDecimal.valueOf(qty * (8 + seed % 12) / 100.0).setScale(2, java.math.RoundingMode.HALF_UP));
        inv.setWeight(BigDecimal.valueOf(qty * (3 + seed % 7) / 10.0).setScale(2, java.math.RoundingMode.HALF_UP));
        // 保质期：以今天为基准 -15 ~ +165 天，约 8% 已过期
        inv.setExpiryDate(LocalDate.now().plusDays((seed % 181) - 15));
        // 锁定状态：约 8% 出货中、6% 冻结
        inv.setLockStatus(seed % 13 == 0 ? 1 : (seed % 17 == 0 ? 2 : 0));
    }

    /** 播种近 7 日进出库流量（确定性，幂等） */
    private void seedStockFlow() {
        if (stockFlowMapper.selectCount(null) > 0) {
            return;
        }
        List<Warehouse> warehouses = warehouseMapper.selectList(null);
        LocalDate today = LocalDate.now();
        int rows = 0;
        for (Warehouse wh : warehouses) {
            for (int d = 6; d >= 0; d--) {
                StockFlow flow = new StockFlow();
                flow.setWarehouseId(wh.getId());
                flow.setFlowDate(today.minusDays(d));
                long base = wh.getId() == null ? 1 : wh.getId();
                flow.setInQty(40 + (int) ((base * 7 + d * 13) % 120));
                flow.setOutQty(35 + (int) ((base * 11 + d * 7) % 110));
                flow.setCreatedAt(LocalDateTime.now());
                stockFlowMapper.insert(flow);
                rows++;
            }
        }
        log.info("[DataInitializer] 播种进出库流量 {} 行", rows);
    }
}
