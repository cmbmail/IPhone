package com.phonebiz.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.phonebiz.entity.SysPermission;

@Repository
public interface SysPermissionRepository extends JpaRepository<SysPermission, Long> {

    List<SysPermission> findAllByOrderByModuleAscSortOrderAsc();

    @Query("SELECT DISTINCT p.module FROM SysPermission p ORDER BY p.module")
    List<String> findAllModules();

    @Query("SELECT p FROM SysPermission p WHERE p.module = :module ORDER BY p.sortOrder")
    List<SysPermission> findByModule(@Param("module") String module);

    @Query("SELECT p FROM SysPermission p WHERE p.id IN :ids ORDER BY p.module, p.sortOrder")
    List<SysPermission> findByIdIn(@Param("ids") List<Long> ids);
}
