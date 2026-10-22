package com.warehouse.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("shelf")
public class Shelf {

    @TableId(type = IdType.AUTO)
    private Long id;

    @NotNull(message = "所属仓库不能为空")
    private Long warehouseId;

    @NotNull(message = "所属子仓库不能为空")
    private Long subWarehouseId;

    @NotBlank(message = "货架名称不能为空")
    private String name;

    @NotBlank(message = "货架编码不能为空")
    private String code;

    @NotNull(message = "货架类型不能为空")
    private Integer type;

    @NotNull(message = "行数不能为空")
    @DecimalMin(value = "1", message = "行数至少为 1")
    @DecimalMax(value = "50", message = "行数不能超过 50")
    private Integer rowCount;

    @NotNull(message = "排数不能为空")
    @DecimalMin(value = "1", message = "排数至少为 1")
    @DecimalMax(value = "50", message = "排数不能超过 50")
    private Integer bayCount;

    @NotNull(message = "列数不能为空")
    @DecimalMin(value = "1", message = "列数至少为 1")
    @DecimalMax(value = "50", message = "列数不能超过 50")
    private Integer colCount;

    @NotNull(message = "层数不能为空")
    @DecimalMin(value = "1", message = "层数至少为 1")
    @DecimalMax(value = "50", message = "层数不能超过 50")
    private Integer layerCount;

    private BigDecimal positionX;

    private BigDecimal positionY;

    private BigDecimal width;

    private BigDecimal height;

    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
