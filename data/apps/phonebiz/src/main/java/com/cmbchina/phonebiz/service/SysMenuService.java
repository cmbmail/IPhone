package com.cmbchina.phonebiz.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cmbchina.phonebiz.entity.SysMenu;
import com.cmbchina.phonebiz.mapper.SysMenuMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SysMenuService extends ServiceImpl<SysMenuMapper, SysMenu> {

    public List<SysMenu> getAllMenus() {
        LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(SysMenu::getSort);
        return list(wrapper);
    }

    public boolean addMenu(SysMenu menu) {
        return save(menu);
    }

    public boolean updateMenu(SysMenu menu) {
        return updateById(menu);
    }

    public boolean deleteMenu(Long id) {
        return removeById(id);
    }

    public SysMenu getMenuById(Long id) {
        return getById(id);
    }
}
