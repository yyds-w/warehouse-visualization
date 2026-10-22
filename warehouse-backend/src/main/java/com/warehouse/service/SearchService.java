package com.warehouse.service;

import com.warehouse.common.BusinessException;
import com.warehouse.mapper.LocationMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

@Service
public class SearchService {

    private final LocationMapper locationMapper;

    public SearchService(LocationMapper locationMapper) {
        this.locationMapper = locationMapper;
    }

    /**
     * 智能找货：按货位编号 / 商品名称 / SKU 编码模糊匹配，返回货位及其所属链路信息。
     */
    public List<Map<String, Object>> search(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            throw new BusinessException("搜索关键字不能为空");
        }
        return locationMapper.searchLocations(keyword.trim());
    }
}
