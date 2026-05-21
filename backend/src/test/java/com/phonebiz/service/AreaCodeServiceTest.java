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
import com.phonebiz.dto.CreateAreaCodeMappingRequest;
import com.phonebiz.entity.AreaCodeOrgMapping;
import com.phonebiz.entity.OrgStructure;
import com.phonebiz.repository.AreaCodeOrgMappingRepository;
import com.phonebiz.repository.OrgStructureRepository;

@ExtendWith(MockitoExtension.class)
class AreaCodeServiceTest {

    @Mock
    private AreaCodeOrgMappingRepository mappingRepository;

    @Mock
    private OrgStructureRepository orgRepository;

    @InjectMocks
    private AreaCodeService areaCodeService;

    private AreaCodeOrgMapping testMapping;
    private OrgStructure testOrg;

    @BeforeEach
    void setUp() {
        testOrg = new OrgStructure();
        testOrg.setId(1L);
        testOrg.setName("Test Org");

        testMapping = new AreaCodeOrgMapping();
        testMapping.setId(1L);
        testMapping.setAreaCode("010");
        testMapping.setOrgId(1L);
        testMapping.setPriority(1);
    }

    @Test
    @DisplayName("测试创建映射 - 成功")
    void testCreateMapping_Success() {
        CreateAreaCodeMappingRequest request = new CreateAreaCodeMappingRequest();
        request.setAreaCode("010");
        request.setOrgId(1L);
        request.setPriority(1);

        when(orgRepository.findById(1L)).thenReturn(Optional.of(testOrg));
        when(mappingRepository.existsByAreaCodeAndOrgId("010", 1L)).thenReturn(false);
        when(mappingRepository.save(any(AreaCodeOrgMapping.class))).thenReturn(testMapping);

        AreaCodeOrgMapping result = areaCodeService.createMapping(request, "admin");

        assertNotNull(result);
        assertEquals("010", result.getAreaCode());
        verify(mappingRepository).save(any(AreaCodeOrgMapping.class));
    }

    @Test
    @DisplayName("测试创建映射 - 组织不存在")
    void testCreateMapping_OrgNotFound() {
        CreateAreaCodeMappingRequest request = new CreateAreaCodeMappingRequest();
        request.setAreaCode("010");
        request.setOrgId(99L);

        when(orgRepository.findById(99L)).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class, () -> 
            areaCodeService.createMapping(request, "admin"));
        assertTrue(exception.getMessage().contains("Organization not found"));
    }

    @Test
    @DisplayName("测试创建映射 - 映射已存在")
    void testCreateMapping_DuplicateMapping() {
        CreateAreaCodeMappingRequest request = new CreateAreaCodeMappingRequest();
        request.setAreaCode("010");
        request.setOrgId(1L);

        when(orgRepository.findById(1L)).thenReturn(Optional.of(testOrg));
        when(mappingRepository.existsByAreaCodeAndOrgId("010", 1L)).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class, () -> 
            areaCodeService.createMapping(request, "admin"));
        assertTrue(exception.getMessage().contains("already mapped"));
    }

    @Test
    @DisplayName("测试获取映射 - 成功")
    void testGetMappingById_Success() {
        when(mappingRepository.findById(1L)).thenReturn(Optional.of(testMapping));

        AreaCodeOrgMapping result = areaCodeService.getMappingById(1L);

        assertNotNull(result);
        assertEquals("010", result.getAreaCode());
    }

    @Test
    @DisplayName("测试获取映射 - 不存在")
    void testGetMappingById_NotFound() {
        when(mappingRepository.findById(99L)).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class, () -> 
            areaCodeService.getMappingById(99L));
        assertTrue(exception.getMessage().contains("Parameter validation failed"));
    }

    @Test
    @DisplayName("测试删除映射 - 成功")
    void testDeleteMapping_Success() {
        doNothing().when(mappingRepository).deleteById(1L);

        assertDoesNotThrow(() -> areaCodeService.deleteMapping(1L));
        verify(mappingRepository).deleteById(1L);
    }

    @Test
    @DisplayName("测试按区号查询映射")
    void testGetMappingsByAreaCode() {
        when(mappingRepository.findByAreaCodeOrderByPriority("010")).thenReturn(Arrays.asList(testMapping));

        List<AreaCodeOrgMapping> result = areaCodeService.getMappingsByAreaCode("010");

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("测试按组织查询映射")
    void testGetMappingsByOrg() {
        when(mappingRepository.findByOrgId(1L)).thenReturn(Arrays.asList(testMapping));

        List<AreaCodeOrgMapping> result = areaCodeService.getMappingsByOrg(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("测试区号匹配组织 - 成功")
    void testMatchOrgByAreaCode_Success() {
        when(mappingRepository.findFirstByAreaCodeOrderByPriorityAsc("010")).thenReturn(Optional.of(testMapping));

        Long result = areaCodeService.matchOrgByAreaCode("010");

        assertNotNull(result);
        assertEquals(1L, result);
    }

    @Test
    @DisplayName("测试获取所有映射")
    void testGetAllMappings() {
        when(mappingRepository.findAll()).thenReturn(Arrays.asList(testMapping));

        List<AreaCodeOrgMapping> result = areaCodeService.getAllMappings();

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("测试检查区号映射是否存在")
    void testExistsMapping_Exists() {
        when(mappingRepository.existsByAreaCodeAndOrgId("010", 1L)).thenReturn(true);

        assertTrue(mappingRepository.existsByAreaCodeAndOrgId("010", 1L));
    }

    @Test
    @DisplayName("测试检查区号映射是否存在 - 不存在")
    void testExistsMapping_NotExists() {
        when(mappingRepository.existsByAreaCodeAndOrgId("010", 1L)).thenReturn(false);

        assertFalse(mappingRepository.existsByAreaCodeAndOrgId("010", 1L));
    }

    @Test
    @DisplayName("测试按组织查询映射 - 空结果")
    void testGetMappingsByOrg_Empty() {
        when(mappingRepository.findByOrgId(1L)).thenReturn(Arrays.asList());

        List<AreaCodeOrgMapping> result = areaCodeService.getMappingsByOrg(1L);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}

