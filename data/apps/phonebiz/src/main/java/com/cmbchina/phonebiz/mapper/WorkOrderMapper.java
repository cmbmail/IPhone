package com.cmbchina.phonebiz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cmbchina.phonebiz.entity.WorkOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface WorkOrderMapper extends BaseMapper<WorkOrder> {

    List<WorkOrder> selectByStatus(@Param("status") String status);

    List<WorkOrder> selectByRequesterId(@Param("requesterId") Long requesterId);

    List<WorkOrder> selectByHandlerId(@Param("handlerId") Long handlerId);

    List<WorkOrder> selectByPhoneId(@Param("phoneId") Long phoneId);
}
