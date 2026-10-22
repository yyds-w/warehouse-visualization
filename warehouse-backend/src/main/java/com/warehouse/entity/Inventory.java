package com.warehouse.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("inventory")
public class Inventory {

    @TableId(type = IdType.AUTO)
    private Long id;

    @NotNull(message = "货位不能为空")
    private Long locationId;

    @NotNull(message = "商品不能为空")
    private Long goodsId;

    @NotNull(message = "数量不能为空")
    private Integer quantity;

    private String batchNo;

    private Integer status;

    /** 占用体积（立方米） */
    private BigDecimal volume;

    /** 总重量（千克） */
    private BigDecimal weight;

    /** 保质期至 */
    private LocalDate expiryDate;

    /** 锁定状态：0 正常 1 出货中 2 冻结 */
    private Integer lockStatus;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
