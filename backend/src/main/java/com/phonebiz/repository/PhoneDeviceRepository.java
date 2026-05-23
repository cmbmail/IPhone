package com.phonebiz.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.phonebiz.entity.PhoneDevice;

@Repository
public interface PhoneDeviceRepository extends JpaRepository<PhoneDevice, Long> {
    Optional<PhoneDevice> findByMacAddress(String macAddress);
    boolean existsByMacAddress(String macAddress);
    List<PhoneDevice> findByOrgId(Long orgId);
    List<PhoneDevice> findByAssignedEmployeeNo(String employeeNo);
    Page<PhoneDevice> findByStatus(Integer status, Pageable pageable);

    @Query("SELECT d FROM PhoneDevice d WHERE d.orgId IN (:orgIds)")
    Page<PhoneDevice> findByOrgIds(@Param("orgIds") List<Long> orgIds, Pageable pageable);

    @Query("SELECT d.orgId FROM PhoneDevice d WHERE d.id IN :ids")
    List<Long> findOrgIdsByIds(@Param("ids") List<Long> ids);

    @Query("SELECT d.assignedEmployeeNo FROM PhoneDevice d WHERE d.id IN :ids AND d.assignedEmployeeNo IS NOT NULL")
    List<String> findAssignedTosByIds(@Param("ids") List<Long> ids);

    @Query("SELECT d FROM PhoneDevice d WHERE d.orgId IN (:orgIds) AND d.status = :status")
    Page<PhoneDevice> findByOrgIdsAndStatus(@Param("orgIds") List<Long> orgIds,
                                            @Param("status") Integer status,
                                            Pageable pageable);

    @Query("SELECT d FROM PhoneDevice d WHERE d.macAddress LIKE %:keyword% OR d.assignedEmployeeNo LIKE %:keyword%")
    List<PhoneDevice> searchByKeyword(@Param("keyword") String keyword);

}

