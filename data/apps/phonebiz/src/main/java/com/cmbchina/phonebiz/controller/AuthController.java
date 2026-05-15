package com.cmbchina.phonebiz.controller;

import com.cmbchina.phonebiz.annotation.AuditLog;
import com.cmbchina.phonebiz.common.Result;
import com.cmbchina.phonebiz.dto.LoginRequest;
import com.cmbchina.phonebiz.dto.LoginResponse;
import com.cmbchina.phonebiz.entity.SysEmployee;
import com.cmbchina.phonebiz.entity.SysMenu;
import com.cmbchina.phonebiz.entity.SysRole;
import com.cmbchina.phonebiz.service.SysEmployeeService;
import com.cmbchina.phonebiz.service.SysRoleService;
import com.cmbchina.phonebiz.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.DigestUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private SysEmployeeService sysEmployeeService;

    @Autowired
    private SysRoleService sysRoleService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/login")
    @AuditLog(module = "登录认证", operation = "用户登录")
    public Result<LoginResponse> login(@Validated @RequestBody LoginRequest request) {
        SysEmployee employee = sysEmployeeService.getEmployeeByUsername(request.getUsername());
        if (employee == null) {
            return Result.error("用户名或密码错误");
        }
        if (employee.getStatus() == 0) {
            return Result.error("用户已被禁用");
        }
        String md5Password = DigestUtils.md5DigestAsHex(request.getPassword().getBytes());
        if (!md5Password.equals(employee.getPassword())) {
            return Result.error("用户名或密码错误");
        }
        String token = jwtUtil.generateToken(employee.getId(), employee.getUsername());
        LoginResponse response = new LoginResponse(token, employee.getUsername(), employee.getRealName());
        return Result.success(response);
    }

    @GetMapping("/user/info")
    @AuditLog(module = "登录认证", operation = "获取用户信息")
    public Result<Map<String, Object>> getUserInfo(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String username = (String) request.getAttribute("username");
        
        SysEmployee employee = sysEmployeeService.getEmployeeById(userId);
        List<SysRole> roles = sysEmployeeService.getRolesByEmployeeId(userId);
        List<SysMenu> menus = sysRoleService.getMenusByEmployeeId(userId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        result.put("username", username);
        result.put("realName", employee.getRealName());
        result.put("roles", roles);
        result.put("menus", menus);
        
        return Result.success(result);
    }

    @GetMapping("/user/menus")
    @AuditLog(module = "登录认证", operation = "获取用户菜单")
    public Result<List<SysMenu>> getUserMenus(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        List<SysMenu> menus = sysRoleService.getMenusByEmployeeId(userId);
        return Result.success(menus);
    }
}