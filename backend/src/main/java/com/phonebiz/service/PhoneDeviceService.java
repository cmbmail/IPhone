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

    public Page<PhoneDeviceDTO> getDeviceList(List<Long> orgIds, Integer status, Pageable pageable) {
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
        // Batch load related data to avoid N+1
        List<Long> deviceIds = page.getContent().stream().map(PhoneDevice::getId).collect(Collectors.toList());
        java.util.Map<Long, String> orgNameMap = batchLoadOrgNames(page.getContent().stream().map(PhoneDevice::getOrgId).filter(id -> id != null).distinct().collect(Collectors.toList()));
        java.util.Map<String, String> empNameMap = batchLoadEmployeeNames(
                page.getContent().stream().map(PhoneDevice::getAssignedTo).filter(a -> a != null).distinct().collect(Collectors.toList()));
        java.util.Map<Long, Integer> phoneCountMap = batchLoadPhoneCounts(deviceIds);
        return page.map(d -> toDTOEnriched(d, orgNameMap, empNameMap, phoneCountMap));
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
        if (mappings.isEmpty()) return List.of();

        // Batch load all related data
        List<Long> phoneIds = mappings.stream().map(DevicePhoneMapping::getPhoneId).collect(Collectors.toList());
        java.util.Map<Long, PhoneNumber> phoneMap = new java.util.HashMap<>();
        phoneNumberRepository.findAllById(phoneIds).forEach(p -> phoneMap.put(p.getId(), p));

        List<String> userIds = phoneMap.values().stream()
                .map(PhoneNumber::getUserId).filter(u -> u != null).distinct().collect(Collectors.toList());
        java.util.Map<String, String> empNameMap = batchLoadEmployeeNames(userIds);

        List<Long> orgIds = phoneMap.values().stream()
                .map(PhoneNumber::getAllocationOrgId).filter(o -> o != null).distinct().collect(Collectors.toList());
        java.util.Map<Long, String> orgNameMap = batchLoadOrgNames(orgIds);

        return mappings.stream()
                .map(mapping -> {
                    PhoneNumber phone = phoneMap.get(mapping.getPhoneId());
                    if (phone == null) return null;
                    BoundPhoneDTO dto = new BoundPhoneDTO();
                    dto.setPhoneId(phone.getId());
                    dto.setPhoneNumber(phone.getPhoneNumber());
                    dto.setExtensionNumber(phone.getExtensionNumber());
                    dto.setStatus(String.valueOf(phone.getStatus()));
                    dto.setUserId(phone.getUserId());
                    dto.setLineOrder(mapping.getLineOrder());
                    dto.setCreatedAt(mapping.getCreatedAt());
                    if (phone.getUserId() != null) {
                        dto.setUserName(empNameMap.getOrDefault(phone.getUserId(), null));
                    }
                    if (phone.getAllocationOrgId() != null) {
                        dto.setOrgId(phone.getAllocationOrgId());
                        dto.setOrgName(orgNameMap.getOrDefault(phone.getAllocationOrgId(), null));
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
                .status(PhoneDevice.PD_STOCK)
                .remark(request.getRemark())
                .createdBy(currentUser)
                .updatedBy(currentUser)
                .build();

        PhoneDevice saved = phoneDeviceRepository.save(device);
        saveHistory(saved, "录入", null, saved.getStatus(), null, null, null);
        return toDTO(saved);
    }

    @Transactional
    public PhoneDeviceDTO updateDevice(Long id, UpdatePhoneDeviceRequest request) {
        PhoneDevice device = phoneDeviceRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEVICE_NOT_FOUND));
        if (device.getStatus() == PhoneDevice.PD_RETIRED) {
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
        if (device.getStatus() == PhoneDevice.PD_RETIRED) {
            throw new BusinessException(ErrorCode.DEVICE_RETIRED);
        }
        if (device.getStatus() != PhoneDevice.PD_STOCK) {
            throw new BusinessException(ErrorCode.DEVICE_NOT_STOCK);
        }

        Employee employee = employeeRepository.findByEmployeeNo(request.getEmployeeNo())
                .orElseThrow(() -> new BusinessException(ErrorCode.DEVICE_EMPLOYEE_NOT_FOUND));
        if (employee.getStatus() != Employee.EMP_ACTIVE) {
            throw new BusinessException(ErrorCode.DEVICE_EMPLOYEE_INACTIVE);
        }

        String currentUser = getCurrentUsername();
        int fromStatus = device.getStatus();
        String fromAssigned = device.getAssignedTo();

        device.setStatus(PhoneDevice.PD_ACTIVE);
        device.setAssignedTo(request.getEmployeeNo());
        device.setUpdatedBy(currentUser);

        PhoneDevice saved = phoneDeviceRepository.save(device);
        saveHistory(saved, "分配", fromStatus, saved.getStatus(), fromAssigned, saved.getAssignedTo(), request.getRemark());
        return toDTO(saved);
    }

    @Transactional
    public PhoneDeviceDTO reclaimDevice(Long id, String remark) {
        PhoneDevice device = phoneDeviceRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEVICE_NOT_FOUND));
        if (device.getStatus() == PhoneDevice.PD_RETIRED) {
            throw new BusinessException(ErrorCode.DEVICE_RETIRED);
        }
        if (device.getStatus() != PhoneDevice.PD_ACTIVE) {
            throw new BusinessException(ErrorCode.DEVICE_NOT_ACTIVE);
        }

        String currentUser = getCurrentUsername();
        int fromStatus = device.getStatus();
        String fromAssigned = device.getAssignedTo();

        device.setStatus(PhoneDevice.PD_STOCK);
        device.setAssignedTo(null);
        device.setUpdatedBy(currentUser);

        PhoneDevice saved = phoneDeviceRepository.save(device);
        saveHistory(saved, "回收", fromStatus, saved.getStatus(), fromAssigned, null, remark);
        return toDTO(saved);
    }

    @Transactional
    public PhoneDeviceDTO deactivateDevice(Long id, String remark) {
        PhoneDevice device = phoneDeviceRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEVICE_NOT_FOUND));
        if (device.getStatus() == PhoneDevice.PD_RETIRED) {
            throw new BusinessException(ErrorCode.DEVICE_RETIRED);
        }
        if (device.getStatus() != PhoneDevice.PD_ACTIVE && device.getStatus() != PhoneDevice.PD_STOCK) {
            throw new BusinessException(ErrorCode.DEVICE_STATUS_INVALID);
        }

        String currentUser = getCurrentUsername();
        int fromStatus = device.getStatus();

        device.setStatus(PhoneDevice.PD_INACTIVE);
        device.setUpdatedBy(currentUser);

        PhoneDevice saved = phoneDeviceRepository.save(device);
        saveHistory(saved, "停用", fromStatus, saved.getStatus(), device.getAssignedTo(), device.getAssignedTo(), remark);
        return toDTO(saved);
    }

    @Transactional
    public PhoneDeviceDTO reactivateDevice(Long id, String remark) {
        PhoneDevice device = phoneDeviceRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEVICE_NOT_FOUND));
        if (device.getStatus() == PhoneDevice.PD_RETIRED) {
            throw new BusinessException(ErrorCode.DEVICE_RETIRED);
        }
        if (device.getStatus() != PhoneDevice.PD_INACTIVE) {
            throw new BusinessException(ErrorCode.DEVICE_NOT_INACTIVE);
        }

        String currentUser = getCurrentUsername();
        int fromStatus = device.getStatus();

        device.setStatus(PhoneDevice.PD_STOCK);
        device.setUpdatedBy(currentUser);

        PhoneDevice saved = phoneDeviceRepository.save(device);
        saveHistory(saved, "恢复", fromStatus, saved.getStatus(), device.getAssignedTo(), device.getAssignedTo(), remark);
        return toDTO(saved);
    }

    @Transactional
    public PhoneDeviceDTO repairDevice(Long id, String remark) {
        PhoneDevice device = phoneDeviceRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEVICE_NOT_FOUND));
        if (device.getStatus() == PhoneDevice.PD_RETIRED) {
            throw new BusinessException(ErrorCode.DEVICE_RETIRED);
        }
        if (device.getStatus() != PhoneDevice.PD_ACTIVE) {
            throw new BusinessException(ErrorCode.DEVICE_NOT_ACTIVE);
        }

        String currentUser = getCurrentUsername();
        int fromStatus = device.getStatus();

        device.setStatus(PhoneDevice.PD_REPAIRING);
        device.setUpdatedBy(currentUser);

        PhoneDevice saved = phoneDeviceRepository.save(device);
        saveHistory(saved, "送修", fromStatus, saved.getStatus(), device.getAssignedTo(), device.getAssignedTo(), remark);
        unbindAllPhones(device.getId());
        return toDTO(saved);
    }

    @Transactional
    public PhoneDeviceDTO repairDoneDevice(Long id, String remark) {
        PhoneDevice device = phoneDeviceRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEVICE_NOT_FOUND));
        if (device.getStatus() == PhoneDevice.PD_RETIRED) {
            throw new BusinessException(ErrorCode.DEVICE_RETIRED);
        }
        if (device.getStatus() != PhoneDevice.PD_REPAIRING) {
            throw new BusinessException(ErrorCode.DEVICE_NOT_REPAIRING);
        }

        String currentUser = getCurrentUsername();
        int fromStatus = device.getStatus();

        device.setStatus(PhoneDevice.PD_ACTIVE);
        device.setUpdatedBy(currentUser);

        PhoneDevice saved = phoneDeviceRepository.save(device);
        saveHistory(saved, "修复完成", fromStatus, saved.getStatus(), device.getAssignedTo(), device.getAssignedTo(), remark);
        return toDTO(saved);
    }

    @Transactional
    public PhoneDeviceDTO retireDevice(Long id, String remark) {
        PhoneDevice device = phoneDeviceRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEVICE_NOT_FOUND));
        if (device.getStatus() == PhoneDevice.PD_RETIRED) {
            throw new BusinessException(ErrorCode.DEVICE_RETIRED);
        }

        String currentUser = getCurrentUsername();
        int fromStatus = device.getStatus();
        String fromAssigned = device.getAssignedTo();

        device.setStatus(PhoneDevice.PD_RETIRED);
        device.setAssignedTo(null);
        device.setUpdatedBy(currentUser);

        PhoneDevice saved = phoneDeviceRepository.save(device);
        saveHistory(saved, "报废", fromStatus, saved.getStatus(), fromAssigned, null, remark);
        unbindAllPhones(device.getId());
        return toDTO(saved);
    }

    @Transactional
    public void bindPhone(Long deviceId, BindPhoneRequest request) {
        PhoneDevice device = phoneDeviceRepository.findById(deviceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEVICE_NOT_FOUND));
        if (device.getStatus() == PhoneDevice.PD_RETIRED) {
            throw new BusinessException(ErrorCode.DEVICE_RETIRED);
        }
        if (device.getStatus() != PhoneDevice.PD_ACTIVE) {
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

    @Transactional
    public void unbindPhoneByExtension(Long deviceId, String extensionNumber) {
        phoneDeviceRepository.findById(deviceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEVICE_NOT_FOUND));
        // Look up phone by extension number, then find and delete the mapping
        com.phonebiz.entity.PhoneNumber phone = phoneNumberRepository.findByExtensionNumber(extensionNumber)
                .orElseThrow(() -> new BusinessException(ErrorCode.SYS_002, "Phone with extension " + extensionNumber + " not found"));
        devicePhoneMappingRepository.findByDeviceIdAndPhoneId(deviceId, phone.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.SYS_002, "Binding not found for extension " + extensionNumber));
        devicePhoneMappingRepository.deleteByDeviceIdAndPhoneId(deviceId, phone.getId());
        log.info("话机{}解绑分机{}成功", deviceId, extensionNumber);
    }

    @Transactional
    public void unbindAllPhonesForDevice(Long deviceId) {
        unbindAllPhones(deviceId);
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

    private PhoneDeviceDTO toDTOEnriched(PhoneDevice device, java.util.Map<Long, String> orgNameMap,
                                         java.util.Map<String, String> empNameMap, java.util.Map<Long, Integer> phoneCountMap) {
        PhoneDeviceDTO dto = new PhoneDeviceDTO();
        dto.setId(device.getId());
        dto.setMacAddress(device.getMacAddress());
        dto.setModel(device.getModel());
        dto.setBrand(device.getBrand());
        dto.setPurchaseDate(device.getPurchaseDate());
        dto.setOrgId(device.getOrgId());
        dto.setAssignedTo(device.getAssignedTo());
        dto.setStatus(device.getStatus());
        dto.setRemark(device.getRemark());
        dto.setCreatedAt(device.getCreatedAt());
        dto.setUpdatedAt(device.getUpdatedAt());

        if (device.getOrgId() != null) {
            dto.setOrgName(orgNameMap.getOrDefault(device.getOrgId(), null));
        }

        if (device.getAssignedTo() != null) {
            dto.setAssignedEmployeeName(empNameMap.getOrDefault(device.getAssignedTo(), null));
        }

        dto.setBoundPhoneCount(phoneCountMap.getOrDefault(device.getId(), 0));

        return dto;
    }

    private PhoneDeviceDTO toDTO(PhoneDevice device) {
        // Used for single device operations (detail, create, update) - N+1 not an issue
        return toDTOEnriched(device,
                batchLoadOrgNames(List.of(device.getOrgId())),
                batchLoadEmployeeNames(device.getAssignedTo() != null ? List.of(device.getAssignedTo()) : List.of()),
                batchLoadPhoneCounts(List.of(device.getId())));
    }

    private java.util.Map<Long, String> batchLoadOrgNames(List<Long> orgIds) {
        java.util.Map<Long, String> map = new java.util.HashMap<>();
        if (orgIds == null || orgIds.isEmpty()) return map;
        List<Long> distinctIds = orgIds.stream().distinct().collect(Collectors.toList());
        orgStructureRepository.findAllById(distinctIds).forEach(o -> map.put(o.getId(), o.getName()));
        return map;
    }

    private java.util.Map<String, String> batchLoadEmployeeNames(List<String> employeeNos) {
        java.util.Map<String, String> map = new java.util.HashMap<>();
        if (employeeNos == null || employeeNos.isEmpty()) return map;
        List<String> distinctNos = employeeNos.stream().distinct().collect(Collectors.toList());
        employeeRepository.findAllByEmployeeNoIn(distinctNos).forEach(e -> map.put(e.getEmployeeNo(), e.getName()));
        return map;
    }

    private java.util.Map<Long, Integer> batchLoadPhoneCounts(List<Long> deviceIds) {
        java.util.Map<Long, Integer> map = new java.util.HashMap<>();
        if (deviceIds == null || deviceIds.isEmpty()) return map;
        List<Object[]> results = devicePhoneMappingRepository.countByDeviceIdIn(deviceIds);
        for (Object[] row : results) {
            map.put((Long) row[0], ((Number) row[1]).intValue());
        }
        return map;
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

    private void saveHistory(PhoneDevice device, String action, Integer fromStatus, Integer toStatus, String fromAssigned, String toAssigned, String remark) {
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

