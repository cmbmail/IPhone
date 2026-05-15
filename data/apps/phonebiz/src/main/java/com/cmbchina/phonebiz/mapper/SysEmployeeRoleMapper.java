package com.cmbchina.phonebiz.mapper;

import com.cmbchina.phonebiz.entity.SysEmployeeRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SysEmployeeRoleMapper {

    void insertBatch(@Param("list") List<SysEmployeeRole> list);

    void deleteByEmployeeId(@Param("employeeId") Long employeeId);

    List<Long> selectRoleIdsByEmployeeId(@Param("employeeId") Long employeeId);
}