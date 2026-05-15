package com.cmbchina.phonebiz.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cmbchina.phonebiz.entity.SysMenu;
import com.cmbchina.phonebiz.entity.SysRole;
import com.cmbchina.phonebiz.entity.SysRoleMenu;
import com.cmbchina.phonebiz.mapper.SysMenuMapper;
import com.cmbchina.phonebiz.mapper.SysRoleMapper;
import com.cmbchina.phonebiz.mapper.SysRoleMenuMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class SysRoleService extends ServiceImpl<SysRoleMapper, SysRole> {

    @Autowired
    private SysRoleMenuMapper sysRoleMenuMapper;

    @Autowired
    private SysMenuMapper sysMenuMapper;

    public List<SysRole> getAllRoles() {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(SysRole::getSort);
        return list(wrapper);
    }

    public boolean addRole(SysRole role) {
        return save(role);
    }

    public boolean updateRole(SysRole role) {
        return updateById(role);
    }

    public boolean deleteRole(Long id) {
        return removeById(id);
    }

    public SysRole getRoleById(Long id) {
        return getById(id);
    }

    @Transactional
    public boolean assignMenus(Long roleId, List<Long> menuIds) {
        sysRoleMenuMapper.deleteByRoleId(roleId);
        if (menuIds != null && !menuIds.isEmpty()) {
            List<SysRoleMenu> list = new ArrayList<>();
            for (Long menuId : menuIds) {
                SysRoleMenu rm = new SysRoleMenu();
                rm.setRoleId(roleId);
                rm.setMenuId(menuId);
                list.add(rm);
            }
            sysRoleMenuMapper.insertBatch(list);
        }
        return true;
    }

    public List<SysMenu> getMenusByRoleId(Long roleId) {
        return sysMenuMapper.selectMenusByRoleId(roleId);
    }

    public List<Long> getMenuIdsByRoleId(Long roleId) {
        return sysRoleMenuMapper.selectMenuIdsByRoleId(roleId);
    }

    public List<SysMenu> getMenusByEmployeeId(Long employeeId) {
        return sysMenuMapper.selectMenusByEmployeeId(employeeId);
    }
}