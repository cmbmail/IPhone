package com.phonebiz.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.phonebiz.config.TestSecurityConfig;
import com.phonebiz.entity.Employee;
import com.phonebiz.service.EmployeeService;

@WebMvcTest(EmployeeController.class)
@Import(TestSecurityConfig.class)
class EmployeeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EmployeeService employeeService;

    private Employee createTestEmployee() {
        Employee employee = new Employee();
        employee.setId(1L);
        employee.setEmployeeNo("EMP001");
        employee.setName("Test Employee");
        employee.setStatus(Employee.EmployeeStatus.active);
        return employee;
    }

    @Test
    @DisplayName("测试获取员工列表")
    void testGetEmployees() throws Exception {
        when(employeeService.getEmployeesByOrg(anyLong())).thenReturn(List.of(createTestEmployee()));

        mockMvc.perform(get("/employees"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("测试获取员工详情")
    void testGetEmployee() throws Exception {
        when(employeeService.getEmployeeById(1L)).thenReturn(createTestEmployee());

        mockMvc.perform(get("/employees/{id}", 1L))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("测试创建员工")
    void testCreateEmployee() throws Exception {
        when(employeeService.createEmployee(any(), anyString())).thenReturn(createTestEmployee());

        mockMvc.perform(post("/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"employeeNo\":\"EMP001\",\"name\":\"Test Employee\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("测试更新员工")
    void testUpdateEmployee() throws Exception {
        when(employeeService.updateEmployee(eq(1L), any(), anyString())).thenReturn(createTestEmployee());

        mockMvc.perform(put("/employees/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Updated Employee\"}"))
                .andExpect(status().isOk());
    }
}

