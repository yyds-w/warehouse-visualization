package com.warehouse.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("warehouse_layout")
public class WarehouseLayout {

    @TableId(type = IdType.AUTO)
    private Long id;

    @NotNull(message = "所属仓库不能为空")
    private Long warehouseId;

    @NotBlank(message = "平面图名称不能为空")
    private String name;

    private BigDecimal width;

    private BigDecimal height;

    private String bgImage;

    private String layoutData;

    private Integer sourceType;

    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
