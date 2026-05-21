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
import com.phonebiz.dto.CreateOrgRequest;
import com.phonebiz.entity.OrgStructure;
import com.phonebiz.repository.OrgStructureRepository;

@ExtendWith(MockitoExtension.class)
class OrgServiceTest {

    @Mock
    private OrgStructureRepository orgRepository;

    @InjectMocks
    private OrgService orgService;

    private OrgStructure testOrg;

    @BeforeEach
    void setUp() {
        testOrg = new OrgStructure();
        testOrg.setId(1L);
        testOrg.setName("Test Org");
        testOrg.setParentId(null);
        testOrg.setLevel(1);
        testOrg.setPath("/1");
    }

    @Test
    @DisplayName("测试创建组织 - 成功")
    void testCreateOrg_Success() {
        when(orgRepository.existsByParentIdIsNullAndName("Test Org")).thenReturn(false);
        when(orgRepository.save(any(OrgStructure.class))).thenReturn(testOrg);

        CreateOrgRequest request = new CreateOrgRequest();
        request.setName("Test Org");

        OrgStructure result = orgService.createOrg(request, "admin");

        assertNotNull(result);
        assertEquals("Test Org", result.getName());
        verify(orgRepository, times(2)).save(any(OrgStructure.class));
    }

    @Test
    @DisplayName("测试创建组织 - 名称重复")
    void testCreateOrg_DuplicateName() {
        when(orgRepository.existsByParentIdIsNullAndName("Test Org")).thenReturn(true);

        CreateOrgRequest request = new CreateOrgRequest();
        request.setName("Test Org");

        BusinessException exception = assertThrows(BusinessException.class, () -> orgService.createOrg(request, "admin"));
        assertTrue(exception.getMessage().contains("already exists"));
    }

    @Test
    @DisplayName("测试创建子组织 - 成功")
    void testCreateOrg_WithParent() {
        OrgStructure parentOrg = new OrgStructure();
        parentOrg.setId(1L);
        parentOrg.setName("Parent");
        parentOrg.setLevel(1);
        parentOrg.setPath("/1");

        when(orgRepository.findById(1L)).thenReturn(Optional.of(parentOrg));
        when(orgRepository.existsByParentIdAndName(1L, "Child")).thenReturn(false);
        when(orgRepository.save(any(OrgStructure.class))).thenAnswer(invocation -> {
            OrgStructure org = invocation.getArgument(0);
            if (org.getId() == null) {
                org.setId(2L);
            }
            return org;
        });

        CreateOrgRequest request = new CreateOrgRequest();
        request.setName("Child");
        request.setParentId(1L);

        OrgStructure result = orgService.createOrg(request, "admin");

        assertNotNull(result);
        assertEquals(2, result.getLevel());
    }

    @Test
    @DisplayName("测试创建子组织 - 父组织不存在")
    void testCreateOrg_ParentNotFound() {
        when(orgRepository.findById(1L)).thenReturn(Optional.empty());

        CreateOrgRequest request = new CreateOrgRequest();
        request.setName("Child");
        request.setParentId(1L);

        BusinessException exception = assertThrows(BusinessException.class, () -> orgService.createOrg(request, "admin"));
        assertTrue(exception.getMessage().contains("Organization not found"));
    }

    @Test
    @DisplayName("测试删除组织 - 成功")
    void testDeleteOrg_Success() {
        when(orgRepository.findById(1L)).thenReturn(Optional.of(testOrg));
        when(orgRepository.countByParentId(1L)).thenReturn(0L);
        doNothing().when(orgRepository).delete(any(OrgStructure.class));

        assertDoesNotThrow(() -> orgService.deleteOrg(1L));
        verify(orgRepository).delete(any(OrgStructure.class));
    }

    @Test
    @DisplayName("测试删除组织 - 有子组织")
    void testDeleteOrg_HasChildren() {
        when(orgRepository.findById(1L)).thenReturn(Optional.of(testOrg));
        when(orgRepository.countByParentId(1L)).thenReturn(1L);

        BusinessException exception = assertThrows(BusinessException.class, () -> orgService.deleteOrg(1L));
        assertTrue(exception.getMessage().contains("Cannot delete organization with children"));
    }

    @Test
    @DisplayName("测试获取树形结构")
    void testGetOrgTree() {
        OrgStructure childOrg = new OrgStructure();
        childOrg.setId(2L);
        childOrg.setName("Child");
        childOrg.setParentId(1L);
        childOrg.setLevel(2);
        childOrg.setPath("/1/2");

        when(orgRepository.findAllActiveOrderByLevelAndName()).thenReturn(Arrays.asList(testOrg, childOrg));

        List<OrgStructure> tree = orgService.getOrgTree();

        assertNotNull(tree);
        assertEquals(1, tree.size());
        assertEquals("Test Org", tree.get(0).getName());
    }

    @Test
    @DisplayName("测试获取组织 - 成功")
    void testGetOrgById_Success() {
        when(orgRepository.findById(1L)).thenReturn(Optional.of(testOrg));

        OrgStructure result = orgService.getOrgById(1L);

        assertNotNull(result);
        assertEquals("Test Org", result.getName());
    }

    @Test
    @DisplayName("测试获取组织 - 不存在")
    void testGetOrgById_NotFound() {
        when(orgRepository.findById(99L)).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class, () -> orgService.getOrgById(99L));
        assertTrue(exception.getMessage().contains("Organization not found"));
    }

    @Test
    @DisplayName("测试删除组织 - 不存在")
    void testDeleteOrg_NotFound() {
        when(orgRepository.findById(99L)).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class, () -> orgService.deleteOrg(99L));
        assertTrue(exception.getMessage().contains("Organization not found"));
    }

    @Test
    @DisplayName("测试获取子组织列表")
    void testGetChildren() {
        OrgStructure childOrg = new OrgStructure();
        childOrg.setId(2L);
        childOrg.setName("Child");
        childOrg.setParentId(1L);
        childOrg.setStatus(OrgStructure.OrgStatus.active);

        when(orgRepository.findByParentIdAndStatus(1L, OrgStructure.OrgStatus.active))
                .thenReturn(Arrays.asList(childOrg));

        List<OrgStructure> result = orgService.getChildren(1L);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals("Child", result.get(0).getName());
    }
}

