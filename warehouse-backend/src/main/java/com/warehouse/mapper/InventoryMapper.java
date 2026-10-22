package com.warehouse.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.warehouse.entity.Inventory;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Map;

public interface InventoryMapper extends BaseMapper<Inventory> {

    @Select("SELECT i.`id` AS \"id\", i.`quantity` AS \"quantity\", i.`batch_no` AS \"batchNo\", "
            + "i.`status` AS \"status\", i.`updated_at` AS \"updatedAt\", "
            + "g.`id` AS \"goodsId\", g.`name` AS \"goodsName\", g.`sku_code` AS \"skuCode\", g.`unit` AS \"unit\", "
            + "l.`id` AS \"locationId\", l.`location_code` AS \"locationCode\" "
            + "FROM `inventory` i "
            + "JOIN `goods` g ON i.`goods_id`=g.`id` AND g.`deleted`=0 "
            + "JOIN `location` l ON i.`location_id`=l.`id` AND l.`deleted`=0 "
            + "WHERE i.`deleted`=0 AND (#{keyword} IS NULL "
            + "OR g.`name` LIKE CONCAT('%',#{keyword},'%') "
            + "OR g.`sku_code` LIKE CONCAT('%',#{keyword},'%') "
            + "OR l.`location_code` LIKE CONCAT('%',#{keyword},'%')) "
            + "ORDER BY i.`updated_at` DESC")
    IPage<Map<String, Object>> pageWithDetail(Page<Map<String, Object>> page, @Param("keyword") String keyword);

    @Select("SELECT COALESCE(SUM(i.`quantity`),0) FROM `inventory` i "
            + "JOIN `location` l ON i.`location_id`=l.`id` AND l.`deleted`=0 "
            + "WHERE i.`deleted`=0 AND l.`warehouse_id`=#{warehouseId}")
    Long sumByWarehouse(@Param("warehouseId") Long warehouseId);

    @Select("SELECT COALESCE(SUM(i.`quantity`),0) FROM `inventory` i "
            + "JOIN `location` l ON i.`location_id`=l.`id` AND l.`deleted`=0 "
            + "WHERE i.`deleted`=0 AND l.`sub_warehouse_id`=#{subWarehouseId}")
    Long sumBySubWarehouse(@Param("subWarehouseId") Long subWarehouseId);

    @Select("SELECT COALESCE(SUM(i.`quantity`),0) FROM `inventory` i "
            + "WHERE i.`deleted`=0 AND i.`location_id`=#{locationId}")
    Long sumByLocation(@Param("locationId") Long locationId);
}
