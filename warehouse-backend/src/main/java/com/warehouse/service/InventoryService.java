package com.warehouse.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.warehouse.common.BusinessException;
import com.warehouse.common.PageResult;
import com.warehouse.dto.InboundRequest;
import com.warehouse.entity.Goods;
import com.warehouse.entity.Inventory;
import com.warehouse.entity.Location;
import com.warehouse.mapper.GoodsMapper;
import com.warehouse.mapper.InventoryMapper;
import com.warehouse.mapper.LocationMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Map;

@Service
public class InventoryService {

    private final InventoryMapper inventoryMapper;
    private final LocationMapper locationMapper;
    private final GoodsMapper goodsMapper;

    public InventoryService(InventoryMapper inventoryMapper, LocationMapper locationMapper,
                            GoodsMapper goodsMapper) {
        this.inventoryMapper = inventoryMapper;
        this.locationMapper = locationMapper;
        this.goodsMapper = goodsMapper;
    }

    public PageResult<Map<String, Object>> page(String keyword, long pageNum, long pageSize) {
        String kw = StringUtils.hasText(keyword) ? keyword : null;
        IPage<Map<String, Object>> page = inventoryMapper.pageWithDetail(new Page<>(pageNum, pageSize), kw);
        PageResult<Map<String, Object>> result = new PageResult<>();
        result.setList(page.getRecords());
        result.setTotal(page.getTotal());
        result.setPageNum(page.getCurrent());
        result.setPageSize(page.getSize());
        result.setPages(page.getPages());
        return result;
    }

    @Transactional
    public Inventory inbound(InboundRequest req) {
        Location location = locationMapper.selectById(req.getLocationId());
        if (location == null) {
            throw new BusinessException(404, "货位不存在");
        }
        if (location.getStatus() != null && (location.getStatus() == 0 || location.getStatus() == 3)) {
            throw new BusinessException("货位已禁用或锁定，无法入库");
        }
        Goods goods = goodsMapper.selectById(req.getGoodsId());
        if (goods == null) {
            throw new BusinessException(404, "商品不存在");
        }
        LambdaQueryWrapper<Inventory> qw = new LambdaQueryWrapper<>();
        qw.eq(Inventory::getLocationId, req.getLocationId()).eq(Inventory::getGoodsId, req.getGoodsId());
        if (StringUtils.hasText(req.getBatchNo())) {
            qw.eq(Inventory::getBatchNo, req.getBatchNo());
        } else {
            qw.isNull(Inventory::getBatchNo);
        }
        Inventory existing = inventoryMapper.selectOne(qw.last("LIMIT 1"));
        Inventory saved;
        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + req.getQuantity());
            inventoryMapper.updateById(existing);
            saved = existing;
        } else {
            Inventory inv = new Inventory();
            inv.setLocationId(req.getLocationId());
            inv.setGoodsId(req.getGoodsId());
            inv.setQuantity(req.getQuantity());
            inv.setBatchNo(req.getBatchNo());
            inv.setStatus(1);
            inventoryMapper.insert(inv);
            saved = inv;
        }
        if (location.getStatus() == null || location.getStatus() != 2) {
            location.setStatus(2);
            locationMapper.updateById(location);
        }
        return saved;
    }

    @Transactional
    public void adjust(Long id, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new BusinessException("调整数量必须大于 0");
        }
        Inventory inv = inventoryMapper.selectById(id);
        if (inv == null) {
            throw new BusinessException(404, "库存记录不存在");
        }
        inv.setQuantity(quantity);
        inventoryMapper.updateById(inv);
    }

    @Transactional
    public void delete(Long id) {
        Inventory inv = inventoryMapper.selectById(id);
        if (inv == null) {
            throw new BusinessException(404, "库存记录不存在");
        }
        inventoryMapper.deleteById(id);
        LambdaQueryWrapper<Inventory> qw = new LambdaQueryWrapper<>();
        qw.eq(Inventory::getLocationId, inv.getLocationId());
        if (inventoryMapper.selectCount(qw) == 0) {
            Location location = locationMapper.selectById(inv.getLocationId());
            if (location != null && location.getStatus() != null && location.getStatus() == 2) {
                location.setStatus(1);
                locationMapper.updateById(location);
            }
        }
    }
}
