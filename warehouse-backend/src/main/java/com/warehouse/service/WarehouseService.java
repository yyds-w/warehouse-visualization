package com.warehouse.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.warehouse.common.BusinessException;
import com.warehouse.common.PageResult;
import com.warehouse.entity.Warehouse;
import com.warehouse.mapper.WarehouseMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class WarehouseService {

    private final WarehouseMapper warehouseMapper;

    public WarehouseService(WarehouseMapper warehouseMapper) {
        this.warehouseMapper = warehouseMapper;
    }

    public PageResult<Warehouse> page(String keyword, Integer status, long pageNum, long pageSize) {
        LambdaQueryWrapper<Warehouse> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            qw.and(w -> w.like(Warehouse::getName, keyword).or().like(Warehouse::getCode, keyword));
        }
        if (status != null) {
            qw.eq(Warehouse::getStatus, status);
        }
        qw.orderByDesc(Warehouse::getCreatedAt);
        Page<Warehouse> page = warehouseMapper.selectPage(new Page<>(pageNum, pageSize), qw);
        return PageResult.of(page);
    }

    public List<Warehouse> listAll() {
        return warehouseMapper.selectList(new LambdaQueryWrapper<Warehouse>()
                .eq(Warehouse::getStatus, 1).orderByAsc(Warehouse::getCode));
    }

    public Warehouse getById(Long id) {
        Warehouse w = warehouseMapper.selectById(id);
        if (w == null) {
            throw new BusinessException(404, "仓库不存在");
        }
        return w;
    }

    public Warehouse create(Warehouse warehouse) {
        checkCodeUnique(warehouse.getCode(), null);
        warehouse.setId(null);
        warehouseMapper.insert(warehouse);
        return warehouse;
    }

    public void update(Warehouse warehouse) {
        if (warehouse.getId() == null) {
            throw new BusinessException("仓库 id 不能为空");
        }
        getById(warehouse.getId());
        checkCodeUnique(warehouse.getCode(), warehouse.getId());
        warehouseMapper.updateById(warehouse);
    }

    public void delete(Long id) {
        getById(id);
        warehouseMapper.deleteById(id);
    }

    private void checkCodeUnique(String code, Long excludeId) {
        LambdaQueryWrapper<Warehouse> qw = new LambdaQueryWrapper<>();
        qw.eq(Warehouse::getCode, code);
        if (excludeId != null) {
            qw.ne(Warehouse::getId, excludeId);
        }
        if (warehouseMapper.selectCount(qw) > 0) {
            throw new BusinessException("仓库编码已存在：" + code);
        }
    }
}
