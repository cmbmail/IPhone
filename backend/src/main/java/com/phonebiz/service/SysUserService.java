package com.phonebiz.service;

import com.phonebiz.entity.Employee;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SysUserService {

    public void createUserForEmployee(Employee employee, String operator) {
        // TODO: M04 will implement real user creation
        log.info("Creating sys_user for employee: {} (will be implemented in M04)", employee.getEmployeeNo());
    }
}
