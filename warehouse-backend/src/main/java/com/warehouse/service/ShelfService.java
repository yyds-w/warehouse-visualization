package com.warehouse.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.warehouse.common.BusinessException;
import com.warehouse.common.PageResult;
import com.warehouse.dto.ShelfBatchRequest;
import com.warehouse.dto.ShelfSaveRequest;
import com.warehouse.entity.Location;
import com.warehouse.entity.Shelf;
import com.warehouse.entity.SubWarehouse;
import com.warehouse.entity.Warehouse;
import com.warehouse.mapper.InventoryMapper;
import com.warehouse.mapper.LocationMapper;
import com.warehouse.mapper.ShelfMapper;
import com.warehouse.mapper.SubWarehouseMapper;
import com.warehouse.mapper.WarehouseMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class ShelfService {

    private final ShelfMapper shelfMapper;
    private final SubWarehouseMapper subWarehouseMapper;
    private final WarehouseMapper warehouseMapper;
    private final LocationMapper locationMapper;
    private final InventoryMapper inventoryMapper;

    public ShelfService(ShelfMapper shelfMapper, SubWarehouseMapper subWarehouseMapper,
                        WarehouseMapper warehouseMapper, LocationMapper locationMapper,
                        InventoryMapper inventoryMapper) {
        this.shelfMapper = shelfMapper;
        this.subWarehouseMapper = subWarehouseMapper;
        this.warehouseMapper = warehouseMapper;
        this.locationMapper = locationMapper;
        this.inventoryMapper = inventoryMapper;
    }

    public PageResult<Shelf> page(Long warehouseId, Long subWarehouseId, String keyword, long pageNum, long pageSize) {
        LambdaQueryWrapper<Shelf> qw = new LambdaQueryWrapper<>();
        if (warehouseId != null) {
            qw.eq(Shelf::getWarehouseId, warehouseId);
        }
        if (subWarehouseId != null) {
            qw.eq(Shelf::getSubWarehouseId, subWarehouseId);
        }
        if (StringUtils.hasText(keyword)) {
            qw.and(w -> w.like(Shelf::getName, keyword).or().like(Shelf::getCode, keyword));
        }
        qw.orderByAsc(Shelf::getCode);
        return PageResult.of(shelfMapper.selectPage(new Page<>(pageNum, pageSize), qw));
    }

    public List<Shelf> listBySub(Long subWarehouseId) {
        return shelfMapper.selectList(new LambdaQueryWrapper<Shelf>()
                .eq(Shelf::getSubWarehouseId, subWarehouseId)
                .orderByAsc(Shelf::getCode));
    }

    public Shelf getById(Long id) {
        Shelf shelf = shelfMapper.selectById(id);
        if (shelf == null) {
            throw new BusinessException(404, "货架不存在");
        }
        return shelf;
    }

    @Transactional
    public Map<String, Object> batchCreate(ShelfBatchRequest req) {
        Warehouse warehouse = warehouseMapper.selectById(req.getWarehouseId());
        if (warehouse == null) {
            throw new BusinessException("所属仓库不存在");
        }
        SubWarehouse subWarehouse = subWarehouseMapper.selectById(req.getSubWarehouseId());
        if (subWarehouse == null || !subWarehouse.getWarehouseId().equals(req.getWarehouseId())) {
            throw new BusinessException("子仓库不存在或不属于所选仓库");
        }
        LambdaQueryWrapper<Shelf> qw = new LambdaQueryWrapper<>();
        qw.eq(Shelf::getSubWarehouseId, req.getSubWarehouseId()).eq(Shelf::getCode, req.getCode());
        if (shelfMapper.selectCount(qw) > 0) {
            throw new BusinessException("该子仓库下货架编码已存在：" + req.getCode());
        }
        Shelf shelf = new Shelf();
        BeanUtils.copyProperties(req, shelf);
        shelf.setId(null);
        if (shelf.getStatus() == null) {
            shelf.setStatus(1);
        }
        shelfMapper.insert(shelf);
        int locationCount = generateLocations(shelf);
        Map<String, Object> result = new HashMap<>();
        result.put("shelf", shelf);
        result.put("locationCount", locationCount);
        return result;
    }

    @Transactional
    public void update(Shelf shelf) {
        if (shelf.getId() == null) {
            throw new BusinessException("货架 id 不能为空");
        }
        Shelf old = getById(shelf.getId());
        boolean structureChanged = !Objects.equals(old.getRowCount(), shelf.getRowCount())
                || !Objects.equals(old.getBayCount(), shelf.getBayCount())
                || !Objects.equals(old.getColCount(), shelf.getColCount())
                || !Objects.equals(old.getLayerCount(), shelf.getLayerCount());
        if (structureChanged) {
            checkNoStock(shelf.getId());
            shelfMapper.updateById(shelf);
            locationMapper.physicalDeleteByShelf(shelf.getId());
            generateLocations(shelf);
        } else {
            shelfMapper.updateById(shelf);
        }
    }

    @Transactional
    public void delete(Long id) {
        getById(id);
        checkNoStock(id);
        locationMapper.physicalDeleteByShelf(id);
        shelfMapper.deleteById(id);
    }

    /**
     * 货架配置页「保存」：把前端画布上的工作副本整体同步到库。
     * removedIds 中的货架先删（释放其编码），再逐个创建/更新 shelves。
     */
    @Transactional
    public Map<String, Object> saveAll(ShelfSaveRequest req) {
        Warehouse warehouse = warehouseMapper.selectById(req.getWarehouseId());
        if (warehouse == null) {
            throw new BusinessException("所属仓库不存在");
        }
        SubWarehouse subWarehouse = subWarehouseMapper.selectById(req.getSubWarehouseId());
        if (subWarehouse == null || !subWarehouse.getWarehouseId().equals(req.getWarehouseId())) {
            throw new BusinessException("子仓库不存在或不属于所选仓库");
        }
        int created = 0;
        int updated = 0;
        int removed = 0;
        // 1. 删除（先删，编码随之释放，可被新货架复用）
        if (req.getRemovedIds() != null) {
            for (Long id : req.getRemovedIds()) {
                if (id == null) {
                    continue;
                }
                delete(id);
                removed++;
            }
        }
        // 2. 请求内部编码查重
        List<Shelf> shelves = req.getShelves() == null ? new ArrayList<>() : req.getShelves();
        java.util.Set<String> codes = new java.util.HashSet<>();
        for (Shelf s : shelves) {
            if (s.getCode() != null && !codes.add(s.getCode())) {
                throw new BusinessException("货架编码重复：" + s.getCode());
            }
        }
        // 3. 与库中现有货架编码查重（排除本次更新/删除的对象）
        for (Shelf s : shelves) {
            LambdaQueryWrapper<Shelf> qw = new LambdaQueryWrapper<>();
            qw.eq(Shelf::getSubWarehouseId, req.getSubWarehouseId()).eq(Shelf::getCode, s.getCode());
            if (s.getId() != null) {
                qw.ne(Shelf::getId, s.getId());
            }
            if (shelfMapper.selectCount(qw) > 0) {
                throw new BusinessException("该子仓库下货架编码已存在：" + s.getCode());
            }
        }
        // 4. 逐个创建 / 更新
        for (Shelf s : shelves) {
            if (s.getId() == null) {
                s.setWarehouseId(req.getWarehouseId());
                s.setSubWarehouseId(req.getSubWarehouseId());
                if (s.getStatus() == null) {
                    s.setStatus(1);
                }
                shelfMapper.insert(s);
                generateLocations(s);
                created++;
            } else {
                update(s);
                updated++;
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("created", created);
        result.put("updated", updated);
        result.put("removed", removed);
        return result;
    }

    private void checkNoStock(Long shelfId) {
        LambdaQueryWrapper<Location> qw = new LambdaQueryWrapper<>();
        qw.eq(Location::getShelfId, shelfId).eq(Location::getStatus, 2);
        if (locationMapper.selectCount(qw) > 0) {
            throw new BusinessException("货架下存在占用中的货位，请先清空库存");
        }
    }

    /**
     * 按货架参数（行 × 排 × 列 × 层）批量生成货位，返回生成数量。
     * 货位编码规则：仓库编码-子仓编码-货架编码-L层-R行-B排-C列
     */
    public int generateLocations(Shelf shelf) {
        Warehouse warehouse = warehouseMapper.selectById(shelf.getWarehouseId());
        SubWarehouse subWarehouse = subWarehouseMapper.selectById(shelf.getSubWarehouseId());
        String prefix = warehouse.getCode() + "-" + subWarehouse.getCode() + "-" + shelf.getCode();
        BigDecimal cellW = null;
        BigDecimal cellH = null;
        if (shelf.getWidth() != null && shelf.getColCount() != null && shelf.getColCount() > 0) {
            cellW = shelf.getWidth().divide(BigDecimal.valueOf(shelf.getColCount()), 2, RoundingMode.HALF_UP);
        }
        if (shelf.getHeight() != null && shelf.getRowCount() != null && shelf.getRowCount() > 0) {
            cellH = shelf.getHeight().divide(BigDecimal.valueOf(shelf.getRowCount()), 2, RoundingMode.HALF_UP);
        }
        int count = 0;
        for (int layer = 1; layer <= shelf.getLayerCount(); layer++) {
            for (int row = 1; row <= shelf.getRowCount(); row++) {
                for (int bay = 1; bay <= shelf.getBayCount(); bay++) {
                    for (int col = 1; col <= shelf.getColCount(); col++) {
                        Location loc = new Location();
                        loc.setWarehouseId(shelf.getWarehouseId());
                        loc.setSubWarehouseId(shelf.getSubWarehouseId());
                        loc.setShelfId(shelf.getId());
                        loc.setLocationCode(String.format("%s-L%d-R%d-B%d-C%d", prefix, layer, row, bay, col));
                        loc.setLayerIndex(layer);
                        loc.setRowIndex(row);
                        loc.setBayIndex(bay);
                        loc.setColIndex(col);
                        if (shelf.getPositionX() != null && cellW != null) {
                            loc.setPositionX(shelf.getPositionX().add(cellW.multiply(BigDecimal.valueOf(col - 1L))));
                        }
                        if (shelf.getPositionY() != null && cellH != null) {
                            loc.setPositionY(shelf.getPositionY().add(cellH.multiply(BigDecimal.valueOf(row - 1L))));
                        }
                        loc.setStatus(1);
                        loc.setCapacity(new BigDecimal("100"));
                        locationMapper.insert(loc);
                        count++;
                    }
                }
            }
        }
        return count;
    }
}
