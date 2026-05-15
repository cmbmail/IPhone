package com.cmbchina.phonebiz.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cmbchina.phonebiz.entity.SysEmployee;
import com.cmbchina.phonebiz.entity.SysEmployeeRole;
import com.cmbchina.phonebiz.entity.SysRole;
import com.cmbchina.phonebiz.mapper.SysEmployeeMapper;
import com.cmbchina.phonebiz.mapper.SysEmployeeRoleMapper;
import com.cmbchina.phonebiz.mapper.SysRoleMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
public class SysEmployeeService extends ServiceImpl<SysEmployeeMapper, SysEmployee> {

    @Autowired
    private SysEmployeeRoleMapper sysEmployeeRoleMapper;

    @Autowired
    private SysRoleMapper sysRoleMapper;

    public Page<SysEmployee> getEmployeePage(Integer pageNum, Integer pageSize, String realName, Long orgId) {
        Page<SysEmployee> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysEmployee> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(realName)) {
            wrapper.like(SysEmployee::getRealName, realName);
        }
        if (orgId != null) {
            wrapper.eq(SysEmployee::getOrgId, orgId);
        }
        wrapper.orderByDesc(SysEmployee::getCreateTime);
        return page(page, wrapper);
    }

    public boolean addEmployee(SysEmployee employee) {
        return save(employee);
    }

    public boolean updateEmployee(SysEmployee employee) {
        return updateById(employee);
    }

    public boolean deleteEmployee(Long id) {
        return removeById(id);
    }

    public SysEmployee getEmployeeById(Long id) {
        return getById(id);
    }

    public SysEmployee getEmployeeByUsername(String username) {
        return baseMapper.selectByUsername(username);
    }

    @Transactional
    public boolean assignRoles(Long employeeId, List<Long> roleIds) {
        sysEmployeeRoleMapper.deleteByEmployeeId(employeeId);
        if (roleIds != null && !roleIds.isEmpty()) {
            List<SysEmployeeRole> list = new ArrayList<>();
            for (Long roleId : roleIds) {
                SysEmployeeRole er = new SysEmployeeRole();
                er.setEmployeeId(employeeId);
                er.setRoleId(roleId);
                list.add(er);
            }
            sysEmployeeRoleMapper.insertBatch(list);
        }
        return true;
    }

    public List<SysRole> getRolesByEmployeeId(Long employeeId) {
        return sysRoleMapper.selectRolesByEmployeeId(employeeId);
    }

    public List<Long> getRoleIdsByEmployeeId(Long employeeId) {
        return sysEmployeeRoleMapper.selectRoleIdsByEmployeeId(employeeId);
    }
}