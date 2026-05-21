package com.phonebiz.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.phonebiz.common.BusinessException;
import com.phonebiz.dto.CreatePhoneRequest;
import com.phonebiz.dto.PhoneAllocationRequest;
import com.phonebiz.dto.PhoneChangeRequest;
import com.phonebiz.dto.PhoneReclaimRequest;
import com.phonebiz.dto.PhoneSurrenderRequest;
import com.phonebiz.dto.UpdatePhoneRequest;
import com.phonebiz.entity.Employee;
import com.phonebiz.entity.OrgStructure;
import com.phonebiz.entity.PhoneHistory;
import com.phonebiz.entity.PhoneNumber;
import com.phonebiz.entity.PhoneSurrenderRecord;
import com.phonebiz.repository.EmployeeRepository;
import com.phonebiz.repository.OrgStructureRepository;
import com.phonebiz.repository.PhoneHistoryRepository;
import com.phonebiz.repository.PhoneNumberRepository;
import com.phonebiz.repository.PhoneSurrenderRecordRepository;

@ExtendWith(MockitoExtension.class)
class PhoneServiceTest {

    @Mock
    private PhoneNumberRepository phoneRepository;

    @Mock
    private PhoneHistoryRepository historyRepository;

    @Mock
    private PhoneSurrenderRecordRepository surrenderRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private OrgStructureRepository orgRepository;

    @InjectMocks
    private PhoneService phoneService;

    private PhoneNumber testPhone;

    @BeforeEach
    void setUp() {
        testPhone = new PhoneNumber();
        testPhone.setId(1L);
        testPhone.setPhoneNumber("13800138000");
        testPhone.setStatus(PhoneNumber.PhoneStatus.idle);
        testPhone.setUserId(null);
        testPhone.setOrgId(null);
    }

    @Test
    @DisplayName("测试分配号码 - 成功")
    void testAllocatePhone_Success() {
        when(phoneRepository.findByIdWithLock(1L)).thenReturn(Optional.of(testPhone));
        when(employeeRepository.existsByEmployeeNo("EMP001")).thenReturn(true);
        when(orgRepository.existsById(1L)).thenReturn(true);
        when(phoneRepository.save(any(PhoneNumber.class))).thenReturn(testPhone);
        when(phoneRepository.findById(1L)).thenReturn(Optional.of(testPhone));
        when(historyRepository.save(any(PhoneHistory.class))).thenReturn(new PhoneHistory());

        PhoneAllocationRequest request = new PhoneAllocationRequest();
        request.setPhoneId(1L);
        request.setUserId("EMP001");
        request.setOrgId(1L);

        PhoneNumber result = phoneService.allocatePhone(request, "admin");

        assertNotNull(result);
        assertEquals(PhoneNumber.PhoneStatus.active, result.getStatus());
        assertEquals("EMP001", result.getUserId());
        assertEquals(1L, result.getOrgId());
        verify(phoneRepository).save(any(PhoneNumber.class));
        verify(historyRepository).save(any(PhoneHistory.class));
    }

    @Test
    @DisplayName("测试分配号码 - 号码不存在")
    void testAllocatePhone_PhoneNotFound() {
        when(phoneRepository.findByIdWithLock(1L)).thenReturn(Optional.empty());

        PhoneAllocationRequest request = new PhoneAllocationRequest();
        request.setPhoneId(1L);
        request.setUserId("EMP001");
        request.setOrgId(1L);

        assertThrows(BusinessException.class, () -> phoneService.allocatePhone(request, "admin"));
    }

    @Test
    @DisplayName("测试分配号码 - 非空闲状态")
    void testAllocatePhone_NotIdle() {
        testPhone.setStatus(PhoneNumber.PhoneStatus.active);
        when(phoneRepository.findByIdWithLock(1L)).thenReturn(Optional.of(testPhone));

        PhoneAllocationRequest request = new PhoneAllocationRequest();
        request.setPhoneId(1L);
        request.setUserId("EMP001");
        request.setOrgId(1L);

        assertThrows(BusinessException.class, () -> phoneService.allocatePhone(request, "admin"));
    }

    @Test
    @DisplayName("测试回收号码 - 成功")
    void testReclaimPhone_Success() {
        testPhone.setStatus(PhoneNumber.PhoneStatus.active);
        testPhone.setUserId("EMP001");
        testPhone.setOrgId(1L);

        when(phoneRepository.findByIdWithLock(1L)).thenReturn(Optional.of(testPhone));
        when(phoneRepository.save(any(PhoneNumber.class))).thenReturn(testPhone);
        when(phoneRepository.findById(1L)).thenReturn(Optional.of(testPhone));
        when(historyRepository.save(any(PhoneHistory.class))).thenReturn(new PhoneHistory());

        PhoneReclaimRequest request = new PhoneReclaimRequest();
        request.setPhoneId(1L);
        request.setReason("Employee resigned");

        PhoneNumber result = phoneService.reclaimPhone(request, "admin");

        assertNotNull(result);
        assertEquals(PhoneNumber.PhoneStatus.idle, result.getStatus());
        verify(phoneRepository).save(any(PhoneNumber.class));
        verify(historyRepository).save(any(PhoneHistory.class));
    }

    @Test
    @DisplayName("测试回收号码 - 非活跃状态")
    void testReclaimPhone_NotActive() {
        when(phoneRepository.findByIdWithLock(1L)).thenReturn(Optional.of(testPhone));

        PhoneReclaimRequest request = new PhoneReclaimRequest();
        request.setPhoneId(1L);

        assertThrows(BusinessException.class, () -> phoneService.reclaimPhone(request, "admin"));
    }

    @Test
    @DisplayName("测试预约号码 - 成功")
    void testReservePhone_Success() {
        when(phoneRepository.findByIdWithLock(1L)).thenReturn(Optional.of(testPhone));
        when(phoneRepository.save(any(PhoneNumber.class))).thenReturn(testPhone);
        when(phoneRepository.findById(1L)).thenReturn(Optional.of(testPhone));
        when(historyRepository.save(any(PhoneHistory.class))).thenReturn(new PhoneHistory());

        PhoneNumber result = phoneService.reservePhone(1L, "admin", "WO001", "Reserved for new hire");

        assertNotNull(result);
        assertEquals(PhoneNumber.PhoneStatus.reserved, result.getStatus());
        verify(phoneRepository).save(any(PhoneNumber.class));
        verify(historyRepository).save(any(PhoneHistory.class));
    }

    @Test
    @DisplayName("测试预约号码 - 非空闲状态")
    void testReservePhone_NotIdle() {
        testPhone.setStatus(PhoneNumber.PhoneStatus.active);
        when(phoneRepository.findByIdWithLock(1L)).thenReturn(Optional.of(testPhone));

        assertThrows(BusinessException.class, () -> phoneService.reservePhone(1L, "admin", "WO001", "Test"));
    }

    @Test
    @DisplayName("测试释放预约 - 成功")
    void testReleasePhone_Success() {
        testPhone.setStatus(PhoneNumber.PhoneStatus.reserved);
        when(phoneRepository.findByIdWithLock(1L)).thenReturn(Optional.of(testPhone));
        when(phoneRepository.save(any(PhoneNumber.class))).thenReturn(testPhone);
        when(phoneRepository.findById(1L)).thenReturn(Optional.of(testPhone));
        when(historyRepository.save(any(PhoneHistory.class))).thenReturn(new PhoneHistory());

        PhoneNumber result = phoneService.releasePhone(1L, "admin", "WO001", "Release");

        assertNotNull(result);
        assertEquals(PhoneNumber.PhoneStatus.idle, result.getStatus());
        verify(phoneRepository).save(any(PhoneNumber.class));
        verify(historyRepository).save(any(PhoneHistory.class));
    }

    @Test
    @DisplayName("测试状态变更 - 有效转换")
    void testChangeStatus_ValidTransition() {
        when(phoneRepository.findByIdWithLock(1L)).thenReturn(Optional.of(testPhone));
        when(phoneRepository.save(any(PhoneNumber.class))).thenReturn(testPhone);
        when(phoneRepository.findById(1L)).thenReturn(Optional.of(testPhone));
        when(historyRepository.save(any(PhoneHistory.class))).thenReturn(new PhoneHistory());

        PhoneNumber result = phoneService.changeStatus(1L, PhoneNumber.PhoneStatus.active, "admin", "WO001", "Test");

        assertNotNull(result);
        assertEquals(PhoneNumber.PhoneStatus.active, result.getStatus());
    }

    @Test
    @DisplayName("测试状态变更 - 无效转换")
    void testChangeStatus_InvalidTransition() {
        testPhone.setStatus(PhoneNumber.PhoneStatus.cancelled);
        when(phoneRepository.findByIdWithLock(1L)).thenReturn(Optional.of(testPhone));

        assertThrows(BusinessException.class, () ->
            phoneService.changeStatus(1L, PhoneNumber.PhoneStatus.active, "admin", "WO001", "Test"));
    }

    @Test
    @DisplayName("测试拆机归档 - 成功")
    void testSurrenderPhone_Success() {
        testPhone.setStatus(PhoneNumber.PhoneStatus.active);
        testPhone.setUserId("EMP001");
        testPhone.setOrgId(1L);

        when(phoneRepository.findByIdWithLock(1L)).thenReturn(Optional.of(testPhone));
        when(surrenderRepository.save(any(PhoneSurrenderRecord.class))).thenReturn(new PhoneSurrenderRecord());
        when(phoneRepository.save(any(PhoneNumber.class))).thenReturn(testPhone);
        when(phoneRepository.findById(1L)).thenReturn(Optional.of(testPhone));
        when(historyRepository.save(any(PhoneHistory.class))).thenReturn(new PhoneHistory());

        PhoneSurrenderRecord result = phoneService.surrenderPhone(1L, "surrender", "admin", "WO001", "Test");

        assertNotNull(result);
        verify(surrenderRepository).save(any(PhoneSurrenderRecord.class));
        verify(phoneRepository).save(any(PhoneNumber.class));
        verify(historyRepository).save(any(PhoneHistory.class));
    }

    @Test
    @DisplayName("测试拆机归档 - 无效状态")
    void testSurrenderPhone_InvalidStatus() {
        when(phoneRepository.findByIdWithLock(1L)).thenReturn(Optional.of(testPhone));

        assertThrows(BusinessException.class, () ->
            phoneService.surrenderPhone(1L, "surrender", "admin", "WO001", "Test"));
    }

    @Test
    @DisplayName("测试创建号码 - 成功")
    void testCreatePhone_Success() {
        CreatePhoneRequest request = new CreatePhoneRequest();
        request.setPhoneNumber("13900139000");
        request.setOrgId(1L);

        when(phoneRepository.existsByPhoneNumber("13900139000")).thenReturn(false);
        when(phoneRepository.save(any(PhoneNumber.class))).thenReturn(testPhone);

        PhoneNumber result = phoneService.createPhone(request, "admin");

        assertNotNull(result);
        verify(phoneRepository).save(any(PhoneNumber.class));
    }

    @Test
    @DisplayName("测试创建号码 - 号码已存在")
    void testCreatePhone_AlreadyExists() {
        CreatePhoneRequest request = new CreatePhoneRequest();
        request.setPhoneNumber("13800138000");
        request.setOrgId(1L);

        when(phoneRepository.existsByPhoneNumber("13800138000")).thenReturn(true);

        assertThrows(BusinessException.class, () -> phoneService.createPhone(request, "admin"));
    }

    @Test
    @DisplayName("测试更新号码 - 成功")
    void testUpdatePhone_Success() {
        UpdatePhoneRequest request = new UpdatePhoneRequest();
        request.setRemark("Updated");

        when(phoneRepository.findById(1L)).thenReturn(Optional.of(testPhone));
        when(phoneRepository.save(any(PhoneNumber.class))).thenReturn(testPhone);

        PhoneNumber result = phoneService.updatePhone(1L, request, "admin");

        assertNotNull(result);
        verify(phoneRepository).save(any(PhoneNumber.class));
    }

    @Test
    @DisplayName("测试更新号码 - 号码不存在")
    void testUpdatePhone_NotFound() {
        UpdatePhoneRequest request = new UpdatePhoneRequest();
        when(phoneRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> phoneService.updatePhone(1L, request, "admin"));
    }

    @Test
    @DisplayName("测试过户 - 成功")
    void testChangeUser_Success() {
        testPhone.setStatus(PhoneNumber.PhoneStatus.active);
        testPhone.setUserId("EMP001");

        when(phoneRepository.findByIdWithLock(1L)).thenReturn(Optional.of(testPhone));
        when(employeeRepository.existsByEmployeeNo("EMP002")).thenReturn(true);
        when(phoneRepository.save(any(PhoneNumber.class))).thenReturn(testPhone);
        when(phoneRepository.findById(1L)).thenReturn(Optional.of(testPhone));
        when(historyRepository.save(any(PhoneHistory.class))).thenReturn(new PhoneHistory());

        PhoneNumber result = phoneService.changeUser(1L, "EMP002", "admin", "WO001", "Transfer");

        assertNotNull(result);
        assertEquals("EMP002", result.getUserId());
    }

    @Test
    @DisplayName("测试过户 - 用户不存在")
    void testChangeUser_UserNotFound() {
        testPhone.setStatus(PhoneNumber.PhoneStatus.active);

        when(phoneRepository.findByIdWithLock(1L)).thenReturn(Optional.of(testPhone));
        when(employeeRepository.existsByEmployeeNo("EMP002")).thenReturn(false);

        assertThrows(BusinessException.class, () ->
            phoneService.changeUser(1L, "EMP002", "admin", "WO001", "Test"));
    }

    @Test
    @DisplayName("测试转移组织 - 成功")
    void testChangeOrg_Success() {
        testPhone.setStatus(PhoneNumber.PhoneStatus.active);
        testPhone.setOrgId(1L);

        when(phoneRepository.findByIdWithLock(1L)).thenReturn(Optional.of(testPhone));
        when(orgRepository.existsById(2L)).thenReturn(true);
        when(phoneRepository.save(any(PhoneNumber.class))).thenReturn(testPhone);
        when(phoneRepository.findById(1L)).thenReturn(Optional.of(testPhone));
        when(historyRepository.save(any(PhoneHistory.class))).thenReturn(new PhoneHistory());

        PhoneNumber result = phoneService.changeOrg(1L, 2L, "admin", "WO001", "Transfer");

        assertNotNull(result);
        assertEquals(2L, result.getOrgId());
    }

    @Test
    @DisplayName("测试转移组织 - 组织不存在")
    void testChangeOrg_OrgNotFound() {
        testPhone.setStatus(PhoneNumber.PhoneStatus.active);

        when(phoneRepository.findByIdWithLock(1L)).thenReturn(Optional.of(testPhone));
        when(orgRepository.existsById(2L)).thenReturn(false);

        assertThrows(BusinessException.class, () ->
            phoneService.changeOrg(1L, 2L, "admin", "WO001", "Test"));
    }

    @Test
    @DisplayName("测试获取空闲号码")
    void testGetIdlePhones() {
        when(phoneRepository.findIdlePhones()).thenReturn(Arrays.asList(testPhone));

        var result = phoneService.getIdlePhones();

        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    @DisplayName("测试按号码查询")
    void testGetPhoneByNumber() {
        when(phoneRepository.findByPhoneNumber("13800138000")).thenReturn(Optional.of(testPhone));

        PhoneNumber result = phoneService.getPhoneByNumber("13800138000");

        assertNotNull(result);
        assertEquals("13800138000", result.getPhoneNumber());
    }

    @Test
    @DisplayName("测试按号码查询 - 不存在")
    void testGetPhoneByNumber_NotFound() {
        when(phoneRepository.findByPhoneNumber("13800138000")).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> phoneService.getPhoneByNumber("13800138000"));
    }

    @Test
    @DisplayName("测试按用户查询")
    void testGetPhonesByUser() {
        when(phoneRepository.findByUserId("EMP001")).thenReturn(Arrays.asList(testPhone));

        var result = phoneService.getPhonesByUser("EMP001");

        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    @DisplayName("测试交换号码 - 成功")
    void testSwapPhoneNumbers_Success() {
        PhoneNumber phone1 = new PhoneNumber();
        phone1.setId(1L);
        phone1.setPhoneNumber("13800138001");
        phone1.setStatus(PhoneNumber.PhoneStatus.active);

        PhoneNumber phone2 = new PhoneNumber();
        phone2.setId(2L);
        phone2.setPhoneNumber("13800138002");
        phone2.setStatus(PhoneNumber.PhoneStatus.active);

        when(phoneRepository.findByIdsForUpdate(Arrays.asList(1L, 2L)))
                .thenReturn(Arrays.asList(phone1, phone2));
        when(phoneRepository.saveAll(anyList())).thenReturn(Arrays.asList(phone1, phone2));
        when(phoneRepository.findById(anyLong())).thenReturn(Optional.of(phone1), Optional.of(phone2));
        when(historyRepository.save(any(PhoneHistory.class))).thenReturn(new PhoneHistory());

        phoneService.swapPhoneNumbers(1L, 2L, "admin", "WO001", "Test swap");

        assertEquals("13800138002", phone1.getPhoneNumber());
        assertEquals("13800138001", phone2.getPhoneNumber());
        verify(phoneRepository).saveAll(anyList());
        verify(historyRepository, times(2)).save(any(PhoneHistory.class));
    }

    @Test
    @DisplayName("测试交换号码 - 相同号码")
    void testSwapPhoneNumbers_SamePhone() {
        assertThrows(BusinessException.class, () ->
            phoneService.swapPhoneNumbers(1L, 1L, "admin", "WO001", "Test"));
    }

    @Test
    @DisplayName("测试交换号码 - 号码不存在")
    void testSwapPhoneNumbers_PhoneNotFound() {
        when(phoneRepository.findByIdsForUpdate(Arrays.asList(1L, 2L))).thenReturn(Arrays.asList(testPhone));

        assertThrows(BusinessException.class, () ->
            phoneService.swapPhoneNumbers(1L, 2L, "admin", "WO001", "Test"));
    }
}

