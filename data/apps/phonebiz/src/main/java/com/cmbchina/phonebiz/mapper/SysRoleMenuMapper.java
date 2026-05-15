package com.cmbchina.phonebiz.mapper;

import com.cmbchina.phonebiz.entity.SysRoleMenu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SysRoleMenuMapper {

    void insertBatch(@Param("list") List<SysRoleMenu> list);

    void deleteByRoleId(@Param("roleId") Long roleId);

    List<Long> selectMenuIdsByRoleId(@Param("roleId") Long roleId);
}