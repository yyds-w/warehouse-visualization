package com.warehouse.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("location")
public class Location {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long warehouseId;

    private Long subWarehouseId;

    private Long shelfId;

    private String locationCode;

    private Integer rowIndex;

    private Integer bayIndex;

    private Integer colIndex;

    private Integer layerIndex;

    private BigDecimal positionX;

    private BigDecimal positionY;

    private Integer status;

    private BigDecimal capacity;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
