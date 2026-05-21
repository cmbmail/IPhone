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
import com.phonebiz.dto.CreateCostCenterRequest;
import com.phonebiz.dto.UpdateCostCenterRequest;
import com.phonebiz.entity.CostCenterMapping;
import com.phonebiz.entity.OrgStructure;
import com.phonebiz.repository.CostCenterMappingRepository;
import com.phonebiz.repository.OrgStructureRepository;

@ExtendWith(MockitoExtension.class)
class CostCenterServiceTest {

    @Mock
    private CostCenterMappingRepository costCenterRepository;

    @Mock
    private OrgStructureRepository orgRepository;

    @InjectMocks
    private CostCenterService costCenterService;

    private CostCenterMapping testCostCenter;
    private OrgStructure testOrg;

    @BeforeEach
    void setUp() {
        testOrg = new OrgStructure();
        testOrg.setId(1L);
        testOrg.setName("Test Org");

        testCostCenter = new CostCenterMapping();
        testCostCenter.setId(1L);
        testCostCenter.setOrgId(1L);
        testCostCenter.setCostCenterCode("CC001");
        testCostCenter.setCostCenterName("Cost Center 1");
        testCostCenter.setStatus(CostCenterMapping.CostCenterStatus.active);
    }

    @Test
    @DisplayName("测试创建成本中心 - 成功")
    void testCreateCostCenter_Success() {
        CreateCostCenterRequest request = new CreateCostCenterRequest();
        request.setOrgId(1L);
        request.setCostCenterCode("CC001");
        request.setCostCenterName("Cost Center 1");
        request.setStatus("active");

        when(orgRepository.findById(1L)).thenReturn(Optional.of(testOrg));
        when(costCenterRepository.existsByOrgIdAndCostCenterCode(1L, "CC001")).thenReturn(false);
        when(costCenterRepository.save(any(CostCenterMapping.class))).thenReturn(testCostCenter);

        CostCenterMapping result = costCenterService.createCostCenter(request, "admin");

        assertNotNull(result);
        assertEquals("CC001", result.getCostCenterCode());
        verify(costCenterRepository).save(any(CostCenterMapping.class));
    }

    @Test
    @DisplayName("测试创建成本中心 - 组织不存在")
    void testCreateCostCenter_OrgNotFound() {
        CreateCostCenterRequest request = new CreateCostCenterRequest();
        request.setOrgId(99L);
        request.setCostCenterCode("CC001");
        request.setCostCenterName("Cost Center 1");

        when(orgRepository.findById(99L)).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class, () -> 
            costCenterService.createCostCenter(request, "admin"));
        assertTrue(exception.getMessage().contains("Organization not found"));
    }

    @Test
    @DisplayName("测试创建成本中心 - 编码重复")
    void testCreateCostCenter_DuplicateCode() {
        CreateCostCenterRequest request = new CreateCostCenterRequest();
        request.setOrgId(1L);
        request.setCostCenterCode("CC001");
        request.setCostCenterName("Cost Center 1");

        when(orgRepository.findById(1L)).thenReturn(Optional.of(testOrg));
        when(costCenterRepository.existsByOrgIdAndCostCenterCode(1L, "CC001")).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class, () -> 
            costCenterService.createCostCenter(request, "admin"));
        assertTrue(exception.getMessage().contains("already exists"));
    }

    @Test
    @DisplayName("测试获取成本中心 - 成功")
    void testGetCostCenterById_Success() {
        when(costCenterRepository.findById(1L)).thenReturn(Optional.of(testCostCenter));

        CostCenterMapping result = costCenterService.getCostCenterById(1L);

        assertNotNull(result);
        assertEquals("CC001", result.getCostCenterCode());
    }

    @Test
    @DisplayName("测试获取成本中心 - 不存在")
    void testGetCostCenterById_NotFound() {
        when(costCenterRepository.findById(99L)).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class, () -> 
            costCenterService.getCostCenterById(99L));
        assertTrue(exception.getMessage().contains("Parameter validation failed"));
    }

    @Test
    @DisplayName("测试更新成本中心 - 成功")
    void testUpdateCostCenter_Success() {
        UpdateCostCenterRequest request = new UpdateCostCenterRequest();
        request.setCostCenterName("Updated Name");

        when(costCenterRepository.findById(1L)).thenReturn(Optional.of(testCostCenter));
        when(costCenterRepository.save(any(CostCenterMapping.class))).thenReturn(testCostCenter);

        CostCenterMapping result = costCenterService.updateCostCenter(1L, request, "admin");

        assertNotNull(result);
        verify(costCenterRepository).save(any(CostCenterMapping.class));
    }

    @Test
    @DisplayName("测试更新成本中心 - 编码变更")
    void testUpdateCostCenter_ChangeCode() {
        UpdateCostCenterRequest request = new UpdateCostCenterRequest();
        request.setCostCenterCode("CC002");

        when(costCenterRepository.findById(1L)).thenReturn(Optional.of(testCostCenter));
        when(costCenterRepository.existsByOrgIdAndCostCenterCode(1L, "CC002")).thenReturn(false);
        when(costCenterRepository.save(any(CostCenterMapping.class))).thenReturn(testCostCenter);

        assertDoesNotThrow(() -> costCenterService.updateCostCenter(1L, request, "admin"));
        verify(costCenterRepository).save(any(CostCenterMapping.class));
    }

    @Test
    @DisplayName("测试删除成本中心 - 成功")
    void testDeleteCostCenter_Success() {
        doNothing().when(costCenterRepository).deleteById(1L);

        assertDoesNotThrow(() -> costCenterService.deleteCostCenter(1L));
        verify(costCenterRepository).deleteById(1L);
    }

    @Test
    @DisplayName("测试获取组织下成本中心列表")
    void testGetCostCentersByOrg() {
        when(costCenterRepository.findByOrgId(1L)).thenReturn(Arrays.asList(testCostCenter));

        List<CostCenterMapping> result = costCenterService.getCostCentersByOrg(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("测试获取所有成本中心")
    void testGetAllCostCenters() {
        when(costCenterRepository.findAll()).thenReturn(Arrays.asList(testCostCenter));

        List<CostCenterMapping> result = costCenterService.getAllCostCenters();

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("测试更新成本中心 - 不存在")
    void testUpdateCostCenter_NotFound() {
        UpdateCostCenterRequest request = new UpdateCostCenterRequest();
        request.setCostCenterName("Updated Name");

        when(costCenterRepository.findById(99L)).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class, () -> 
            costCenterService.updateCostCenter(99L, request, "admin"));
        assertNotNull(exception);
    }

    @Test
    @DisplayName("测试更新成本中心 - 编码重复")
    void testUpdateCostCenter_DuplicateCode() {
        UpdateCostCenterRequest request = new UpdateCostCenterRequest();
        request.setCostCenterCode("CC002");

        when(costCenterRepository.findById(1L)).thenReturn(Optional.of(testCostCenter));
        when(costCenterRepository.existsByOrgIdAndCostCenterCode(1L, "CC002")).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class, () -> 
            costCenterService.updateCostCenter(1L, request, "admin"));
        assertEquals("Cost center code already exists for this organization", exception.getMessage());
    }

    @Test
    @DisplayName("测试删除成本中心 - 验证删除调用")
    void testDeleteCostCenter_VerifyDelete() {
        doNothing().when(costCenterRepository).deleteById(1L);

        costCenterService.deleteCostCenter(1L);
        verify(costCenterRepository).deleteById(1L);
    }

    @Test
    @DisplayName("测试更新成本中心 - 部分更新")
    void testUpdateCostCenter_PartialUpdate() {
        UpdateCostCenterRequest request = new UpdateCostCenterRequest();
        request.setCostCenterName("Partial Update");

        when(costCenterRepository.findById(1L)).thenReturn(Optional.of(testCostCenter));
        when(costCenterRepository.save(any(CostCenterMapping.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CostCenterMapping result = costCenterService.updateCostCenter(1L, request, "admin");

        assertEquals("Partial Update", result.getCostCenterName());
    }

    @Test
    @DisplayName("测试更新成本中心 - 状态更新")
    void testUpdateCostCenter_StatusUpdate() {
        UpdateCostCenterRequest request = new UpdateCostCenterRequest();
        request.setStatus("inactive");

        when(costCenterRepository.findById(1L)).thenReturn(Optional.of(testCostCenter));
        when(costCenterRepository.save(any(CostCenterMapping.class))).thenReturn(testCostCenter);

        assertDoesNotThrow(() -> costCenterService.updateCostCenter(1L, request, "admin"));
    }
}

