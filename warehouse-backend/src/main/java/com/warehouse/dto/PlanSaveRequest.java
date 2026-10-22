package com.warehouse.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 保存「在线绘制仓库」平面图方案的请求体：画布参数 + 图形（仓库）列表。
 */
@Data
public class PlanSaveRequest {

    private Long id;

    private String name;

    private BigDecimal width;

    private BigDecimal height;

    private BigDecimal gridSize;

    private String bgImage;

    private List<PlanShape> shapes;

    @Data
    public static class PlanShape {
        /** 关联仓库 ID，首次保存前为空 */
        private Long warehouseId;
        /** square / rect / parallelogram / trapezoid */
        private String shape;
        /** 画布 X（米） */
        private Double x;
        /** 画布 Y（米） */
        private Double y;
        /** 长度（米，X 方向） */
        private Double w;
        /** 宽度（米，Y 方向） */
        private Double h;
        /** 平行四边形顶边水平偏移（米） */
        private Double skew;
        /** 梯形顶边长度（米） */
        private Double topW;

        private String name;
        private String code;
        /** 容量 */
        private BigDecimal capacity;
        /** 最大重量 */
        private BigDecimal maxWeight;
        /** 仓库立方数 */
        private BigDecimal volume;
        /** 仓库状态：1=启用 0=停用 */
        private Integer status;
        /** 高度（米） */
        private Double heightM;
        /** 负责人 */
        private String manager;
        /** 归属部门 */
        private String department;
        /** 联系电话 */
        private String phone;
        private String address;
        private String remark;
    }
}
