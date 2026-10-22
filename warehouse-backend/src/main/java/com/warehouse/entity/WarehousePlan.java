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

/**
 * 仓库平面图方案：整张画布一个方案，画布上的图形为各仓库（shapes 存于 planData JSON）。
 */
@Data
@TableName("warehouse_plan")
public class WarehousePlan {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    /** 画布宽度（米） */
    private BigDecimal width;

    /** 画布高度（米） */
    private BigDecimal height;

    /** 方格大小（米） */
    private BigDecimal gridSize;

    /** 背景图（data URL，CAD/手绘图导入后转存） */
    private String bgImage;

    /** 图形元素 JSON（含几何与仓库属性） */
    private String planData;

    /** 来源：1=在线绘制，2=导入图纸 */
    private Integer sourceType;

    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
