package com.warehouse.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("warehouse_user_relation")
public class WarehouseUserRelation {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long warehouseId;

    private Long userId;

    private LocalDateTime createdAt;
}
