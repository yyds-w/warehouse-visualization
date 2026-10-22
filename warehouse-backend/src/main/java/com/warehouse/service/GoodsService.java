package com.warehouse.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.warehouse.common.BusinessException;
import com.warehouse.common.PageResult;
import com.warehouse.entity.Goods;
import com.warehouse.mapper.GoodsMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class GoodsService {

    private final GoodsMapper goodsMapper;

    public GoodsService(GoodsMapper goodsMapper) {
        this.goodsMapper = goodsMapper;
    }

    public PageResult<Goods> page(String keyword, String category, long pageNum, long pageSize) {
        LambdaQueryWrapper<Goods> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            qw.and(w -> w.like(Goods::getName, keyword).or().like(Goods::getSkuCode, keyword));
        }
        if (StringUtils.hasText(category)) {
            qw.eq(Goods::getCategory, category);
        }
        qw.orderByAsc(Goods::getSkuCode);
        return PageResult.of(goodsMapper.selectPage(new Page<>(pageNum, pageSize), qw));
    }

    public List<Goods> listAll() {
        return goodsMapper.selectList(new LambdaQueryWrapper<Goods>()
                .eq(Goods::getStatus, 1).orderByAsc(Goods::getSkuCode));
    }

    public Goods getById(Long id) {
        Goods goods = goodsMapper.selectById(id);
        if (goods == null) {
            throw new BusinessException(404, "商品不存在");
        }
        return goods;
    }

    public Goods create(Goods goods) {
        checkSkuUnique(goods.getSkuCode(), null);
        goods.setId(null);
        goodsMapper.insert(goods);
        return goods;
    }

    public void update(Goods goods) {
        if (goods.getId() == null) {
            throw new BusinessException("商品 id 不能为空");
        }
        getById(goods.getId());
        checkSkuUnique(goods.getSkuCode(), goods.getId());
        goodsMapper.updateById(goods);
    }

    public void delete(Long id) {
        getById(id);
        goodsMapper.deleteById(id);
    }

    private void checkSkuUnique(String skuCode, Long excludeId) {
        LambdaQueryWrapper<Goods> qw = new LambdaQueryWrapper<>();
        qw.eq(Goods::getSkuCode, skuCode);
        if (excludeId != null) {
            qw.ne(Goods::getId, excludeId);
        }
        if (goodsMapper.selectCount(qw) > 0) {
            throw new BusinessException("SKU 编码已存在：" + skuCode);
        }
    }
}
