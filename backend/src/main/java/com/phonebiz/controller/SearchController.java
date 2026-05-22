package com.phonebiz.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

import com.phonebiz.common.ApiResponse;
import com.phonebiz.entity.PhoneNumber;
import com.phonebiz.entity.ExtensionNumber;
import com.phonebiz.entity.PhoneDevice;
import com.phonebiz.entity.WorkOrder;
import com.phonebiz.entity.Employee;
import com.phonebiz.repository.PhoneNumberRepository;
import com.phonebiz.repository.ExtensionNumberRepository;
import com.phonebiz.repository.PhoneDeviceRepository;
import com.phonebiz.repository.WorkOrderRepository;
import com.phonebiz.repository.EmployeeRepository;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.HashMap;

@RestController
@RequestMapping("/search")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class SearchController {

    private final PhoneNumberRepository phoneNumberRepository;
    private final ExtensionNumberRepository extensionNumberRepository;
    private final PhoneDeviceRepository phoneDeviceRepository;
    private final WorkOrderRepository workOrderRepository;
    private final EmployeeRepository employeeRepository;

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> globalSearch(@RequestParam String q) {
        List<Map<String, Object>> results = new ArrayList<>();
        String keyword = q.trim();
        if (keyword.isEmpty()) {
            return ApiResponse.success(results);
        }

        // Search phone numbers
        try {
            List<PhoneNumber> phones = phoneNumberRepository.searchByKeyword(keyword);
            for (PhoneNumber p : phones.subList(0, Math.min(5, phones.size()))) {
                Map<String, Object> item = new HashMap<>();
                item.put("type", "phone");
                item.put("id", p.getId());
                item.put("label", p.getPhoneNumber());
                item.put("subLabel", "号码资源");
                item.put("route", "/phones");
                results.add(item);
            }
        } catch (Exception ignored) {}

        // Search extension numbers
        try {
            List<ExtensionNumber> exts = extensionNumberRepository.searchByKeyword(keyword);
            for (ExtensionNumber e : exts.subList(0, Math.min(5, exts.size()))) {
                Map<String, Object> item = new HashMap<>();
                item.put("type", "extension");
                item.put("id", e.getId());
                item.put("label", e.getExtensionNumber());
                item.put("subLabel", "分机 - " + (e.getUserName() != null ? e.getUserName() : "未分配"));
                item.put("route", "/extension-pools");
                results.add(item);
            }
        } catch (Exception ignored) {}

        // Search work orders
        try {
            List<WorkOrder> orders = workOrderRepository.searchByKeyword(keyword);
            for (WorkOrder w : orders.subList(0, Math.min(5, orders.size()))) {
                Map<String, Object> item = new HashMap<>();
                item.put("type", "work-order");
                item.put("id", w.getId());
                item.put("label", w.getWorkOrderNo());
                item.put("subLabel", w.getTitle());
                item.put("route", "/work-orders");
                results.add(item);
            }
        } catch (Exception ignored) {}

        // Search employees
        try {
            List<Employee> employees = employeeRepository.searchByKeyword(keyword);
            for (Employee emp : employees.subList(0, Math.min(5, employees.size()))) {
                Map<String, Object> item = new HashMap<>();
                item.put("type", "employee");
                item.put("id", emp.getId());
                item.put("label", emp.getName());
                item.put("subLabel", emp.getEmployeeNo());
                item.put("route", "/orgs");
                results.add(item);
            }
        } catch (Exception ignored) {}

        // Search devices
        try {
            List<PhoneDevice> devices = phoneDeviceRepository.searchByKeyword(keyword);
            for (PhoneDevice d : devices.subList(0, Math.min(5, devices.size()))) {
                Map<String, Object> item = new HashMap<>();
                item.put("type", "device");
                item.put("id", d.getId());
                item.put("label", d.getMacAddress());
                item.put("subLabel", "话机设备");
                item.put("route", "/devices");
                results.add(item);
            }
        } catch (Exception ignored) {}

        return ApiResponse.success(results.subList(0, Math.min(20, results.size())));
    }
}
