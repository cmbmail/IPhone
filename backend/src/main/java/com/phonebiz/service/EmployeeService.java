package com.phonebiz.service;

import com.phonebiz.common.BusinessException;
import com.phonebiz.common.ErrorCode;
import com.phonebiz.dto.CreateEmployeeRequest;
import com.phonebiz.dto.UpdateEmployeeRequest;
import com.phonebiz.entity.Employee;
import com.phonebiz.entity.OrgStructure;
import com.phonebiz.repository.EmployeeRepository;
import com.phonebiz.repository.OrgStructureRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final OrgStructureRepository orgRepository;
    private final SysUserService sysUserService;

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
        employee.setPhone(request.getPhone());
        employee.setEmail(request.getEmail());
        employee.setIsVirtual(request.getIsVirtual() != null ? request.getIsVirtual() : false);

        if (request.getEntryDate() != null) {
            employee.setEntryDate(LocalDate.parse(request.getEntryDate()));
        }

        employee.setStatus(Employee.EmployeeStatus.active);
        employee.setCreatedBy(operator);
        employee.setUpdatedBy(operator);

        Employee saved = employeeRepository.save(employee);

        if (!Boolean.TRUE.equals(employee.getIsVirtual())) {
            try {
                sysUserService.createUserForEmployee(saved, operator);
            } catch (Exception e) {
                log.warn("Failed to create sys_user for employee: {}", employee.getEmployeeNo(), e);
            }
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

        if (request.getPhone() != null) {
            employee.setPhone(request.getPhone());
        }

        if (request.getEmail() != null) {
            employee.setEmail(request.getEmail());
        }

        if (request.getStatus() != null) {
            employee.setStatus(Employee.EmployeeStatus.valueOf(request.getStatus()));
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
        employee.setStatus(Employee.EmployeeStatus.inactive);
        employeeRepository.save(employee);
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
