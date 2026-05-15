package com.cmbchina.phonebiz.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cmbchina.phonebiz.entity.PhoneNumber;
import com.cmbchina.phonebiz.enums.PhoneStatus;
import com.cmbchina.phonebiz.enums.PhoneStatusTransition;
import com.cmbchina.phonebiz.mapper.PhoneNumberMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Service
public class PhoneNumberService extends ServiceImpl<PhoneNumberMapper, PhoneNumber> {

    @Transactional
    public boolean addPhoneNumber(String phoneNumber, String remark) {
        PhoneNumber existing = baseMapper.selectByPhoneNumber(phoneNumber);
        if (existing != null) {
            log.warn("电话号码已存在: {}", phoneNumber);
            return false;
        }
        PhoneNumber phone = new PhoneNumber();
        phone.setPhoneNumber(phoneNumber);
        phone.setStatus(PhoneStatus.UNASSIGNED.name());
        phone.setRemark(remark);
        return save(phone);
    }

    @Transactional
    public boolean assignPhone(Long id, Long employeeId, String employeeName) {
        PhoneNumber phone = getById(id);
        if (phone == null) {
            log.warn("电话号码不存在: {}", id);
            return false;
        }
        PhoneStatus currentStatus = PhoneStatus.valueOf(phone.getStatus());
        PhoneStatus targetStatus = PhoneStatus.ASSIGNED;
        if (!PhoneStatusTransition.isValidTransition(currentStatus, targetStatus)) {
            log.warn("状态转换不允许: {} -> {}", currentStatus, targetStatus);
            return false;
        }
        phone.setStatus(targetStatus.name());
        phone.setEmployeeId(employeeId);
        phone.setEmployeeName(employeeName);
        return updateById(phone);
    }

    @Transactional
    public boolean activatePhone(Long id) {
        PhoneNumber phone = getById(id);
        if (phone == null) {
            log.warn("电话号码不存在: {}", id);
            return false;
        }
        PhoneStatus currentStatus = PhoneStatus.valueOf(phone.getStatus());
        PhoneStatus targetStatus = PhoneStatus.IN_USE;
        if (!PhoneStatusTransition.isValidTransition(currentStatus, targetStatus)) {
            log.warn("状态转换不允许: {} -> {}", currentStatus, targetStatus);
            return false;
        }
        phone.setStatus(targetStatus.name());
        return updateById(phone);
    }

    @Transactional
    public boolean suspendPhone(Long id) {
        PhoneNumber phone = getById(id);
        if (phone == null) {
            log.warn("电话号码不存在: {}", id);
            return false;
        }
        PhoneStatus currentStatus = PhoneStatus.valueOf(phone.getStatus());
        PhoneStatus targetStatus = PhoneStatus.SUSPENDED;
        if (!PhoneStatusTransition.isValidTransition(currentStatus, targetStatus)) {
            log.warn("状态转换不允许: {} -> {}", currentStatus, targetStatus);
            return false;
        }
        phone.setStatus(targetStatus.name());
        return updateById(phone);
    }

    @Transactional
    public boolean resumePhone(Long id) {
        PhoneNumber phone = getById(id);
        if (phone == null) {
            log.warn("电话号码不存在: {}", id);
            return false;
        }
        PhoneStatus currentStatus = PhoneStatus.valueOf(phone.getStatus());
        PhoneStatus targetStatus = PhoneStatus.IN_USE;
        if (!PhoneStatusTransition.isValidTransition(currentStatus, targetStatus)) {
            log.warn("状态转换不允许: {} -> {}", currentStatus, targetStatus);
            return false;
        }
        phone.setStatus(targetStatus.name());
        return updateById(phone);
    }

    @Transactional
    public boolean recyclePhone(Long id) {
        PhoneNumber phone = getById(id);
        if (phone == null) {
            log.warn("电话号码不存在: {}", id);
            return false;
        }
        PhoneStatus currentStatus = PhoneStatus.valueOf(phone.getStatus());
        PhoneStatus targetStatus = PhoneStatus.RECYCLED;
        if (!PhoneStatusTransition.isValidTransition(currentStatus, targetStatus)) {
            log.warn("状态转换不允许: {} -> {}", currentStatus, targetStatus);
            return false;
        }
        phone.setStatus(targetStatus.name());
        phone.setEmployeeId(null);
        phone.setEmployeeName(null);
        return updateById(phone);
    }

    @Transactional
    public boolean reassignPhone(Long id) {
        PhoneNumber phone = getById(id);
        if (phone == null) {
            log.warn("电话号码不存在: {}", id);
            return false;
        }
        PhoneStatus currentStatus = PhoneStatus.valueOf(phone.getStatus());
        PhoneStatus targetStatus = PhoneStatus.UNASSIGNED;
        if (!PhoneStatusTransition.isValidTransition(currentStatus, targetStatus)) {
            log.warn("状态转换不允许: {} -> {}", currentStatus, targetStatus);
            return false;
        }
        phone.setStatus(targetStatus.name());
        phone.setEmployeeId(null);
        phone.setEmployeeName(null);
        return updateById(phone);
    }

    public Page<PhoneNumber> getPhonePage(Integer pageNum, Integer pageSize, String phoneNumber, String status, Long employeeId) {
        Page<PhoneNumber> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<PhoneNumber> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(phoneNumber)) {
            wrapper.like(PhoneNumber::getPhoneNumber, phoneNumber);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(PhoneNumber::getStatus, status);
        }
        if (employeeId != null) {
            wrapper.eq(PhoneNumber::getEmployeeId, employeeId);
        }
        wrapper.orderByDesc(PhoneNumber::getCreateTime);
        return page(page, wrapper);
    }

    public PhoneNumber getPhoneById(Long id) {
        return getById(id);
    }

    public List<PhoneNumber> getPhonesByStatus(String status) {
        return baseMapper.selectByStatus(status);
    }

    public List<PhoneNumber> getPhonesByEmployeeId(Long employeeId) {
        return baseMapper.selectByEmployeeId(employeeId);
    }
}
