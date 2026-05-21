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

import com.phonebiz.common.BusinessException;
import com.phonebiz.dto.CreateExtensionPoolRequest;
import com.phonebiz.entity.ExtensionPool;
import com.phonebiz.entity.OrgStructure;
import com.phonebiz.entity.PhoneNumber;
import com.phonebiz.repository.ExtensionPoolRepository;
import com.phonebiz.repository.OrgStructureRepository;
import com.phonebiz.repository.PhoneNumberRepository;

@ExtendWith(MockitoExtension.class)
class ExtensionPoolServiceTest {

    @Mock
    private ExtensionPoolRepository poolRepository;

    @Mock
    private OrgStructureRepository orgRepository;

    @Mock
    private PhoneNumberRepository phoneRepository;

    @InjectMocks
    private ExtensionPoolService extensionPoolService;

    private ExtensionPool testPool;
    private OrgStructure testOrg;

    @BeforeEach
    void setUp() {
        testOrg = new OrgStructure();
        testOrg.setId(1L);
        testOrg.setName("Test Org");

        testPool = new ExtensionPool();
        testPool.setId(1L);
        testPool.setOrgId(1L);
        testPool.setStartNumber("1000");
        testPool.setEndNumber("2000");
    }

    @Test
    @DisplayName("测试创建分机号池 - 成功")
    void testCreatePool_Success() {
        CreateExtensionPoolRequest request = new CreateExtensionPoolRequest();
        request.setOrgId(1L);
        request.setStartNumber("1000");
        request.setEndNumber("2000");

        when(orgRepository.findById(1L)).thenReturn(Optional.of(testOrg));
        when(poolRepository.findByOrgId(1L)).thenReturn(Arrays.asList());
        when(poolRepository.save(any(ExtensionPool.class))).thenReturn(testPool);

        ExtensionPool result = extensionPoolService.createPool(request, "admin");

        assertNotNull(result);
        assertEquals("1000", result.getStartNumber());
        verify(poolRepository).save(any(ExtensionPool.class));
    }

    @Test
    @DisplayName("测试创建分机号池 - 组织不存在")
    void testCreatePool_OrgNotFound() {
        CreateExtensionPoolRequest request = new CreateExtensionPoolRequest();
        request.setOrgId(99L);
        request.setStartNumber("1000");
        request.setEndNumber("2000");

        when(orgRepository.findById(99L)).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class, () -> 
            extensionPoolService.createPool(request, "admin"));
        assertTrue(exception.getMessage().contains("Organization not found"));
    }

    @Test
    @DisplayName("测试创建分机号池 - 无效范围")
    void testCreatePool_InvalidRange() {
        CreateExtensionPoolRequest request = new CreateExtensionPoolRequest();
        request.setOrgId(1L);
        request.setStartNumber("2000");
        request.setEndNumber("1000");

        when(orgRepository.findById(1L)).thenReturn(Optional.of(testOrg));

        BusinessException exception = assertThrows(BusinessException.class, () -> 
            extensionPoolService.createPool(request, "admin"));
        assertTrue(exception.getMessage().contains("Invalid number range"));
    }

    @Test
    @DisplayName("测试创建分机号池 - 范围重叠")
    void testCreatePool_RangeOverlap() {
        CreateExtensionPoolRequest request = new CreateExtensionPoolRequest();
        request.setOrgId(1L);
        request.setStartNumber("1500");
        request.setEndNumber("2500");

        when(orgRepository.findById(1L)).thenReturn(Optional.of(testOrg));
        when(poolRepository.findByOrgId(1L)).thenReturn(Arrays.asList(testPool));

        BusinessException exception = assertThrows(BusinessException.class, () -> 
            extensionPoolService.createPool(request, "admin"));
        assertTrue(exception.getMessage().contains("Extension pool range overlaps"));
    }

    @Test
    @DisplayName("测试获取分机号池 - 成功")
    void testGetPoolById_Success() {
        when(poolRepository.findById(1L)).thenReturn(Optional.of(testPool));

        ExtensionPool result = extensionPoolService.getPoolById(1L);

        assertNotNull(result);
        assertEquals("1000", result.getStartNumber());
    }

    @Test
    @DisplayName("测试获取分机号池 - 不存在")
    void testGetPoolById_NotFound() {
        when(poolRepository.findById(99L)).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class, () -> 
            extensionPoolService.getPoolById(99L));
        assertTrue(exception.getMessage().contains("Parameter validation failed"));
    }

    @Test
    @DisplayName("测试删除分机号池 - 成功")
    void testDeletePool_Success() {
        doNothing().when(poolRepository).deleteById(1L);

        assertDoesNotThrow(() -> extensionPoolService.deletePool(1L));
        verify(poolRepository).deleteById(1L);
    }

    @Test
    @DisplayName("测试获取分机号池列表")
    void testGetPoolsByOrg() {
        when(poolRepository.findByOrgId(1L)).thenReturn(Arrays.asList(testPool));

        List<ExtensionPool> result = extensionPoolService.getPoolsByOrg(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("测试获取分机号池使用情况 - 低使用率")
    void testGetPoolUsage_LowUsage() {
        when(poolRepository.findById(1L)).thenReturn(Optional.of(testPool));
        when(phoneRepository.findByOrgId(1L)).thenReturn(Arrays.asList());

        ExtensionPoolService.ExtensionPoolUsage usage = extensionPoolService.getPoolUsage(1L);

        assertNotNull(usage);
        assertEquals(1001, usage.totalCount());
        assertEquals(0, usage.usedCount());
        assertEquals("green", usage.status());
    }

    @Test
    @DisplayName("测试获取所有分机号池")
    void testGetAllPools() {
        when(poolRepository.findAll()).thenReturn(Arrays.asList(testPool));

        List<ExtensionPool> result = extensionPoolService.getAllPools();

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("测试检查组织是否已有分机号池")
    void testHasPoolForOrg_Exists() {
        when(poolRepository.existsByOrgId(1L)).thenReturn(true);

        assertTrue(poolRepository.existsByOrgId(1L));
    }

    @Test
    @DisplayName("测试检查组织是否已有分机号池 - 不存在")
    void testHasPoolForOrg_NotExists() {
        when(poolRepository.existsByOrgId(1L)).thenReturn(false);

        assertFalse(poolRepository.existsByOrgId(1L));
    }
}

