package com.warehouse.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.warehouse.common.BusinessException;
import com.warehouse.common.PageResult;
import com.warehouse.entity.WarehouseUser;
import com.warehouse.mapper.WarehouseUserMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class UserService {

    private final WarehouseUserMapper userMapper;

    public UserService(WarehouseUserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public PageResult<WarehouseUser> page(String keyword, Integer role, long pageNum, long pageSize) {
        LambdaQueryWrapper<WarehouseUser> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            qw.and(w -> w.like(WarehouseUser::getUsername, keyword)
                    .or().like(WarehouseUser::getRealName, keyword));
        }
        if (role != null) {
            qw.eq(WarehouseUser::getRole, role);
        }
        qw.orderByAsc(WarehouseUser::getId);
        return PageResult.of(userMapper.selectPage(new Page<>(pageNum, pageSize), qw));
    }

    public List<WarehouseUser> listAll() {
        return userMapper.selectList(new LambdaQueryWrapper<WarehouseUser>()
                .eq(WarehouseUser::getStatus, 1).orderByAsc(WarehouseUser::getId));
    }

    public WarehouseUser getById(Long id) {
        WarehouseUser u = userMapper.selectById(id);
        if (u == null) {
            throw new BusinessException(404, "用户不存在");
        }
        return u;
    }

    public WarehouseUser create(WarehouseUser user) {
        checkUsernameUnique(user.getUsername(), null);
        user.setId(null);
        userMapper.insert(user);
        return user;
    }

    public void update(WarehouseUser user) {
        if (user.getId() == null) {
            throw new BusinessException("用户 id 不能为空");
        }
        getById(user.getId());
        checkUsernameUnique(user.getUsername(), user.getId());
        userMapper.updateById(user);
    }

    public void delete(Long id) {
        getById(id);
        userMapper.deleteById(id);
    }

    private void checkUsernameUnique(String username, Long excludeId) {
        LambdaQueryWrapper<WarehouseUser> qw = new LambdaQueryWrapper<>();
        qw.eq(WarehouseUser::getUsername, username);
        if (excludeId != null) {
            qw.ne(WarehouseUser::getId, excludeId);
        }
        if (userMapper.selectCount(qw) > 0) {
            throw new BusinessException("用户名已存在：" + username);
        }
    }
}
