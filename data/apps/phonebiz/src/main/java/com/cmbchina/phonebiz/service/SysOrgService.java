package com.cmbchina.phonebiz.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cmbchina.phonebiz.entity.SysOrg;
import com.cmbchina.phonebiz.mapper.SysOrgMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SysOrgService extends ServiceImpl<SysOrgMapper, SysOrg> {

    public List<SysOrg> getAllOrgs() {
        LambdaQueryWrapper<SysOrg> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(SysOrg::getSort);
        return list(wrapper);
    }

    public boolean addOrg(SysOrg org) {
        return save(org);
    }

    public boolean updateOrg(SysOrg org) {
        return updateById(org);
    }

    public boolean deleteOrg(Long id) {
        return removeById(id);
    }

    public SysOrg getOrgById(Long id) {
        return getById(id);
    }
}
