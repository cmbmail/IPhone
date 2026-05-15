package com.cmbchina.phonebiz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cmbchina.phonebiz.entity.SysMenu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SysMenuMapper extends BaseMapper<SysMenu> {

    List<SysMenu> selectMenusByRoleId(@Param("roleId") Long roleId);

    List<SysMenu> selectMenusByEmployeeId(@Param("employeeId") Long employeeId);
}