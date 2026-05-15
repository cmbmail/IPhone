package com.cmbchina.phonebiz.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cmbchina.phonebiz.entity.WorkOrder;
import com.cmbchina.phonebiz.enums.WorkOrderStatus;
import com.cmbchina.phonebiz.enums.WorkOrderStatusTransition;
import com.cmbchina.phonebiz.mapper.WorkOrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
public class WorkOrderService extends ServiceImpl<WorkOrderMapper, WorkOrder> {

    private static final AtomicLong ORDER_SEQ = new AtomicLong(0);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private String generateOrderNo() {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        long seq = ORDER_SEQ.incrementAndGet() % 10000;
        return "WO" + timestamp + String.format("%04d", seq);
    }

    @Transactional
    public boolean createOrder(WorkOrder order) {
        order.setOrderNo(generateOrderNo());
        order.setStatus(WorkOrderStatus.PENDING.name());
        return save(order);
    }

    @Transactional
    public boolean acceptOrder(Long id, Long handlerId, String handlerName) {
        WorkOrder order = getById(id);
        if (order == null) {
            log.warn("工单不存在: {}", id);
            return false;
        }
        WorkOrderStatus currentStatus = WorkOrderStatus.valueOf(order.getStatus());
        WorkOrderStatus targetStatus = WorkOrderStatus.PROCESSING;
        if (!WorkOrderStatusTransition.isValidTransition(currentStatus, targetStatus)) {
            log.warn("状态转换不允许: {} -> {}", currentStatus, targetStatus);
            return false;
        }
        order.setStatus(targetStatus.name());
        order.setHandlerId(handlerId);
        order.setHandlerName(handlerName);
        return updateById(order);
    }

    @Transactional
    public boolean approveOrder(Long id, String result) {
        WorkOrder order = getById(id);
        if (order == null) {
            log.warn("工单不存在: {}", id);
            return false;
        }
        WorkOrderStatus currentStatus = WorkOrderStatus.valueOf(order.getStatus());
        WorkOrderStatus targetStatus = WorkOrderStatus.APPROVED;
        if (!WorkOrderStatusTransition.isValidTransition(currentStatus, targetStatus)) {
            log.warn("状态转换不允许: {} -> {}", currentStatus, targetStatus);
            return false;
        }
        order.setStatus(targetStatus.name());
        order.setResult(result);
        return updateById(order);
    }

    @Transactional
    public boolean rejectOrder(Long id, String reason) {
        WorkOrder order = getById(id);
        if (order == null) {
            log.warn("工单不存在: {}", id);
            return false;
        }
        WorkOrderStatus currentStatus = WorkOrderStatus.valueOf(order.getStatus());
        WorkOrderStatus targetStatus = WorkOrderStatus.REJECTED;
        if (!WorkOrderStatusTransition.isValidTransition(currentStatus, targetStatus)) {
            log.warn("状态转换不允许: {} -> {}", currentStatus, targetStatus);
            return false;
        }
        order.setStatus(targetStatus.name());
        order.setReason(reason);
        return updateById(order);
    }

    @Transactional
    public boolean completeOrder(Long id, String result) {
        WorkOrder order = getById(id);
        if (order == null) {
            log.warn("工单不存在: {}", id);
            return false;
        }
        WorkOrderStatus currentStatus = WorkOrderStatus.valueOf(order.getStatus());
        WorkOrderStatus targetStatus = WorkOrderStatus.COMPLETED;
        if (!WorkOrderStatusTransition.isValidTransition(currentStatus, targetStatus)) {
            log.warn("状态转换不允许: {} -> {}", currentStatus, targetStatus);
            return false;
        }
        order.setStatus(targetStatus.name());
        order.setResult(result);
        return updateById(order);
    }

    @Transactional
    public boolean cancelOrder(Long id) {
        WorkOrder order = getById(id);
        if (order == null) {
            log.warn("工单不存在: {}", id);
            return false;
        }
        WorkOrderStatus currentStatus = WorkOrderStatus.valueOf(order.getStatus());
        WorkOrderStatus targetStatus = WorkOrderStatus.CANCELLED;
        if (!WorkOrderStatusTransition.isValidTransition(currentStatus, targetStatus)) {
            log.warn("状态转换不允许: {} -> {}", currentStatus, targetStatus);
            return false;
        }
        order.setStatus(targetStatus.name());
        return updateById(order);
    }

    @Transactional
    public boolean reopenOrder(Long id) {
        WorkOrder order = getById(id);
        if (order == null) {
            log.warn("工单不存在: {}", id);
            return false;
        }
        WorkOrderStatus currentStatus = WorkOrderStatus.valueOf(order.getStatus());
        WorkOrderStatus targetStatus = WorkOrderStatus.PENDING;
        if (!WorkOrderStatusTransition.isValidTransition(currentStatus, targetStatus)) {
            log.warn("状态转换不允许: {} -> {}", currentStatus, targetStatus);
            return false;
        }
        order.setStatus(targetStatus.name());
        order.setHandlerId(null);
        order.setHandlerName(null);
        return updateById(order);
    }

    public Page<WorkOrder> getOrderPage(Integer pageNum, Integer pageSize, String orderNo, String status,
                                        Long requesterId, Long handlerId, String orderType) {
        Page<WorkOrder> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<WorkOrder> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(orderNo)) {
            wrapper.like(WorkOrder::getOrderNo, orderNo);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(WorkOrder::getStatus, status);
        }
        if (requesterId != null) {
            wrapper.eq(WorkOrder::getRequesterId, requesterId);
        }
        if (handlerId != null) {
            wrapper.eq(WorkOrder::getHandlerId, handlerId);
        }
        if (StringUtils.hasText(orderType)) {
            wrapper.eq(WorkOrder::getOrderType, orderType);
        }
        wrapper.orderByDesc(WorkOrder::getCreateTime);
        return page(page, wrapper);
    }

    public WorkOrder getOrderById(Long id) {
        return getById(id);
    }

    public List<WorkOrder> getOrdersByStatus(String status) {
        return baseMapper.selectByStatus(status);
    }

    public List<WorkOrder> getOrdersByRequesterId(Long requesterId) {
        return baseMapper.selectByRequesterId(requesterId);
    }

    public List<WorkOrder> getOrdersByHandlerId(Long handlerId) {
        return baseMapper.selectByHandlerId(handlerId);
    }
}
