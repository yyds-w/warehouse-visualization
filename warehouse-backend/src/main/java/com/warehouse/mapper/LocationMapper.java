package com.warehouse.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.warehouse.entity.Location;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

public interface LocationMapper extends BaseMapper<Location> {

    // location_code 有唯一索引且业务上按 shelfCode 前缀成套生成，
    // 逻辑删除会让重建货架时撞唯一键，这里必须物理删除
    @Delete("DELETE FROM `location` WHERE `shelf_id`=#{shelfId}")
    int physicalDeleteByShelf(@Param("shelfId") Long shelfId);

    @Select("SELECT COUNT(*) AS \"total\", "
            + "COALESCE(SUM(CASE WHEN `status`=2 THEN 1 ELSE 0 END),0) AS \"occupied\" "
            + "FROM `location` WHERE `deleted`=0 AND `warehouse_id`=#{warehouseId}")
    Map<String, Object> statByWarehouse(@Param("warehouseId") Long warehouseId);

    @Select("SELECT COUNT(*) AS \"total\", "
            + "COALESCE(SUM(CASE WHEN `status`=2 THEN 1 ELSE 0 END),0) AS \"occupied\" "
            + "FROM `location` WHERE `deleted`=0 AND `sub_warehouse_id`=#{subWarehouseId}")
    Map<String, Object> statBySubWarehouse(@Param("subWarehouseId") Long subWarehouseId);

    @Select("SELECT COUNT(*) AS \"total\", "
            + "COALESCE(SUM(CASE WHEN `status`=2 THEN 1 ELSE 0 END),0) AS \"occupied\" "
            + "FROM `location` WHERE `deleted`=0 AND `shelf_id`=#{shelfId}")
    Map<String, Object> statByShelf(@Param("shelfId") Long shelfId);

    @Select("SELECT l.`id` AS \"id\", l.`location_code` AS \"locationCode\", l.`status` AS \"status\", "
            + "l.`row_index` AS \"rowIndex\", l.`bay_index` AS \"bayIndex\", l.`col_index` AS \"colIndex\", "
            + "l.`layer_index` AS \"layerIndex\", l.`position_x` AS \"positionX\", l.`position_y` AS \"positionY\", "
            + "g.`name` AS \"goodsName\", i.`quantity` AS \"quantity\" "
            + "FROM `location` l "
            + "LEFT JOIN `inventory` i ON i.`location_id`=l.`id` AND i.`deleted`=0 "
            + "LEFT JOIN `goods` g ON i.`goods_id`=g.`id` AND g.`deleted`=0 "
            + "WHERE l.`deleted`=0 AND l.`shelf_id`=#{shelfId} "
            + "ORDER BY l.`layer_index`, l.`row_index`, l.`bay_index`, l.`col_index`")
    List<Map<String, Object>> locationsWithStock(@Param("shelfId") Long shelfId);

    @Select("SELECT l.`id` AS \"id\", l.`shelf_id` AS \"shelfId\", l.`location_code` AS \"locationCode\", "
            + "l.`status` AS \"status\", l.`row_index` AS \"rowIndex\", l.`bay_index` AS \"bayIndex\", "
            + "l.`col_index` AS \"colIndex\", l.`layer_index` AS \"layerIndex\", "
            + "COALESCE(SUM(i.`quantity`),0) AS \"quantity\", "
            + "COALESCE(SUM(i.`volume`),0) AS \"volume\", "
            + "COALESCE(SUM(i.`weight`),0) AS \"weight\", "
            + "COUNT(DISTINCT i.`goods_id`) AS \"goodsCount\", "
            + "MAX(g.`name`) AS \"goodsName\" "
            + "FROM `location` l "
            + "LEFT JOIN `inventory` i ON i.`location_id`=l.`id` AND i.`deleted`=0 "
            + "LEFT JOIN `goods` g ON i.`goods_id`=g.`id` AND g.`deleted`=0 "
            + "WHERE l.`deleted`=0 AND l.`sub_warehouse_id`=#{subWarehouseId} "
            + "GROUP BY l.`id`, l.`shelf_id`, l.`location_code`, l.`status`, l.`row_index`, l.`bay_index`, "
            + "l.`col_index`, l.`layer_index` "
            + "ORDER BY l.`shelf_id`, l.`layer_index`, l.`row_index`, l.`bay_index`, l.`col_index`")
    List<Map<String, Object>> locationsWithStockBySub(@Param("subWarehouseId") Long subWarehouseId);

    @Select("SELECT l.`id` AS \"locationId\", l.`location_code` AS \"locationCode\", "
            + "l.`status` AS \"locationStatus\", l.`position_x` AS \"positionX\", l.`position_y` AS \"positionY\", "
            + "l.`warehouse_id` AS \"warehouseId\", "
            + "s.`id` AS \"shelfId\", s.`name` AS \"shelfName\", s.`code` AS \"shelfCode\", "
            + "sw.`id` AS \"subWarehouseId\", sw.`name` AS \"subWarehouseName\", "
            + "g.`id` AS \"goodsId\", g.`name` AS \"goodsName\", g.`sku_code` AS \"skuCode\", g.`unit` AS \"unit\", "
            + "i.`quantity` AS \"quantity\", i.`batch_no` AS \"batchNo\" "
            + "FROM `location` l "
            + "JOIN `shelf` s ON l.`shelf_id`=s.`id` AND s.`deleted`=0 "
            + "JOIN `sub_warehouse` sw ON l.`sub_warehouse_id`=sw.`id` AND sw.`deleted`=0 "
            + "LEFT JOIN `inventory` i ON i.`location_id`=l.`id` AND i.`deleted`=0 "
            + "LEFT JOIN `goods` g ON i.`goods_id`=g.`id` AND g.`deleted`=0 "
            + "WHERE l.`deleted`=0 AND ("
            + "l.`location_code` LIKE CONCAT('%',#{keyword},'%') "
            + "OR g.`name` LIKE CONCAT('%',#{keyword},'%') "
            + "OR g.`sku_code` LIKE CONCAT('%',#{keyword},'%')) "
            + "ORDER BY l.`location_code` LIMIT 100")
    List<Map<String, Object>> searchLocations(@Param("keyword") String keyword);
}
