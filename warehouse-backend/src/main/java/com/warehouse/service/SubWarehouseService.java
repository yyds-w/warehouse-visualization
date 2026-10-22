package com.warehouse.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.warehouse.common.BusinessException;
import com.warehouse.common.PageResult;
import com.warehouse.entity.SubWarehouse;
import com.warehouse.mapper.SubWarehouseMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class SubWarehouseService {

    private final SubWarehouseMapper subWarehouseMapper;

    public SubWarehouseService(SubWarehouseMapper subWarehouseMapper) {
        this.subWarehouseMapper = subWarehouseMapper;
    }

    public PageResult<SubWarehouse> page(Long warehouseId, String keyword, long pageNum, long pageSize) {
        LambdaQueryWrapper<SubWarehouse> qw = new LambdaQueryWrapper<>();
        if (warehouseId != null) {
            qw.eq(SubWarehouse::getWarehouseId, warehouseId);
        }
        if (StringUtils.hasText(keyword)) {
            qw.and(w -> w.like(SubWarehouse::getName, keyword).or().like(SubWarehouse::getCode, keyword));
        }
        qw.orderByAsc(SubWarehouse::getCode);
        return PageResult.of(subWarehouseMapper.selectPage(new Page<>(pageNum, pageSize), qw));
    }

    public List<SubWarehouse> listByWarehouse(Long warehouseId) {
        return subWarehouseMapper.selectList(new LambdaQueryWrapper<SubWarehouse>()
                .eq(SubWarehouse::getWarehouseId, warehouseId)
                .eq(SubWarehouse::getStatus, 1)
                .orderByAsc(SubWarehouse::getCode));
    }

    public SubWarehouse getById(Long id) {
        SubWarehouse sw = subWarehouseMapper.selectById(id);
        if (sw == null) {
            throw new BusinessException(404, "子仓库不存在");
        }
        return sw;
    }

    public SubWarehouse create(SubWarehouse subWarehouse) {
        checkCodeUnique(subWarehouse.getWarehouseId(), subWarehouse.getCode(), null);
        subWarehouse.setId(null);
        subWarehouseMapper.insert(subWarehouse);
        return subWarehouse;
    }

    public void update(SubWarehouse subWarehouse) {
        if (subWarehouse.getId() == null) {
            throw new BusinessException("子仓库 id 不能为空");
        }
        getById(subWarehouse.getId());
        checkCodeUnique(subWarehouse.getWarehouseId(), subWarehouse.getCode(), subWarehouse.getId());
        subWarehouseMapper.updateById(subWarehouse);
    }

    public void delete(Long id) {
        getById(id);
        subWarehouseMapper.deleteById(id);
    }

    private void checkCodeUnique(Long warehouseId, String code, Long excludeId) {
        LambdaQueryWrapper<SubWarehouse> qw = new LambdaQueryWrapper<>();
        qw.eq(SubWarehouse::getWarehouseId, warehouseId).eq(SubWarehouse::getCode, code);
        if (excludeId != null) {
            qw.ne(SubWarehouse::getId, excludeId);
        }
        if (subWarehouseMapper.selectCount(qw) > 0) {
            throw new BusinessException("该仓库下子仓库编码已存在：" + code);
        }
    }
}
