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
}
