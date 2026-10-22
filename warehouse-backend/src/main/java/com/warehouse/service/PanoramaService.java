package com.warehouse.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.warehouse.common.BusinessException;
import com.warehouse.entity.Shelf;
import com.warehouse.entity.Warehouse;
import com.warehouse.mapper.LocationMapper;
import com.warehouse.mapper.PanoramaMapper;
import com.warehouse.mapper.ShelfMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 全景可视图：仓库全景卡片聚合 + 单仓「平面图与数据看板」数据。
 */
@Service
public class PanoramaService {

    private final PanoramaMapper panoramaMapper;
    private final ShelfMapper shelfMapper;
    private final LocationMapper locationMapper;

    public PanoramaService(PanoramaMapper panoramaMapper, ShelfMapper shelfMapper,
                           LocationMapper locationMapper) {
        this.panoramaMapper = panoramaMapper;
        this.shelfMapper = shelfMapper;
        this.locationMapper = locationMapper;
    }

    /**
     * 仓库全景可视图：所有仓库卡片 + 悬停详情指标。
     */
    public Map<String, Object> overview() {
        List<Warehouse> warehouses = panoramaMapper.selectList(new LambdaQueryWrapper<Warehouse>()
                .orderByAsc(Warehouse::getCode));

        Map<Long, Map<String, Object>> stockByWh = new HashMap<>();
        for (Map<String, Object> row : panoramaMapper.stockAggByWarehouse()) {
            stockByWh.put(toLong(row.get("warehouseId")), row);
        }
        Map<Long, List<String>> categoriesByWh = new HashMap<>();
        for (Map<String, Object> row : panoramaMapper.categoryAggByWarehouse()) {
            Long whId = toLong(row.get("warehouseId"));
            List<String> list = categoriesByWh.computeIfAbsent(whId, k -> new ArrayList<>());
            if (list.size() < 5) {
                list.add(String.valueOf(row.get("category")));
            }
        }
        Map<Long, Map<String, Object>> todayByWh = new HashMap<>();
        for (Map<String, Object> row : panoramaMapper.todayFlow()) {
            todayByWh.put(toLong(row.get("warehouseId")), row);
        }

        List<Map<String, Object>> list = new ArrayList<>();
        for (Warehouse wh : warehouses) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", wh.getId());
            item.put("name", wh.getName());
            item.put("code", wh.getCode());
            item.put("status", wh.getStatus());
            item.put("positionX", wh.getPositionX());
            item.put("positionY", wh.getPositionY());

            Map<String, Object> stock = stockByWh.getOrDefault(wh.getId(), new HashMap<>());
            long totalQty = toLong(stock.get("totalQty"));
            long frozenQty = toLong(stock.get("frozenQty"));

            Map<String, Object> stat = locationMapper.statByWarehouse(wh.getId());
            long totalLocations = toLong(stat.get("total"));
            long occupied = toLong(stat.get("occupied"));

            Map<String, Object> today = todayByWh.getOrDefault(wh.getId(), new HashMap<>());
            long todayIn = toLong(today.get("inQty"));
            long todayOut = toLong(today.get("outQty"));

            item.put("totalStock", totalQty);
            item.put("availableStock", Math.max(totalQty - frozenQty, 0));
            item.put("usageRate", rate(occupied, totalLocations));
            item.put("frozenStock", frozenQty);
            // 计划类指标按当日流量确定性推算：出库计划=今日出库，预出=明日计划(约 60%)，预冻结=冻结的 20%
            item.put("outboundPlanQty", todayOut);
            item.put("preOutboundQty", Math.round(todayOut * 0.6));
            item.put("preFrozenQty", Math.round(frozenQty * 0.2));
            item.put("outboundQty", todayOut);
            item.put("inboundQty", todayIn);
            item.put("categories", categoriesByWh.getOrDefault(wh.getId(), List.of()));
            list.add(item);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("warehouses", list);
        result.put("total", list.size());
        return result;
    }

    /**
     * 仓库平面图与数据看板：头部统计 + 全部货架货位明细 + 存货种类占比 + 近 7 日进出库流量。
     */
    public Map<String, Object> warehouseBoard(Long warehouseId) {
        Warehouse warehouse = panoramaMapper.selectById(warehouseId);
        if (warehouse == null) {
            throw new BusinessException(404, "仓库不存在");
        }
        Map<String, Object> result = new HashMap<>();
        result.put("warehouse", warehouse);

        Map<String, Object> stock = panoramaMapper.stockAggOfWarehouse(warehouseId);
        long totalQty = toLong(stock.get("totalQty"));
        long frozenQty = toLong(stock.get("frozenQty"));
        Map<String, Object> stat = locationMapper.statByWarehouse(warehouseId);
        long totalLocations = toLong(stat.get("total"));
        long occupied = toLong(stat.get("occupied"));

        result.put("totalStock", totalQty);
        result.put("availableStock", Math.max(totalQty - frozenQty, 0));
        result.put("totalWeight", toBig(stock.get("totalWeight")));
        result.put("totalVolume", toBig(stock.get("totalVolume")));
        result.put("usageRate", rate(occupied, totalLocations));

        List<Shelf> shelves = shelfMapper.selectList(new LambdaQueryWrapper<Shelf>()
                .eq(Shelf::getWarehouseId, warehouseId).orderByAsc(Shelf::getCode));

        Map<Long, List<Map<String, Object>>> cellsByShelf = new LinkedHashMap<>();
        for (Map<String, Object> cell : panoramaMapper.locationCellsOfWarehouse(warehouseId)) {
            cellsByShelf.computeIfAbsent(toLong(cell.get("shelfId")), k -> new ArrayList<>()).add(cell);
        }

        LocalDate today = LocalDate.now();
        List<Map<String, Object>> shelfList = new ArrayList<>();
        for (Shelf shelf : shelves) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", shelf.getId());
            item.put("name", shelf.getName());
            item.put("code", shelf.getCode());
            item.put("type", shelf.getType());
            item.put("layerCount", shelf.getLayerCount());
            item.put("rowCount", shelf.getRowCount());
            item.put("bayCount", shelf.getBayCount());
            item.put("colCount", shelf.getColCount());
            List<Map<String, Object>> cells = new ArrayList<>();
            for (Map<String, Object> cell : cellsByShelf.getOrDefault(shelf.getId(), List.of())) {
                Map<String, Object> c = new HashMap<>(cell);
                c.put("cellStatus", cellStatus(cell, today));
                cells.add(c);
            }
            item.put("cells", cells);
            shelfList.add(item);
        }
        result.put("shelves", shelfList);

        List<Map<String, Object>> categoryStats = new ArrayList<>();
        for (Map<String, Object> row : panoramaMapper.categoryAggByWarehouse()) {
            if (warehouseId.equals(toLong(row.get("warehouseId")))) {
                Map<String, Object> c = new HashMap<>();
                c.put("category", row.get("category"));
                c.put("qty", toLong(row.get("qty")));
                categoryStats.add(c);
            }
        }
        result.put("categoryStats", categoryStats);

        List<Map<String, Object>> flow = panoramaMapper.recentFlow(warehouseId, 7);
        List<Map<String, Object>> dailyFlow = new ArrayList<>();
        for (int i = flow.size() - 1; i >= 0; i--) {
            Map<String, Object> row = flow.get(i);
            Map<String, Object> d = new HashMap<>();
            LocalDate date = toDate(row.get("flowDate"));
            d.put("date", date == null ? "" : date.toString());
            d.put("weekday", date == null ? "" : weekdayCn(date));
            d.put("inQty", toLong(row.get("inQty")));
            d.put("outQty", toLong(row.get("outQty")));
            dailyFlow.add(d);
        }
        result.put("dailyFlow", dailyFlow);
        return result;
    }

    /** 货位看板状态：empty 空闲 / normal 有货 / expired 过期 / shipping 出货中 */
    private String cellStatus(Map<String, Object> cell, LocalDate today) {
        if (cell.get("quantity") == null) {
            return "empty";
        }
        if (Integer.valueOf(1).equals(cell.get("lockStatus"))) {
            return "shipping";
        }
        LocalDate expiry = toDate(cell.get("expiryDate"));
        if (expiry != null && expiry.isBefore(today)) {
            return "expired";
        }
        return "normal";
    }

    private LocalDate toDate(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof java.sql.Date d) {
            return d.toLocalDate();
        }
        if (o instanceof LocalDate d) {
            return d;
        }
        String s = String.valueOf(o);
        return s.length() >= 10 ? LocalDate.parse(s.substring(0, 10)) : null;
    }

    private String weekdayCn(LocalDate date) {
        String[] names = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};
        return names[date.getDayOfWeek().getValue() - 1];
    }

    private long toLong(Object o) {
        return o == null ? 0L : ((Number) o).longValue();
    }

    private BigDecimal toBig(Object o) {
        if (o == null) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(String.valueOf(o)).setScale(2, RoundingMode.HALF_UP);
    }

    private double rate(long occupied, long total) {
        return total == 0 ? 0.0 : Math.round(occupied * 1000.0 / total) / 10.0;
    }
}
