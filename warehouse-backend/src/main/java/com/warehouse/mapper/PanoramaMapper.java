package com.warehouse.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.warehouse.entity.Warehouse;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/** 全景可视图聚合查询 */
public interface PanoramaMapper extends BaseMapper<Warehouse> {

    /** 各仓库库存聚合：总量 / 冻结量 / 总体积 / 总重量 */
    @Select("SELECT l.`warehouse_id` AS \"warehouseId\", "
            + "COALESCE(SUM(i.`quantity`),0) AS \"totalQty\", "
            + "COALESCE(SUM(CASE WHEN i.`lock_status`=2 THEN i.`quantity` ELSE 0 END),0) AS \"frozenQty\", "
            + "COALESCE(SUM(CASE WHEN i.`lock_status`=1 THEN i.`quantity` ELSE 0 END),0) AS \"shippingQty\", "
            + "COALESCE(SUM(i.`volume`),0) AS \"totalVolume\", "
            + "COALESCE(SUM(i.`weight`),0) AS \"totalWeight\" "
            + "FROM `inventory` i JOIN `location` l ON i.`location_id`=l.`id` AND l.`deleted`=0 "
            + "WHERE i.`deleted`=0 GROUP BY l.`warehouse_id`")
    List<Map<String, Object>> stockAggByWarehouse();

    /** 指定仓库库存聚合 */
    @Select("SELECT COALESCE(SUM(i.`quantity`),0) AS \"totalQty\", "
            + "COALESCE(SUM(CASE WHEN i.`lock_status`=2 THEN i.`quantity` ELSE 0 END),0) AS \"frozenQty\", "
            + "COALESCE(SUM(CASE WHEN i.`lock_status`=1 THEN i.`quantity` ELSE 0 END),0) AS \"shippingQty\", "
            + "COALESCE(SUM(i.`volume`),0) AS \"totalVolume\", "
            + "COALESCE(SUM(i.`weight`),0) AS \"totalWeight\" "
            + "FROM `inventory` i JOIN `location` l ON i.`location_id`=l.`id` AND l.`deleted`=0 "
            + "WHERE i.`deleted`=0 AND l.`warehouse_id`=#{warehouseId}")
    Map<String, Object> stockAggOfWarehouse(@Param("warehouseId") Long warehouseId);

    /** 各仓库主要存货种类（按数量降序） */
    @Select("SELECT l.`warehouse_id` AS \"warehouseId\", g.`category` AS \"category\", "
            + "SUM(i.`quantity`) AS \"qty\" "
            + "FROM `inventory` i "
            + "JOIN `location` l ON i.`location_id`=l.`id` AND l.`deleted`=0 "
            + "JOIN `goods` g ON i.`goods_id`=g.`id` AND g.`deleted`=0 "
            + "WHERE i.`deleted`=0 AND g.`category` IS NOT NULL AND g.`category`<>'' "
            + "GROUP BY l.`warehouse_id`, g.`category` ORDER BY SUM(i.`quantity`) DESC")
    List<Map<String, Object>> categoryAggByWarehouse();

    /** 指定仓库货位明细（含库存与商品信息），供平面图与数据看板渲染 */
    @Select("SELECT l.`id` AS \"locationId\", l.`location_code` AS \"locationCode\", "
            + "l.`shelf_id` AS \"shelfId\", l.`status` AS \"locationStatus\", "
            + "l.`row_index` AS \"rowIndex\", l.`bay_index` AS \"bayIndex\", "
            + "l.`col_index` AS \"colIndex\", l.`layer_index` AS \"layerIndex\", "
            + "g.`name` AS \"goodsName\", g.`category` AS \"category\", g.`unit` AS \"unit\", "
            + "i.`quantity` AS \"quantity\", i.`volume` AS \"volume\", i.`weight` AS \"weight\", "
            + "i.`expiry_date` AS \"expiryDate\", i.`lock_status` AS \"lockStatus\" "
            + "FROM `location` l "
            + "LEFT JOIN `inventory` i ON i.`location_id`=l.`id` AND i.`deleted`=0 "
            + "LEFT JOIN `goods` g ON i.`goods_id`=g.`id` AND g.`deleted`=0 "
            + "WHERE l.`deleted`=0 AND l.`warehouse_id`=#{warehouseId} "
            + "ORDER BY l.`shelf_id`, l.`row_index`, l.`bay_index`, l.`col_index`, l.`layer_index`")
    List<Map<String, Object>> locationCellsOfWarehouse(@Param("warehouseId") Long warehouseId);

    /** 指定仓库最近 N 天进出库流量 */
    @Select("SELECT `flow_date` AS \"flowDate\", `in_qty` AS \"inQty\", `out_qty` AS \"outQty\" "
            + "FROM `stock_flow` WHERE `warehouse_id`=#{warehouseId} "
            + "ORDER BY `flow_date` DESC LIMIT #{days}")
    List<Map<String, Object>> recentFlow(@Param("warehouseId") Long warehouseId, @Param("days") int days);

    /** 各仓库今日出入库量 */
    @Select("SELECT `warehouse_id` AS \"warehouseId\", `in_qty` AS \"inQty\", `out_qty` AS \"outQty\" "
            + "FROM `stock_flow` WHERE `flow_date`=CURRENT_DATE")
    List<Map<String, Object>> todayFlow();
}
