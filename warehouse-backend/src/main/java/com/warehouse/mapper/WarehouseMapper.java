package com.warehouse.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.warehouse.entity.Warehouse;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface WarehouseMapper extends BaseMapper<Warehouse> {

    /**
     * 含逻辑删除行的编码计数：code 唯一索引不区分 deleted，
     * 自动生成编码时必须绕过 MyBatis-Plus 的 deleted=0 过滤，否则会撞唯一键。
     */
    @Select("SELECT COUNT(*) FROM warehouse WHERE code = #{code}")
    int countByCodeIncludingDeleted(@Param("code") String code);
}
