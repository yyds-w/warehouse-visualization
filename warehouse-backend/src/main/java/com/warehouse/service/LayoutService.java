package com.warehouse.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.warehouse.common.BusinessException;
import com.warehouse.entity.Warehouse;
import com.warehouse.entity.WarehouseLayout;
import com.warehouse.mapper.WarehouseLayoutMapper;
import com.warehouse.mapper.WarehouseMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;

@Service
public class LayoutService {

    private final WarehouseLayoutMapper layoutMapper;
    private final WarehouseMapper warehouseMapper;

    public LayoutService(WarehouseLayoutMapper layoutMapper, WarehouseMapper warehouseMapper) {
        this.layoutMapper = layoutMapper;
        this.warehouseMapper = warehouseMapper;
    }

    public WarehouseLayout getByWarehouse(Long warehouseId) {
        return layoutMapper.selectOne(new LambdaQueryWrapper<WarehouseLayout>()
                .eq(WarehouseLayout::getWarehouseId, warehouseId)
                .orderByDesc(WarehouseLayout::getId)
                .last("LIMIT 1"));
    }

    @Transactional
    public WarehouseLayout save(WarehouseLayout layout) {
        if (layout.getWarehouseId() == null) {
            throw new BusinessException("所属仓库不能为空");
        }
        Warehouse warehouse = warehouseMapper.selectById(layout.getWarehouseId());
        if (warehouse == null) {
            throw new BusinessException("所属仓库不存在");
        }
        if (layout.getId() != null && layoutMapper.selectById(layout.getId()) != null) {
            layoutMapper.updateById(layout);
        } else {
            layout.setId(null);
            if (layout.getSourceType() == null) {
                layout.setSourceType(1);
            }
            if (layout.getStatus() == null) {
                layout.setStatus(1);
            }
            layoutMapper.insert(layout);
        }
        warehouse.setLayoutId(layout.getId());
        warehouseMapper.updateById(warehouse);
        return layout;
    }

    /**
     * 导入 CAD/手绘图：保存背景图并生成占位平面图元数据。
     * 说明：真实的 CAD 解析 / OCR 识别需要接入第三方识别服务，
     * 本实现保留背景图与空元素结构，前端可在背景图上继续框选编辑。
     */
    @Transactional
    public WarehouseLayout importLayout(Long warehouseId, String name, Integer sourceType, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }
        Warehouse warehouse = warehouseMapper.selectById(warehouseId);
        if (warehouse == null) {
            throw new BusinessException("所属仓库不存在");
        }
        String dataUrl;
        try {
            String contentType = file.getContentType() == null ? "image/png" : file.getContentType();
            dataUrl = "data:" + contentType + ";base64," + Base64.getEncoder().encodeToString(file.getBytes());
        } catch (IOException e) {
            throw new BusinessException("读取上传文件失败：" + e.getMessage());
        }
        WarehouseLayout layout = new WarehouseLayout();
        layout.setWarehouseId(warehouseId);
        layout.setName(name == null || name.isBlank() ? file.getOriginalFilename() : name);
        layout.setWidth(new java.math.BigDecimal("1200"));
        layout.setHeight(new java.math.BigDecimal("800"));
        layout.setBgImage(dataUrl);
        layout.setLayoutData("{\"elements\":[]}");
        layout.setSourceType(sourceType == null ? 2 : sourceType);
        layout.setStatus(1);
        layoutMapper.insert(layout);
        warehouse.setLayoutId(layout.getId());
        warehouseMapper.updateById(warehouse);
        return layout;
    }
}
