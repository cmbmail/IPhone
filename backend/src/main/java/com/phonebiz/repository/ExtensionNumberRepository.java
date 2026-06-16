package com.phonebiz.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.phonebiz.entity.ExtensionNumber;

@Repository
public interface ExtensionNumberRepository extends JpaRepository<ExtensionNumber, Long> {

    Optional<ExtensionNumber> findByExtensionNumber(String extensionNumber);

    Page<ExtensionNumber> findByStatus(Integer status, Pageable pageable);

    Page<ExtensionNumber> findByDeptOrgId(Long deptOrgId, Pageable pageable);

    @Query("SELECT e FROM ExtensionNumber e WHERE " +
           "(:keyword IS NULL OR e.extensionNumber LIKE %:keyword% OR e.employeeName LIKE %:keyword% OR e.deptName LIKE %:keyword% OR e.phoneNumber LIKE %:keyword%) " +
           "AND (:status IS NULL OR e.status = :status) " +
           "AND (:deptOrgId IS NULL OR e.deptOrgId = :deptOrgId)")
    Page<ExtensionNumber> search(@Param("keyword") String keyword,
                                  @Param("status") Integer status,
                                  @Param("deptOrgId") Long deptOrgId,
                                  Pageable pageable);

    long countByStatus(Integer status);

    // Order: AVAILABLE first, then IDLE, then ALLOCATED
    @Query("SELECT e FROM ExtensionNumber e WHERE " +
           "(:keyword IS NULL OR e.extensionNumber LIKE %:keyword% OR e.employeeName LIKE %:keyword% OR e.deptName LIKE %:keyword% OR e.phoneNumber LIKE %:keyword%) " +
           "AND (:status IS NULL OR e.status = :status) " +
           "AND (:deptOrgId IS NULL OR e.deptOrgId = :deptOrgId) " +
           "ORDER BY CASE e.status WHEN 'AVAILABLE' THEN 0 WHEN 'IDLE' THEN 1 WHEN 'ALLOCATED' THEN 2 ELSE 3 END, e.extensionNumber ASC")
    Page<ExtensionNumber> searchOrdered(@Param("keyword") String keyword,
                                         @Param("status") Integer status,
                                         @Param("deptOrgId") Long deptOrgId,
                                         Pageable pageable);

    @Query("SELECT e FROM ExtensionNumber e WHERE e.extensionNumber LIKE %:keyword% OR e.employeeName LIKE %:keyword%")
    List<ExtensionNumber> searchByKeyword(@Param("keyword") String keyword);

    List<ExtensionNumber> findAllByPhoneIdIn(List<Long> phoneIds);

    @Query("SELECT e FROM ExtensionNumber e WHERE e.id = :id")
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    Optional<ExtensionNumber> findByIdWithLock(@Param("id") Long id);
}
