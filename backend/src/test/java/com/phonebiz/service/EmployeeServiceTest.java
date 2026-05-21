package com.phonebiz.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.phonebiz.common.BusinessException;
import com.phonebiz.dto.CreateEmployeeRequest;
import com.phonebiz.dto.UpdateEmployeeRequest;
import com.phonebiz.entity.Employee;
import com.phonebiz.entity.OrgStructure;
import com.phonebiz.repository.EmployeeRepository;
import com.phonebiz.repository.OrgStructureRepository;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private OrgStructureRepository orgRepository;

    @Mock
    private SysUserService sysUserService;

    @InjectMocks
    private EmployeeService employeeService;

    private Employee testEmployee;
    private OrgStructure testOrg;

    @BeforeEach
    void setUp() {
        testOrg = new OrgStructure();
        testOrg.setId(1L);
        testOrg.setName("Test Org");
        testOrg.setLevel(1);
        testOrg.setPath("/1");

        testEmployee = new Employee();
        testEmployee.setId(1L);
        testEmployee.setEmployeeNo("EMP001");
        testEmployee.setName("Test Employee");
        testEmployee.setOrgId(1L);
        testEmployee.setPosition("Developer");
        testEmployee.setStatus(Employee.EmployeeStatus.active);
    }

    @Test
    @DisplayName("测试创建员工 - 成功")
    void testCreateEmployee_Success() {
        CreateEmployeeRequest request = new CreateEmployeeRequest();
        request.setEmployeeNo("EMP001");
        request.setName("Test Employee");
        request.setOrgId(1L);
        request.setPosition("Developer");

        when(orgRepository.findById(1L)).thenReturn(Optional.of(testOrg));
        when(employeeRepository.existsByEmployeeNo("EMP001")).thenReturn(false);
        when(employeeRepository.save(any(Employee.class))).thenReturn(testEmployee);
        doNothing().when(sysUserService).createUserForEmployee(any(Employee.class), anyString());

        Employee result = employeeService.createEmployee(request, "admin");

        assertNotNull(result);
        assertEquals("EMP001", result.getEmployeeNo());
        verify(employeeRepository).save(any(Employee.class));
    }

    @Test
    @DisplayName("测试创建员工 - 员工号已存在")
    void testCreateEmployee_DuplicateEmployeeNo() {
        CreateEmployeeRequest request = new CreateEmployeeRequest();
        request.setEmployeeNo("EMP001");
        request.setName("Test Employee");
        request.setOrgId(1L);

        when(employeeRepository.existsByEmployeeNo("EMP001")).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class, () -> 
            employeeService.createEmployee(request, "admin"));
        assertTrue(exception.getMessage().contains("already exists"));
    }

    @Test
    @DisplayName("测试创建员工 - 员工号格式错误")
    void testCreateEmployee_InvalidEmployeeNo() {
        CreateEmployeeRequest request = new CreateEmployeeRequest();
        request.setEmployeeNo("INVALID");
        request.setName("Test Employee");
        request.setOrgId(1L);

        BusinessException exception = assertThrows(BusinessException.class, () -> 
            employeeService.createEmployee(request, "admin"));
        assertTrue(exception.getMessage().contains("format invalid"));
    }

    @Test
    @DisplayName("测试创建员工 - 组织不存在")
    void testCreateEmployee_OrgNotFound() {
        CreateEmployeeRequest request = new CreateEmployeeRequest();
        request.setEmployeeNo("EMP001");
        request.setName("Test Employee");
        request.setOrgId(99L);

        when(orgRepository.findById(99L)).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class, () -> 
            employeeService.createEmployee(request, "admin"));
        assertTrue(exception.getMessage().contains("Organization not found"));
    }

    @Test
    @DisplayName("测试获取员工 - 成功")
    void testGetEmployeeById_Success() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(testEmployee));

        Employee result = employeeService.getEmployeeById(1L);

        assertNotNull(result);
        assertEquals("EMP001", result.getEmployeeNo());
    }

    @Test
    @DisplayName("测试获取员工 - 不存在")
    void testGetEmployeeById_NotFound() {
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class, () -> 
            employeeService.getEmployeeById(99L));
        assertTrue(exception.getMessage().contains("Employee not found"));
    }

    @Test
    @DisplayName("测试更新员工 - 成功")
    void testUpdateEmployee_Success() {
        UpdateEmployeeRequest request = new UpdateEmployeeRequest();
        request.setName("Updated Name");
        request.setPosition("Senior Developer");

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(testEmployee));
        when(employeeRepository.save(any(Employee.class))).thenReturn(testEmployee);

        Employee result = employeeService.updateEmployee(1L, request, "admin");

        assertNotNull(result);
        verify(employeeRepository).save(any(Employee.class));
    }

    @Test
    @DisplayName("测试更新员工 - 组织变更")
    void testUpdateEmployee_ChangeOrg() {
        UpdateEmployeeRequest request = new UpdateEmployeeRequest();
        request.setOrgId(2L);

        OrgStructure newOrg = new OrgStructure();
        newOrg.setId(2L);

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(testEmployee));
        when(orgRepository.findById(2L)).thenReturn(Optional.of(newOrg));
        when(employeeRepository.save(any(Employee.class))).thenReturn(testEmployee);

        assertDoesNotThrow(() -> employeeService.updateEmployee(1L, request, "admin"));
        verify(employeeRepository).save(any(Employee.class));
    }

    @Test
    @DisplayName("测试删除员工 - 成功")
    void testDeleteEmployee_Success() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(testEmployee));
        when(employeeRepository.save(any(Employee.class))).thenReturn(testEmployee);

        assertDoesNotThrow(() -> employeeService.deleteEmployee(1L));
        verify(employeeRepository).save(any(Employee.class));
    }

    @Test
    @DisplayName("测试获取组织下员工列表")
    void testGetEmployeesByOrg() {
        when(employeeRepository.findByOrgId(1L)).thenReturn(Arrays.asList(testEmployee));

        List<Employee> result = employeeService.getEmployeesByOrg(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("测试获取所有活跃员工")
    void testGetAllActiveEmployees() {
        when(employeeRepository.findAllActive()).thenReturn(Arrays.asList(testEmployee));

        List<Employee> result = employeeService.getAllActiveEmployees();

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("测试按工号查询员工 - 成功")
    void testGetEmployeeByEmployeeNo_Success() {
        when(employeeRepository.findByEmployeeNo("EMP001")).thenReturn(Optional.of(testEmployee));

        Employee result = employeeService.getEmployeeByEmployeeNo("EMP001");

        assertNotNull(result);
        assertEquals("EMP001", result.getEmployeeNo());
    }

    @Test
    @DisplayName("测试按工号查询员工 - 不存在")
    void testGetEmployeeByEmployeeNo_NotFound() {
        when(employeeRepository.findByEmployeeNo("EMP999")).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class, () -> 
            employeeService.getEmployeeByEmployeeNo("EMP999"));
        assertTrue(exception.getMessage().contains("Employee not found"));
    }

    @Test
    @DisplayName("测试统计组织员工数")
    void testCountByOrg() {
        when(employeeRepository.countActiveByOrgId(1L)).thenReturn(10L);

        Long result = employeeService.countByOrg(1L);

        assertNotNull(result);
        assertEquals(10L, result);
    }

    @Test
    @DisplayName("测试更新员工 - 不存在")
    void testUpdateEmployee_NotFound() {
        UpdateEmployeeRequest request = new UpdateEmployeeRequest();
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class, () -> 
            employeeService.updateEmployee(99L, request, "admin"));
        assertTrue(exception.getMessage().contains("Employee not found"));
    }

    @Test
    @DisplayName("测试删除员工 - 不存在")
    void testDeleteEmployee_NotFound() {
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class, () -> 
            employeeService.deleteEmployee(99L));
        assertTrue(exception.getMessage().contains("Employee not found"));
    }

    @Test
    @DisplayName("测试创建员工 - 缺少必填字段")
    void testCreateEmployee_MissingRequired() {
        CreateEmployeeRequest request = new CreateEmployeeRequest();
        request.setEmployeeNo("EMP001");
        // 缺少name字段

        BusinessException exception = assertThrows(BusinessException.class, () -> 
            employeeService.createEmployee(request, "admin"));
        assertNotNull(exception);
    }
}

