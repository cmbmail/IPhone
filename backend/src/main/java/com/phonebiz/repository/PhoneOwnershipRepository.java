package com.phonebiz.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.phonebiz.entity.PhoneOwnership;

@Repository
public interface PhoneOwnershipRepository extends JpaRepository<PhoneOwnership, Long> {

    Optional<PhoneOwnership> findByPhoneNumber(String phoneNumber);

    Page<PhoneOwnership> findByBranchOrgId(Long branchOrgId, Pageable pageable);

    Page<PhoneOwnership> findByDeptOrgId(Long deptOrgId, Pageable pageable);

    @Query("SELECT p FROM PhoneOwnership p WHERE " +
           "(:keyword IS NULL OR p.phoneNumber LIKE %:keyword% OR p.branchName LIKE %:keyword% OR p.deptName LIKE %:keyword%) " +
           "AND (:branchOrgId IS NULL OR p.branchOrgId = :branchOrgId)")
    Page<PhoneOwnership> search(@Param("keyword") String keyword,
                                @Param("branchOrgId") Long branchOrgId,
                                Pageable pageable);

    List<PhoneOwnership> findAllByOrderByPhoneNumberAsc();

    boolean existsByPhoneNumber(String phoneNumber);

    // Count phones by branch org ID
    long countByBranchOrgIdAndDeletedAtIsNull(Long branchOrgId);

    // Count phones by branch org ID with non-null dept
    long countByBranchOrgIdAndDeptOrgIdIsNotNullAndDeletedAtIsNull(Long branchOrgId);

    // Count phones by dept org ID list
    @Query("SELECT COUNT(p) FROM PhoneOwnership p WHERE p.deptOrgId IN :orgIds AND p.deletedAt IS NULL")
    long countByDeptOrgIdIn(@Param("orgIds") List<Long> orgIds);

    // Count phones by dept org ID list with non-null dept
    @Query("SELECT COUNT(p) FROM PhoneOwnership p WHERE p.deptOrgId IN :orgIds AND p.deptOrgId IS NOT NULL AND p.deletedAt IS NULL")
    long countAllocatedByDeptOrgIdIn(@Param("orgIds") List<Long> orgIds);

    // Find by orgIds with pagination
    @Query("SELECT p FROM PhoneOwnership p WHERE p.deptOrgId IN :orgIds AND p.deletedAt IS NULL ORDER BY p.phoneNumber")
    Page<PhoneOwnership> findByDeptOrgIdIn(@Param("orgIds") List<Long> orgIds, Pageable pageable);

    // Find by branchOrgId with pagination
    Page<PhoneOwnership> findByBranchOrgIdAndDeletedAtIsNull(Long branchOrgId, Pageable pageable);
}
