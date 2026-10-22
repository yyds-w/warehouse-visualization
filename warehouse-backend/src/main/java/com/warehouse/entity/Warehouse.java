package com.warehouse.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("warehouse")
public class Warehouse {

    @TableId(type = IdType.AUTO)
    private Long id;

    @NotBlank(message = "仓库名称不能为空")
    @Size(max = 128, message = "仓库名称不能超过 128 个字符")
    private String name;

    @NotBlank(message = "仓库编码不能为空")
    @Size(max = 64, message = "仓库编码不能超过 64 个字符")
    private String code;

    private String address;

    private BigDecimal area;

    private BigDecimal usableArea;

    private Long managerId;

    private Integer status;

    private Long layoutId;

    // ===== 在线绘制仓库模块扩展 =====
    /** 容量 */
    private BigDecimal capacity;
    /** 最大重量 */
    private BigDecimal maxWeight;
    /** 仓库立方数（立方米） */
    private BigDecimal volume;
    /** 负责人 */
    private String managerName;
    /** 归属部门 */
    private String department;
    /** 联系电话 */
    private String phone;
    /** 长度（米，平面图上 X 方向） */
    private BigDecimal lengthM;
    /** 宽度（米，平面图上 Y 方向） */
    private BigDecimal widthM;
    /** 高度（米） */
    private BigDecimal heightM;
    /** 形状类型：square/rect/parallelogram/trapezoid */
    private String shapeType;
    /** 平面图上 X 坐标（米） */
    private BigDecimal positionX;
    /** 平面图上 Y 坐标（米） */
    private BigDecimal positionY;
    /** 备注 */
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
