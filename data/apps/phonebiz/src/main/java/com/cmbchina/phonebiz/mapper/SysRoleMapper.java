package com.cmbchina.phonebiz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cmbchina.phonebiz.entity.SysRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {

    List<SysRole> selectRolesByEmployeeId(@Param("employeeId") Long employeeId);
}