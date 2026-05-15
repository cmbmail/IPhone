package com.phonebiz.controller;

import com.phonebiz.common.ApiResponse;
import com.phonebiz.dto.CreateEmployeeRequest;
import com.phonebiz.dto.UpdateEmployeeRequest;
import com.phonebiz.entity.Employee;
import com.phonebiz.service.EmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    @GetMapping
    public ApiResponse<Page<Employee>> getEmployees(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("employeeNo").ascending());
        return ApiResponse.success(employeeService.getEmployees(pageable));
    }

    @GetMapping("/{id}")
    public ApiResponse<Employee> getEmployeeById(@PathVariable Long id) {
        return ApiResponse.success(employeeService.getEmployeeById(id));
    }

    @GetMapping("/by-no/{employeeNo}")
    public ApiResponse<Employee> getEmployeeByEmployeeNo(@PathVariable String employeeNo) {
        return ApiResponse.success(employeeService.getEmployeeByEmployeeNo(employeeNo));
    }

    @GetMapping("/by-org/{orgId}")
    public ApiResponse<List<Employee>> getEmployeesByOrg(@PathVariable Long orgId) {
        return ApiResponse.success(employeeService.getEmployeesByOrg(orgId));
    }

    @GetMapping("/active")
    public ApiResponse<List<Employee>> getAllActiveEmployees() {
        return ApiResponse.success(employeeService.getAllActiveEmployees());
    }

    @PostMapping
    public ApiResponse<Employee> createEmployee(
            @Valid @RequestBody CreateEmployeeRequest request,
            Authentication authentication) {
        String operator = authentication != null ? authentication.getName() : "system";
        return ApiResponse.success(employeeService.createEmployee(request, operator));
    }

    @PutMapping("/{id}")
    public ApiResponse<Employee> updateEmployee(
            @PathVariable Long id,
            @Valid @RequestBody UpdateEmployeeRequest request,
            Authentication authentication) {
        String operator = authentication != null ? authentication.getName() : "system";
        return ApiResponse.success(employeeService.updateEmployee(id, request, operator));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteEmployee(@PathVariable Long id) {
        employeeService.deleteEmployee(id);
        return ApiResponse.success("Employee deactivated successfully", null);
    }

    @GetMapping("/count/{orgId}")
    public ApiResponse<Long> countByOrg(@PathVariable Long orgId) {
        return ApiResponse.success(employeeService.countByOrg(orgId));
    }
}
