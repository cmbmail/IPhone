package com.phonebiz.service;

import java.time.LocalDate;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.phonebiz.common.BusinessException;
import com.phonebiz.common.ErrorCode;
import com.phonebiz.dto.CreateEmployeeRequest;
import com.phonebiz.dto.UpdateEmployeeRequest;
import com.phonebiz.entity.Employee;
import com.phonebiz.entity.OrgStructure;
import com.phonebiz.entity.PhoneDevice;
import com.phonebiz.entity.PhoneNumber;
import com.phonebiz.repository.EmployeeRepository;
import com.phonebiz.security.DataScope;
import com.phonebiz.repository.OrgStructureRepository;
import com.phonebiz.repository.PhoneDeviceRepository;
import com.phonebiz.repository.PhoneNumberRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final DataScope dataScope;
    private final OrgStructureRepository orgRepository;
    private final SysUserService sysUserService;
    private final PhoneDeviceRepository phoneDeviceRepository;
    private final PhoneNumberRepository phoneNumberRepository;

    private static final Pattern EMPLOYEE_NO_PATTERN = Pattern.compile("^[A-Z0-9]{6}$");

    @Transactional(readOnly = true)
    public Page<Employee> getEmployees(Pageable pageable) {
        return employeeRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Employee getEmployeeById(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.EMP_002));
    }

    @Transactional(readOnly = true)
    public Employee getEmployeeByEmployeeNo(String employeeNo) {
        return employeeRepository.findByEmployeeNo(employeeNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.EMP_002));
    }

    @Transactional(readOnly = true)
    public List<Employee> getEmployeesByOrg(Long orgId) {
        return employeeRepository.findByOrgId(orgId);
    }

    @Transactional(readOnly = true)
    public List<Employee> getAllActiveEmployees() {
        return employeeRepository.findAllActive();
    }

    @Transactional
    public Employee createEmployee(CreateEmployeeRequest request, String operator) {
        validateEmployeeNo(request.getEmployeeNo());

        if (employeeRepository.existsByEmployeeNo(request.getEmployeeNo())) {
            throw new BusinessException(ErrorCode.EMP_001);
        }

        OrgStructure org = orgRepository.findById(request.getOrgId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ORG_001));

        Employee employee = new Employee();
        employee.setEmployeeNo(request.getEmployeeNo().toUpperCase());
        employee.setName(request.getName());
        employee.setOrgId(request.getOrgId());
        employee.setPosition(request.getPosition());
        employee.setPhoneNumber(request.getPhoneNumber());
        employee.setEmail(request.getEmail());
        employee.setIsVirtual(request.getIsVirtual() != null ? request.getIsVirtual() : false);

        if (request.getEntryDate() != null) {
            employee.setEntryDate(LocalDate.parse(request.getEntryDate()));
        }

        employee.setStatus(Employee.EMP_ACTIVE);
        employee.setCreatedBy(operator);
        employee.setUpdatedBy(operator);

        Employee saved = employeeRepository.save(employee);

        if (!Boolean.TRUE.equals(employee.getIsVirtual())) {
            sysUserService.createUserForEmployee(saved, operator);
        }

        return saved;
    }

    @Transactional
    public Employee updateEmployee(Long id, UpdateEmployeeRequest request, String operator) {
        Employee employee = getEmployeeById(id);

        if (request.getName() != null) {
            employee.setName(request.getName());
        }

        if (request.getOrgId() != null) {
            OrgStructure org = orgRepository.findById(request.getOrgId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.ORG_001));
            employee.setOrgId(request.getOrgId());
        }

        if (request.getPosition() != null) {
            employee.setPosition(request.getPosition());
        }

        if (request.getPhoneNumber() != null) {
            employee.setPhoneNumber(request.getPhoneNumber());
        }

        if (request.getEmail() != null) {
            employee.setEmail(request.getEmail());
        }

        if (request.getStatus() != null) {
            employee.setStatus(Integer.valueOf(request.getStatus()));
        }

        if (request.getLeaveDate() != null) {
            employee.setLeaveDate(LocalDate.parse(request.getLeaveDate()));
        }

        employee.setUpdatedBy(operator);
        return employeeRepository.save(employee);
    }

    @Transactional
    public void deleteEmployee(Long id) {
        Employee employee = getEmployeeById(id);
        employee.setStatus(Employee.EMP_INACTIVE);
        employeeRepository.save(employee);
    }

    @Transactional
    public Employee terminateEmployee(Long id, String remark, String operator) {
        Employee employee = getEmployeeById(id);
        
        if (employee.getStatus() != Employee.EMP_ACTIVE) {
            throw new BusinessException(ErrorCode.EMP_004);
        }

        String employeeNo = employee.getEmployeeNo();
        
        List<PhoneDevice> devices = phoneDeviceRepository.findByAssignedEmployeeNo(employeeNo);
        devices.forEach(device -> {
            device.setStatus(PhoneDevice.PD_STOCK);
            device.setAssignedEmployeeNo(null);
            device.setUpdatedBy(operator);
            log.info("Device {} reclaimed from employee {} on termination", device.getId(), employeeNo);
        });
        phoneDeviceRepository.saveAll(devices);

        List<PhoneNumber> phones = phoneNumberRepository.findByEmployeeNo(employeeNo);
        phones.forEach(phone -> {
            phone.setEmployeeNo(null);
            phone.setStatus(PhoneNumber.PS_IDLE);
            phone.setUpdatedBy(operator);
            log.info("Phone {} reclaimed from employee {} on termination", phone.getPhoneNumber(), employeeNo);
        });
        phoneNumberRepository.saveAll(phones);

        employee.setStatus(Employee.EMP_INACTIVE);
        employee.setLeaveDate(LocalDate.now());
        employee.setUpdatedBy(operator);
        
        Employee saved = employeeRepository.save(employee);
        
        log.info("Employee {} terminated, devices and phones reclaimed", employeeNo);
        return saved;
    }

    @Transactional(readOnly = true)
    public long countByOrg(Long orgId) {
        return employeeRepository.countActiveByOrgId(orgId);
    }

    private void validateEmployeeNo(String employeeNo) {
        if (!EMPLOYEE_NO_PATTERN.matcher(employeeNo).matches()) {
            throw new BusinessException(ErrorCode.EMP_003);
        }
    }

    @Transactional(readOnly = true)
    public List<Long> getOrgIdsUnderScope(Long scopeOrgId) {
        if (scopeOrgId == null) {
            return orgRepository.findAll().stream()
                    .map(OrgStructure::getId)
                    .collect(Collectors.toList());
        }
        return orgRepository.findByPathStartingWith("/" + scopeOrgId + "/").stream()
                .map(OrgStructure::getId)
                .collect(Collectors.toList());
    }
}

