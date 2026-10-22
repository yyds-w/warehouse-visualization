package com.warehouse.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class InboundRequest {

    @NotNull(message = "货位不能为空")
    private Long locationId;

    @NotNull(message = "商品不能为空")
    private Long goodsId;

    @NotNull(message = "数量不能为空")
    @DecimalMin(value = "1", message = "入库数量至少为 1")
    private Integer quantity;

    private String batchNo;
}
