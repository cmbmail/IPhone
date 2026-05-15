package com.cmbchina.phonebiz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cmbchina.phonebiz.entity.PhoneNumber;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PhoneNumberMapper extends BaseMapper<PhoneNumber> {

    List<PhoneNumber> selectByStatus(@Param("status") String status);

    List<PhoneNumber> selectByEmployeeId(@Param("employeeId") Long employeeId);

    PhoneNumber selectByPhoneNumber(@Param("phoneNumber") String phoneNumber);
}
