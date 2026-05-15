package com.cmbchina.phonebiz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cmbchina.phonebiz.entity.SysEmployee;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SysEmployeeMapper extends BaseMapper<SysEmployee> {

    SysEmployee selectByUsername(@Param("username") String username);
}
