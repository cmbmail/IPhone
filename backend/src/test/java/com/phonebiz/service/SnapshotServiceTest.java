package com.phonebiz.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

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

import com.phonebiz.entity.CostCenterMapping;
import com.phonebiz.entity.Employee;
import com.phonebiz.entity.OrgStructure;
import com.phonebiz.entity.PhoneNumber;
import com.phonebiz.entity.PhoneSnapshot;
import com.phonebiz.repository.CostCenterMappingRepository;
import com.phonebiz.repository.EmployeeRepository;
import com.phonebiz.repository.OrgStructureRepository;
import com.phonebiz.repository.PhoneNumberRepository;
import com.phonebiz.repository.PhoneSnapshotRepository;

@ExtendWith(MockitoExtension.class)
class SnapshotServiceTest {

    @Mock
    private PhoneSnapshotRepository phoneSnapshotRepository;

    @Mock
    private PhoneNumberRepository phoneNumberRepository;

    @Mock
    private OrgStructureRepository orgStructureRepository;

    @Mock
    private CostCenterMappingRepository costCenterMappingRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private SnapshotService snapshotService;

    private PhoneNumber activePhone;
    private PhoneNumber stoppedPhone;
    private PhoneNumber cancelledPhone;
    private OrgStructure org;

    @BeforeEach
    void setUp() {
        org = new OrgStructure();
        org.setId(1L);
        org.setName("测试部门");

        activePhone = new PhoneNumber();
        activePhone.setId(1L);
        activePhone.setPhoneNumber("13800138001");
        activePhone.setExtensionNumber("1001");
        activePhone.setStatus(PhoneNumber.PhoneStatus.active);
        activePhone.setAllocationOrgId(1L);
        activePhone.setUserId("EMP001");

        stoppedPhone = new PhoneNumber();
        stoppedPhone.setId(2L);
        stoppedPhone.setPhoneNumber("13800138002");
        stoppedPhone.setExtensionNumber("1002");
        stoppedPhone.setStatus(PhoneNumber.PhoneStatus.stopped);
        stoppedPhone.setAllocationOrgId(1L);
        stoppedPhone.setUserId("EMP002");

        cancelledPhone = new PhoneNumber();
        cancelledPhone.setId(3L);
        cancelledPhone.setPhoneNumber("13800138003");
        cancelledPhone.setExtensionNumber("1003");
        cancelledPhone.setStatus(PhoneNumber.PhoneStatus.cancelled);
        cancelledPhone.setAllocationOrgId(1L);
        cancelledPhone.setUserId(null);
    }

    @Test
    @DisplayName("测试生成快照 - 首次生成成功")
    void testGenerateSnapshot_Success() {
        String snapshotMonth = "2024-01";

        when(phoneSnapshotRepository.countBySnapshotMonth(snapshotMonth)).thenReturn(0);
        when(phoneNumberRepository.findAll()).thenReturn(Arrays.asList(activePhone, stoppedPhone, cancelledPhone));
        when(orgStructureRepository.findAll()).thenReturn(List.of(org));
        when(costCenterMappingRepository.findByStatus(CostCenterMapping.CostCenterStatus.active)).thenReturn(List.of());
        when(employeeRepository.findByStatus(Employee.EmployeeStatus.active)).thenReturn(List.of());
        when(phoneSnapshotRepository.save(any(PhoneSnapshot.class))).thenAnswer(invocation -> invocation.getArgument(0));

        snapshotService.generateSnapshot(snapshotMonth);

        verify(phoneSnapshotRepository, times(3)).save(any(PhoneSnapshot.class));
    }

    @Test
    @DisplayName("测试生成快照 - 已存在则跳过")
    void testGenerateSnapshot_AlreadyExists() {
        String snapshotMonth = "2024-01";

        when(phoneSnapshotRepository.countBySnapshotMonth(snapshotMonth)).thenReturn(5);

        snapshotService.generateSnapshot(snapshotMonth);

        verify(phoneSnapshotRepository, never()).save(any(PhoneSnapshot.class));
    }

    @Test
    @DisplayName("测试获取快照列表")
    void testGetSnapshotsByMonth() {
        String snapshotMonth = "2024-01";
        PhoneSnapshot snapshot = PhoneSnapshot.builder()
                .id(1L)
                .snapshotMonth(snapshotMonth)
                .phoneId(1L)
                .phoneNumber("13800138001")
                .status("ACTIVE")
                .build();

        when(phoneSnapshotRepository.findBySnapshotMonth(snapshotMonth)).thenReturn(List.of(snapshot));

        List<PhoneSnapshot> result = snapshotService.getSnapshotsByMonth(snapshotMonth);

        assertEquals(1, result.size());
        assertEquals("13800138001", result.get(0).getPhoneNumber());
    }

    @Test
    @DisplayName("测试获取可用月份")
    void testGetAvailableMonths() {
        when(phoneSnapshotRepository.findDistinctSnapshotMonths()).thenReturn(Arrays.asList("2024-03", "2024-02", "2024-01"));

        List<String> result = snapshotService.getAvailableMonths();

        assertEquals(3, result.size());
        assertEquals("2024-03", result.get(0));
    }

    @Test
    @DisplayName("测试获取快照统计")
    void testGetSnapshotCount() {
        String snapshotMonth = "2024-01";

        when(phoneSnapshotRepository.countBySnapshotMonth(snapshotMonth)).thenReturn(100);

        int total = snapshotService.getSnapshotCount(snapshotMonth);

        assertEquals(100, total);
    }

    @Test
    @DisplayName("测试获取快照统计按状态")
    void testGetSnapshotCountByStatus() {
        String snapshotMonth = "2024-01";

        when(phoneSnapshotRepository.countBySnapshotMonthAndStatus(snapshotMonth, "ACTIVE")).thenReturn(80);

        int active = snapshotService.getSnapshotCountByStatus(snapshotMonth, "ACTIVE");

        assertEquals(80, active);
    }

    @Test
    @DisplayName("测试手动触发快照")
    void testTriggerSnapshot() {
        String snapshotMonth = "2024-01";

        when(phoneSnapshotRepository.countBySnapshotMonth(snapshotMonth)).thenReturn(0);
        when(phoneNumberRepository.findAll()).thenReturn(List.of());
        when(orgStructureRepository.findAll()).thenReturn(List.of());
        when(costCenterMappingRepository.findByStatus(CostCenterMapping.CostCenterStatus.active)).thenReturn(List.of());
        when(employeeRepository.findByStatus(Employee.EmployeeStatus.active)).thenReturn(List.of());

        assertDoesNotThrow(() -> snapshotService.triggerSnapshot(snapshotMonth));
    }

    @Test
    @DisplayName("测试手动触发快照 - 月份格式错误")
    void testTriggerSnapshot_InvalidFormat() {
        assertThrows(Exception.class, () -> snapshotService.triggerSnapshot("2024/01"));
        assertThrows(Exception.class, () -> snapshotService.triggerSnapshot("2024"));
        assertThrows(Exception.class, () -> snapshotService.triggerSnapshot("2024-13"));
    }

    @Test
    @DisplayName("测试重新生成快照")
    void testRegenerateSnapshot() {
        String snapshotMonth = "2024-01";

        when(phoneNumberRepository.findAll()).thenReturn(List.of(activePhone));
        when(orgStructureRepository.findAll()).thenReturn(List.of());
        when(costCenterMappingRepository.findByStatus(CostCenterMapping.CostCenterStatus.active)).thenReturn(List.of());
        when(employeeRepository.findByStatus(Employee.EmployeeStatus.active)).thenReturn(List.of());
        when(phoneSnapshotRepository.save(any(PhoneSnapshot.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertDoesNotThrow(() -> snapshotService.regenerateSnapshot(snapshotMonth));

        verify(phoneSnapshotRepository).deleteBySnapshotMonth(snapshotMonth);
        verify(phoneSnapshotRepository).save(any(PhoneSnapshot.class));
    }

    @Test
    @DisplayName("测试获取单个快照")
    void testGetSnapshot() {
        Long snapshotId = 1L;
        PhoneSnapshot snapshot = PhoneSnapshot.builder()
                .id(snapshotId)
                .snapshotMonth("2024-01")
                .phoneId(1L)
                .phoneNumber("13800138001")
                .build();

        when(phoneSnapshotRepository.findById(snapshotId)).thenReturn(Optional.of(snapshot));

        PhoneSnapshot result = snapshotService.getSnapshot(snapshotId);

        assertNotNull(result);
        assertEquals(snapshotId, result.getId());
    }

    @Test
    @DisplayName("测试获取单个快照 - 不存在")
    void testGetSnapshot_NotFound() {
        when(phoneSnapshotRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> snapshotService.getSnapshot(99L));
    }

    @Test
    @DisplayName("测试获取快照统计 - 空结果")
    void testGetSnapshotCount_Empty() {
        when(phoneSnapshotRepository.countBySnapshotMonth("2024-01")).thenReturn(0);

        int total = snapshotService.getSnapshotCount("2024-01");

        assertEquals(0, total);
    }

    @Test
    @DisplayName("测试获取快照列表 - 空结果")
    void testGetSnapshotsByMonth_Empty() {
        when(phoneSnapshotRepository.findBySnapshotMonth("2024-01")).thenReturn(List.of());

        List<PhoneSnapshot> result = snapshotService.getSnapshotsByMonth("2024-01");

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("测试获取可用月份 - 空结果")
    void testGetAvailableMonths_Empty() {
        when(phoneSnapshotRepository.findDistinctSnapshotMonths()).thenReturn(List.of());

        List<String> result = snapshotService.getAvailableMonths();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("测试获取快照统计按状态 - 空结果")
    void testGetSnapshotCountByStatus_Empty() {
        when(phoneSnapshotRepository.countBySnapshotMonthAndStatus("2024-01", "ACTIVE")).thenReturn(0);

        int active = snapshotService.getSnapshotCountByStatus("2024-01", "ACTIVE");

        assertEquals(0, active);
    }
}