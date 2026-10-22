package com.warehouse.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.warehouse.common.BusinessException;
import com.warehouse.dto.PlanSaveRequest;
import com.warehouse.entity.Warehouse;
import com.warehouse.entity.WarehousePlan;
import com.warehouse.mapper.SubWarehouseMapper;
import com.warehouse.mapper.WarehouseMapper;
import com.warehouse.mapper.WarehousePlanMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 在线绘制仓库：整张画布一个方案，画布上的每个图形对应一个仓库。
 * 保存时把图形属性同步到 warehouse 表（新建/更新/删除），
 * 图形几何（含平行四边形偏移、梯形顶宽等）持久化在 plan_data JSON 中。
 */
@Service
public class PlanService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final WarehousePlanMapper planMapper;
    private final WarehouseMapper warehouseMapper;
    private final SubWarehouseMapper subWarehouseMapper;

    public PlanService(WarehousePlanMapper planMapper, WarehouseMapper warehouseMapper,
                       SubWarehouseMapper subWarehouseMapper) {
        this.planMapper = planMapper;
        this.warehouseMapper = warehouseMapper;
        this.subWarehouseMapper = subWarehouseMapper;
    }

    /** 获取当前方案；不存在时创建默认 60m × 40m、1m 方格的空白方案 */
    public WarehousePlan getPlan() {
        WarehousePlan plan = latestPlan();
        if (plan == null) {
            plan = new WarehousePlan();
            plan.setName("仓库平面图");
            plan.setWidth(new BigDecimal("60"));
            plan.setHeight(new BigDecimal("40"));
            plan.setGridSize(new BigDecimal("1"));
            plan.setPlanData("{\"shapes\":[]}");
            plan.setSourceType(1);
            plan.setStatus(1);
            planMapper.insert(plan);
        }
        return plan;
    }

    @Transactional
    public SaveResult savePlan(PlanSaveRequest req) {
        if (req.getShapes() == null) {
            req.setShapes(new ArrayList<>());
        }
        WarehousePlan plan = getPlan();
        List<PlanSaveRequest.PlanShape> oldShapes = parseShapes(plan.getPlanData());
        Set<Long> oldWarehouseIds = new HashSet<>();
        for (PlanSaveRequest.PlanShape s : oldShapes) {
            if (s.getWarehouseId() != null) {
                oldWarehouseIds.add(s.getWarehouseId());
            }
        }

        Set<Long> keptIds = new HashSet<>();
        List<String> skippedDelete = new ArrayList<>();
        for (PlanSaveRequest.PlanShape s : req.getShapes()) {
            if (s.getName() == null || s.getName().isBlank()) {
                throw new BusinessException("图形名称（仓库名称）不能为空");
            }
            Warehouse w = toWarehouse(s);
            if (s.getWarehouseId() != null && warehouseMapper.selectById(s.getWarehouseId()) != null) {
                w.setId(s.getWarehouseId());
                warehouseMapper.updateById(w);
            } else {
                w.setCode(pickCode(s.getCode()));
                warehouseMapper.insert(w);
                s.setWarehouseId(w.getId());
            }
            keptIds.add(s.getWarehouseId());
        }

        // 从方案中删除的仓库：无子仓库才真删（逻辑删除），否则保留并提示
        for (PlanSaveRequest.PlanShape old : oldShapes) {
            Long oldId = old.getWarehouseId();
            if (oldId == null || keptIds.contains(oldId)) {
                continue;
            }
            Long subCount = subWarehouseMapper.selectCount(new LambdaQueryWrapper<com.warehouse.entity.SubWarehouse>()
                    .eq(com.warehouse.entity.SubWarehouse::getWarehouseId, oldId));
            if (subCount != null && subCount > 0) {
                Warehouse kept = warehouseMapper.selectById(oldId);
                skippedDelete.add(kept != null ? kept.getName() : String.valueOf(oldId));
            } else {
                warehouseMapper.deleteById(oldId);
            }
        }

        plan.setName(req.getName() == null || req.getName().isBlank() ? plan.getName() : req.getName());
        if (req.getWidth() != null) {
            plan.setWidth(req.getWidth());
        }
        if (req.getHeight() != null) {
            plan.setHeight(req.getHeight());
        }
        if (req.getGridSize() != null) {
            plan.setGridSize(req.getGridSize());
        }
        if (req.getBgImage() != null) {
            plan.setBgImage(req.getBgImage());
        }
        plan.setPlanData(writeShapes(req.getShapes()));
        planMapper.updateById(plan);

        SaveResult result = new SaveResult();
        result.setPlan(plan);
        result.setSkippedDelete(skippedDelete);
        return result;
    }

    /** 导入 CAD/手绘图：作为方案背景图，可在其上继续绘制（智能识别需第三方服务，此处保留边界） */
    @Transactional
    public WarehousePlan importBackground(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }
        WarehousePlan plan = getPlan();
        try {
            String contentType = file.getContentType() == null ? "image/png" : file.getContentType();
            plan.setBgImage("data:" + contentType + ";base64,"
                    + Base64.getEncoder().encodeToString(file.getBytes()));
        } catch (IOException e) {
            throw new BusinessException("读取上传文件失败：" + e.getMessage());
        }
        plan.setSourceType(2);
        planMapper.updateById(plan);
        return plan;
    }

    private Warehouse toWarehouse(PlanSaveRequest.PlanShape s) {
        Warehouse w = new Warehouse();
        w.setName(s.getName());
        w.setCode(s.getCode());
        w.setStatus(s.getStatus() == null ? 1 : s.getStatus());
        w.setCapacity(s.getCapacity());
        w.setMaxWeight(s.getMaxWeight());
        w.setVolume(s.getVolume());
        w.setManagerName(s.getManager());
        w.setDepartment(s.getDepartment());
        w.setPhone(s.getPhone());
        w.setAddress(s.getAddress());
        w.setRemark(s.getRemark());
        w.setShapeType(s.getShape());
        w.setHeightM(s.getHeightM() == null ? null : BigDecimal.valueOf(s.getHeightM()));
        if (s.getW() != null && s.getH() != null) {
            w.setLengthM(round2(s.getW()));
            w.setWidthM(round2(s.getH()));
            w.setArea(round2(s.getW() * s.getH()));
        }
        w.setPositionX(s.getX() == null ? null : round2(s.getX()));
        w.setPositionY(s.getY() == null ? null : round2(s.getY()));
        return w;
    }

    private BigDecimal round2(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP);
    }

    /** 编码为空或冲突时自动生成 WH001/W002… */
    private String pickCode(String code) {
        String candidate = code == null || code.isBlank() ? null : code.trim();
        if (candidate != null && warehouseMapper.countByCodeIncludingDeleted(candidate) == 0) {
            return candidate;
        }
        for (int i = 1; i <= 999; i++) {
            String next = String.format("WH%03d", i);
            if (warehouseMapper.countByCodeIncludingDeleted(next) == 0) {
                return next;
            }
        }
        return "WH" + System.currentTimeMillis();
    }

    private WarehousePlan latestPlan() {
        return planMapper.selectOne(new LambdaQueryWrapper<WarehousePlan>()
                .orderByDesc(WarehousePlan::getId)
                .last("LIMIT 1"));
    }

    private List<PlanSaveRequest.PlanShape> parseShapes(String planData) {
        try {
            PlanData pd = MAPPER.readValue(planData == null || planData.isBlank()
                    ? "{\"shapes\":[]}" : planData, PlanData.class);
            return pd.getShapes() == null ? new ArrayList<>() : pd.getShapes();
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    private String writeShapes(List<PlanSaveRequest.PlanShape> shapes) {
        try {
            PlanData pd = new PlanData();
            pd.setShapes(shapes);
            return MAPPER.writeValueAsString(pd);
        } catch (IOException e) {
            throw new BusinessException("序列化平面图数据失败：" + e.getMessage());
        }
    }

    public static class PlanData {
        private List<PlanSaveRequest.PlanShape> shapes;

        public List<PlanSaveRequest.PlanShape> getShapes() {
            return shapes;
        }

        public void setShapes(List<PlanSaveRequest.PlanShape> shapes) {
            this.shapes = shapes;
        }
    }

    public static class SaveResult {
        private WarehousePlan plan;
        private List<String> skippedDelete;

        public WarehousePlan getPlan() {
            return plan;
        }

        public void setPlan(WarehousePlan plan) {
            this.plan = plan;
        }

        public List<String> getSkippedDelete() {
            return skippedDelete;
        }

        public void setSkippedDelete(List<String> skippedDelete) {
            this.skippedDelete = skippedDelete;
        }
    }
}
