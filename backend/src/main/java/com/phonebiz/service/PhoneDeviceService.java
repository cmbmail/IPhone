package com.phonebiz.service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.phonebiz.common.BusinessException;
import com.phonebiz.common.ErrorCode;
import com.phonebiz.dto.*;
import com.phonebiz.entity.*;
import com.phonebiz.repository.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PhoneDeviceService {
    private final PhoneDeviceRepository phoneDeviceRepository;
    private final DevicePhoneMappingRepository devicePhoneMappingRepository;
    private final PhoneDeviceHistoryRepository phoneDeviceHistoryRepository;
    private final PhoneNumberRepository phoneNumberRepository;
    private final EmployeeRepository employeeRepository;
    private final OrgStructureRepository orgStructureRepository;

    public Page<PhoneDeviceDTO> getDeviceList(List<Long> orgIds, PhoneDevice.PhoneDeviceStatus status, Pageable pageable) {
        Page<PhoneDevice> page;
        if (orgIds != null && !orgIds.isEmpty()) {
            if (status != null) {
                page = phoneDeviceRepository.findByOrgIdsAndStatus(orgIds, status, pageable);
            } else {
                page = phoneDeviceRepository.findByOrgIds(orgIds, pageable);
            }
        } else {
            if (status != null) {
                page = phoneDeviceRepository.findByStatus(status, pageable);
            } else {
                page = phoneDeviceRepository.findAll(pageable);
            }
        }
        return page.map(this::toDTO);
    }

    public PhoneDeviceDTO getDeviceDetail(Long id) {
        PhoneDevice device = phoneDeviceRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEVICE_NOT_FOUND));
        return toDTO(device);
    }

    public List<BoundPhoneDTO> getBoundPhones(Long deviceId) {
        phoneDeviceRepository.findById(deviceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEVICE_NOT_FOUND));
        List<DevicePhoneMapping> mappings = devicePhoneMappingRepository.findByDeviceId(deviceId);
        return mappings.stream()
                .map(mapping -> {
                    PhoneNumber phone = phoneNumberRepository.findById(mapping.getPhoneId()).orElse(null);
                    if (phone == null) return null;
                    BoundPhoneDTO dto = new BoundPhoneDTO();
                    dto.setPhoneId(phone.getId());
                    dto.setPhoneNumber(phone.getPhoneNumber());
                    dto.setExtensionNumber(phone.getExtensionNumber());
                    dto.setStatus(phone.getStatus().name());
                    dto.setUserId(phone.getUserId());
                    dto.setLineOrder(mapping.getLineOrder());
                    dto.setCreatedAt(mapping.getCreatedAt());
                    if (phone.getUserId() != null) {
                        Employee emp = employeeRepository.findByEmployeeNo(phone.getUserId()).orElse(null);
                        if (emp != null) {
                            dto.setUserName(emp.getName());
                        }
                    }
                    if (phone.getAllocationOrgId() != null) {
                        dto.setOrgId(phone.getAllocationOrgId());
                        OrgStructure org = orgStructureRepository.findById(phone.getAllocationOrgId()).orElse(null);
                        if (org != null) {
                            dto.setOrgName(org.getName());
                        }
                    }
                    return dto;
                })
                .filter(dto -> dto != null)
                .collect(Collectors.toList());
    }

    public List<PhoneDeviceHistoryDTO> getDeviceHistory(Long deviceId) {
        phoneDeviceRepository.findById(deviceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEVICE_NOT_FOUND));
        List<PhoneDeviceHistory> historyList = phoneDeviceHistoryRepository.findByDeviceIdOrderByOperatedAtDesc(deviceId);
        return historyList.stream().map(this::toHistoryDTO).collect(Collectors.toList());
    }

    @Transactional
    public PhoneDeviceDTO createDevice(CreatePhoneDeviceRequest request) {
        String normalizedMac = normalizeMac(request.getMacAddress());
        if (!isValidMac(normalizedMac)) {
            throw new BusinessException(ErrorCode.DEVICE_MAC_INVALID);
        }
        if (phoneDeviceRepository.existsByMacAddress(normalizedMac)) {
            throw new BusinessException(ErrorCode.DEVICE_MAC_DUPLICATE);
        }
        if (!orgStructureRepository.existsById(request.getOrgId())) {
            throw new BusinessException(ErrorCode.DEVICE_ORG_NOT_FOUND);
        }
        if (request.getPurchaseDate() != null && request.getPurchaseDate().isAfter(LocalDate.now())) {
            throw new BusinessException(ErrorCode.DEVICE_PURCHASE_DATE_FUTURE);
        }

        String currentUser = getCurrentUsername();

        PhoneDevice device = PhoneDevice.builder()
                .macAddress(normalizedMac)
                .model(request.getModel())
                .brand(request.getBrand())
                .purchaseDate(request.getPurchaseDate())
                .orgId(request.getOrgId())
                .status(PhoneDevice.PhoneDeviceStatus.stock)
                .remark(request.getRemark())
                .createdBy(currentUser)
                .updatedBy(currentUser)
                .build();

        PhoneDevice saved = phoneDeviceRepository.save(device);
        saveHistory(saved, "录入", null, saved.getStatus().name(), null, null, null);
        return toDTO(saved);
    }

    @Transactional
    public PhoneDeviceDTO updateDevice(Long id, UpdatePhoneDeviceRequest request) {
        PhoneDevice device = phoneDeviceRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEVICE_NOT_FOUND));
        if (device.getStatus() == PhoneDevice.PhoneDeviceStatus.retired) {
            throw new BusinessException(ErrorCode.DEVICE_RETIRED);
        }
        if (request.getOrgId() != null && !orgStructureRepository.existsById(request.getOrgId())) {
            throw new BusinessException(ErrorCode.DEVICE_ORG_NOT_FOUND);
        }
        if (request.getPurchaseDate() != null && request.getPurchaseDate().isAfter(LocalDate.now())) {
            throw new BusinessException(ErrorCode.DEVICE_PURCHASE_DATE_FUTURE);
        }

        String currentUser = getCurrentUsername();

        if (request.getModel() != null) device.setModel(request.getModel());
        if (request.getBrand() != null) device.setBrand(request.getBrand());
        if (request.getPurchaseDate() != null) device.setPurchaseDate(request.getPurchaseDate());
        if (request.getOrgId() != null) device.setOrgId(request.getOrgId());
        if (request.getRemark() != null) device.setRemark(request.getRemark());
        device.setUpdatedBy(currentUser);

        PhoneDevice saved = phoneDeviceRepository.save(device);
        return toDTO(saved);
    }

    @Transactional
    public PhoneDeviceDTO assignDevice(Long id, AssignPhoneDeviceRequest request) {
        PhoneDevice device = phoneDeviceRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEVICE_NOT_FOUND));
        if (device.getStatus() == PhoneDevice.PhoneDeviceStatus.retired) {
            throw new BusinessException(ErrorCode.DEVICE_RETIRED);
        }
        if (device.getStatus() != PhoneDevice.PhoneDeviceStatus.stock) {
            throw new BusinessException(ErrorCode.DEVICE_NOT_STOCK);
        }

        Employee employee = employeeRepository.findByEmployeeNo(request.getEmployeeNo())
                .orElseThrow(() -> new BusinessException(ErrorCode.DEVICE_EMPLOYEE_NOT_FOUND));
        if (employee.getStatus() != Employee.EmployeeStatus.active) {
            throw new BusinessException(ErrorCode.DEVICE_EMPLOYEE_INACTIVE);
        }

        String currentUser = getCurrentUsername();
        PhoneDevice.PhoneDeviceStatus fromStatus = device.getStatus();
        String fromAssigned = device.getAssignedTo();

        device.setStatus(PhoneDevice.PhoneDeviceStatus.active);
        device.setAssignedTo(request.getEmployeeNo());
        device.setUpdatedBy(currentUser);

        PhoneDevice saved = phoneDeviceRepository.save(device);
        saveHistory(saved, "分配", fromStatus.name(), saved.getStatus().name(), fromAssigned, saved.getAssignedTo(), request.getRemark());
        return toDTO(saved);
    }

    @Transactional
    public PhoneDeviceDTO reclaimDevice(Long id, String remark) {
        PhoneDevice device = phoneDeviceRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEVICE_NOT_FOUND));
        if (device.getStatus() == PhoneDevice.PhoneDeviceStatus.retired) {
            throw new BusinessException(ErrorCode.DEVICE_RETIRED);
        }
        if (device.getStatus() != PhoneDevice.PhoneDeviceStatus.active) {
            throw new BusinessException(ErrorCode.DEVICE_NOT_ACTIVE);
        }

        String currentUser = getCurrentUsername();
        PhoneDevice.PhoneDeviceStatus fromStatus = device.getStatus();
        String fromAssigned = device.getAssignedTo();

        device.setStatus(PhoneDevice.PhoneDeviceStatus.stock);
        device.setAssignedTo(null);
        device.setUpdatedBy(currentUser);

        PhoneDevice saved = phoneDeviceRepository.save(device);
        saveHistory(saved, "回收", fromStatus.name(), saved.getStatus().name(), fromAssigned, null, remark);
        return toDTO(saved);
    }

    @Transactional
    public PhoneDeviceDTO deactivateDevice(Long id, String remark) {
        PhoneDevice device = phoneDeviceRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEVICE_NOT_FOUND));
        if (device.getStatus() == PhoneDevice.PhoneDeviceStatus.retired) {
            throw new BusinessException(ErrorCode.DEVICE_RETIRED);
        }
        if (device.getStatus() != PhoneDevice.PhoneDeviceStatus.active && device.getStatus() != PhoneDevice.PhoneDeviceStatus.stock) {
            throw new BusinessException(ErrorCode.DEVICE_STATUS_INVALID);
        }

        String currentUser = getCurrentUsername();
        PhoneDevice.PhoneDeviceStatus fromStatus = device.getStatus();

        device.setStatus(PhoneDevice.PhoneDeviceStatus.inactive);
        device.setUpdatedBy(currentUser);

        PhoneDevice saved = phoneDeviceRepository.save(device);
        saveHistory(saved, "停用", fromStatus.name(), saved.getStatus().name(), device.getAssignedTo(), device.getAssignedTo(), remark);
        return toDTO(saved);
    }

    @Transactional
    public PhoneDeviceDTO reactivateDevice(Long id, String remark) {
        PhoneDevice device = phoneDeviceRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEVICE_NOT_FOUND));
        if (device.getStatus() == PhoneDevice.PhoneDeviceStatus.retired) {
            throw new BusinessException(ErrorCode.DEVICE_RETIRED);
        }
        if (device.getStatus() != PhoneDevice.PhoneDeviceStatus.inactive) {
            throw new BusinessException(ErrorCode.DEVICE_NOT_INACTIVE);
        }

        String currentUser = getCurrentUsername();
        PhoneDevice.PhoneDeviceStatus fromStatus = device.getStatus();

        device.setStatus(PhoneDevice.PhoneDeviceStatus.stock);
        device.setUpdatedBy(currentUser);

        PhoneDevice saved = phoneDeviceRepository.save(device);
        saveHistory(saved, "恢复", fromStatus.name(), saved.getStatus().name(), device.getAssignedTo(), device.getAssignedTo(), remark);
        return toDTO(saved);
    }

    @Transactional
    public PhoneDeviceDTO repairDevice(Long id, String remark) {
        PhoneDevice device = phoneDeviceRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEVICE_NOT_FOUND));
        if (device.getStatus() == PhoneDevice.PhoneDeviceStatus.retired) {
            throw new BusinessException(ErrorCode.DEVICE_RETIRED);
        }
        if (device.getStatus() != PhoneDevice.PhoneDeviceStatus.active) {
            throw new BusinessException(ErrorCode.DEVICE_NOT_ACTIVE);
        }

        String currentUser = getCurrentUsername();
        PhoneDevice.PhoneDeviceStatus fromStatus = device.getStatus();

        device.setStatus(PhoneDevice.PhoneDeviceStatus.repairing);
        device.setUpdatedBy(currentUser);

        PhoneDevice saved = phoneDeviceRepository.save(device);
        saveHistory(saved, "送修", fromStatus.name(), saved.getStatus().name(), device.getAssignedTo(), device.getAssignedTo(), remark);
        unbindAllPhones(device.getId());
        return toDTO(saved);
    }

    @Transactional
    public PhoneDeviceDTO repairDoneDevice(Long id, String remark) {
        PhoneDevice device = phoneDeviceRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEVICE_NOT_FOUND));
        if (device.getStatus() == PhoneDevice.PhoneDeviceStatus.retired) {
            throw new BusinessException(ErrorCode.DEVICE_RETIRED);
        }
        if (device.getStatus() != PhoneDevice.PhoneDeviceStatus.repairing) {
            throw new BusinessException(ErrorCode.DEVICE_NOT_REPAIRING);
        }

        String currentUser = getCurrentUsername();
        PhoneDevice.PhoneDeviceStatus fromStatus = device.getStatus();

        device.setStatus(PhoneDevice.PhoneDeviceStatus.active);
        device.setUpdatedBy(currentUser);

        PhoneDevice saved = phoneDeviceRepository.save(device);
        saveHistory(saved, "修复完成", fromStatus.name(), saved.getStatus().name(), device.getAssignedTo(), device.getAssignedTo(), remark);
        return toDTO(saved);
    }

    @Transactional
    public PhoneDeviceDTO retireDevice(Long id, String remark) {
        PhoneDevice device = phoneDeviceRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEVICE_NOT_FOUND));
        if (device.getStatus() == PhoneDevice.PhoneDeviceStatus.retired) {
            throw new BusinessException(ErrorCode.DEVICE_RETIRED);
        }

        String currentUser = getCurrentUsername();
        PhoneDevice.PhoneDeviceStatus fromStatus = device.getStatus();
        String fromAssigned = device.getAssignedTo();

        device.setStatus(PhoneDevice.PhoneDeviceStatus.retired);
        device.setAssignedTo(null);
        device.setUpdatedBy(currentUser);

        PhoneDevice saved = phoneDeviceRepository.save(device);
        saveHistory(saved, "报废", fromStatus.name(), saved.getStatus().name(), fromAssigned, null, remark);
        unbindAllPhones(device.getId());
        return toDTO(saved);
    }

    @Transactional
    public void bindPhone(Long deviceId, BindPhoneRequest request) {
        PhoneDevice device = phoneDeviceRepository.findById(deviceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEVICE_NOT_FOUND));
        if (device.getStatus() == PhoneDevice.PhoneDeviceStatus.retired) {
            throw new BusinessException(ErrorCode.DEVICE_RETIRED);
        }
        if (device.getStatus() != PhoneDevice.PhoneDeviceStatus.active) {
            throw new BusinessException(ErrorCode.DEVICE_NOT_ACTIVE);
        }

        PhoneNumber phone = phoneNumberRepository.findByExtensionNumber(request.getExtensionNumber())
                .orElseThrow(() -> new BusinessException(ErrorCode.PHONE_001));
        if (phone.getExtensionNumber() == null || phone.getExtensionNumber().isEmpty()) {
            throw new BusinessException(ErrorCode.DEVICE_PHONE_NO_EXTENSION);
        }
        if (devicePhoneMappingRepository.existsByDeviceIdAndPhoneId(deviceId, phone.getId())) {
            throw new BusinessException(ErrorCode.DEVICE_PHONE_ALREADY_BOUND);
        }

        int maxLineOrder = devicePhoneMappingRepository.findByDeviceId(deviceId).stream()
                .mapToInt(DevicePhoneMapping::getLineOrder)
                .max()
                .orElse(0);

        DevicePhoneMapping mapping = DevicePhoneMapping.builder()
                .deviceId(deviceId)
                .phoneId(phone.getId())
                .lineOrder(maxLineOrder + 1)
                .build();

        devicePhoneMappingRepository.save(mapping);
        log.info("话机{}绑定号码{}成功", deviceId, phone.getId());
    }

    @Transactional
    public void unbindPhone(Long deviceId, Long phoneId) {
        phoneDeviceRepository.findById(deviceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEVICE_NOT_FOUND));
        devicePhoneMappingRepository.findByDeviceIdAndPhoneId(deviceId, phoneId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PHONE_001));
        devicePhoneMappingRepository.deleteByDeviceIdAndPhoneId(deviceId, phoneId);
        log.info("话机{}解绑号码{}成功", deviceId, phoneId);
    }

    private void unbindAllPhones(Long deviceId) {
        List<DevicePhoneMapping> mappings = devicePhoneMappingRepository.findByDeviceId(deviceId);
        if (!mappings.isEmpty()) {
            devicePhoneMappingRepository.deleteByDeviceId(deviceId);
            log.info("话机{}解绑全部{}个号码", deviceId, mappings.size());
        }
    }

    private String normalizeMac(String mac) {
        if (mac == null) return null;
        return mac.replaceAll("[:-]", "").toUpperCase();
    }

    private boolean isValidMac(String mac) {
        if (mac == null || mac.length() != 12) return false;
        return mac.matches("[0-9A-Fa-f]{12}");
    }

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null ? authentication.getName() : "system";
    }

    private PhoneDeviceDTO toDTO(PhoneDevice device) {
        PhoneDeviceDTO dto = new PhoneDeviceDTO();
        dto.setId(device.getId());
        dto.setMacAddress(device.getMacAddress());
        dto.setModel(device.getModel());
        dto.setBrand(device.getBrand());
        dto.setPurchaseDate(device.getPurchaseDate());
        dto.setOrgId(device.getOrgId());
        dto.setAssignedTo(device.getAssignedTo());
        dto.setStatus(device.getStatus().name());
        dto.setRemark(device.getRemark());
        dto.setCreatedAt(device.getCreatedAt());
        dto.setUpdatedAt(device.getUpdatedAt());

        OrgStructure org = orgStructureRepository.findById(device.getOrgId()).orElse(null);
        if (org != null) {
            dto.setOrgName(org.getName());
        }

        if (device.getAssignedTo() != null) {
            Employee emp = employeeRepository.findByEmployeeNo(device.getAssignedTo()).orElse(null);
            if (emp != null) {
                dto.setAssignedEmployeeName(emp.getName());
            }
        }

        List<DevicePhoneMapping> mappings = devicePhoneMappingRepository.findByDeviceId(device.getId());
        dto.setBoundPhoneCount(mappings.size());

        return dto;
    }

    private PhoneDeviceHistoryDTO toHistoryDTO(PhoneDeviceHistory history) {
        return PhoneDeviceHistoryDTO.builder()
                .id(history.getId())
                .deviceId(history.getDeviceId())
                .macAddress(history.getMacAddress())
                .action(history.getAction())
                .fromStatus(history.getFromStatus())
                .toStatus(history.getToStatus())
                .fromAssigned(history.getFromAssigned())
                .toAssigned(history.getToAssigned())
                .operator(history.getOperator())
                .operatedAt(history.getOperatedAt())
                .remark(history.getRemark())
                .build();
    }

    private void saveHistory(PhoneDevice device, String action, String fromStatus, String toStatus, String fromAssigned, String toAssigned, String remark) {
        PhoneDeviceHistory history = PhoneDeviceHistory.builder()
                .deviceId(device.getId())
                .macAddress(device.getMacAddress())
                .action(action)
                .fromStatus(fromStatus)
                .toStatus(toStatus)
                .fromAssigned(fromAssigned)
                .toAssigned(toAssigned)
                .operator(getCurrentUsername())
                .remark(remark)
                .build();
        phoneDeviceHistoryRepository.save(history);
    }
}

