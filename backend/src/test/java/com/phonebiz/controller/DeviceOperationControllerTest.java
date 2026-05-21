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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.web.servlet.MockMvc;

import com.phonebiz.config.TestSecurityConfig;
import com.phonebiz.entity.DeviceOperation;
import com.phonebiz.service.DeviceOperationService;

@WebMvcTest(DeviceOperationController.class)
@Import(TestSecurityConfig.class)
class DeviceOperationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DeviceOperationService operationService;

    private DeviceOperation createTestOperation() {
        DeviceOperation operation = new DeviceOperation();
        operation.setId(1L);
        operation.setDeviceId("device001");
        operation.setOperationType(DeviceOperation.OperationType.REBOOT);
        operation.setStatus(DeviceOperation.OperationStatus.COMPLETED);
        return operation;
    }

    @Test
    @DisplayName("测试获取操作列表")
    void testGetOperations() throws Exception {
        Page<DeviceOperation> page = new PageImpl<>(List.of(createTestOperation()));
        when(operationService.getOperations(any())).thenReturn(page);

        mockMvc.perform(get("/device-operations"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("测试获取操作详情")
    void testGetOperationById() throws Exception {
        when(operationService.getOperationById(1L)).thenReturn(createTestOperation());

        mockMvc.perform(get("/device-operations/{id}", 1L))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("测试获取设备操作历史")
    void testGetOperationsByDeviceId() throws Exception {
        when(operationService.getOperationsByDeviceId("device001")).thenReturn(List.of(createTestOperation()));

        mockMvc.perform(get("/device-operations/device/{deviceId}", "device001"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("测试执行操作")
    void testExecuteOperation() throws Exception {
        when(operationService.executeOperation(1L)).thenReturn(createTestOperation());

        mockMvc.perform(post("/device-operations/{operationId}/execute", 1L))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("测试重启设备")
    void testRebootDevice() throws Exception {
        when(operationService.rebootDevice("device001", "admin")).thenReturn(createTestOperation());

        mockMvc.perform(post("/device-operations/{deviceId}/reboot", "device001"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("测试同步配置")
    void testSyncConfig() throws Exception {
        DeviceOperation operation = createTestOperation();
        operation.setOperationType(DeviceOperation.OperationType.CONFIG_SYNC);
        when(operationService.syncConfig("device001", "admin")).thenReturn(operation);

        mockMvc.perform(post("/device-operations/{deviceId}/sync-config", "device001"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("测试升级固件")
    void testUpgradeFirmware() throws Exception {
        DeviceOperation operation = createTestOperation();
        operation.setOperationType(DeviceOperation.OperationType.FIRMWARE_UPGRADE);
        when(operationService.upgradeFirmware("device001", "v1.0", "admin")).thenReturn(operation);

        mockMvc.perform(post("/device-operations/{deviceId}/upgrade-firmware", "device001")
                        .param("firmwareVersion", "v1.0"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("测试恢复出厂设置")
    void testFactoryReset() throws Exception {
        DeviceOperation operation = createTestOperation();
        operation.setOperationType(DeviceOperation.OperationType.FACTORY_RESET);
        when(operationService.factoryReset("device001", "admin")).thenReturn(operation);

        mockMvc.perform(post("/device-operations/{deviceId}/factory-reset", "device001"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("测试注册设备")
    void testRegisterDevice() throws Exception {
        DeviceOperation operation = createTestOperation();
        operation.setOperationType(DeviceOperation.OperationType.REGISTER);
        when(operationService.registerDevice("device001", "admin")).thenReturn(operation);

        mockMvc.perform(post("/device-operations/{deviceId}/register", "device001"))
                .andExpect(status().isOk());
    }
}

