package com.phonebiz.repository;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.phonebiz.entity.PhoneNumber;

@Repository
public interface PhoneNumberRepository extends JpaRepository<PhoneNumber, Long> {

    Optional<PhoneNumber> findByPhoneNumber(String phoneNumber);

    boolean existsByPhoneNumber(String phoneNumber);

    boolean existsByExtensionNumber(String extensionNumber);

    Optional<PhoneNumber> findByExtensionNumber(String extensionNumber);

    Page<PhoneNumber> findByStatus(PhoneNumber.PhoneStatus status, Pageable pageable);

    Page<PhoneNumber> findByOrgId(Long orgId, Pageable pageable);

    List<PhoneNumber> findByOrgId(Long orgId);

    List<PhoneNumber> findByUserId(String userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PhoneNumber p WHERE p.id = :id")
    Optional<PhoneNumber> findByIdWithLock(@Param("id") Long id);

    @Query("SELECT p FROM PhoneNumber p WHERE p.status = 'idle' ORDER BY p.createdAt")
    List<PhoneNumber> findIdlePhones();

    @Query("SELECT p FROM PhoneNumber p WHERE p.extensionNumber = :ext AND p.status = 'idle'")
    Optional<PhoneNumber> findIdleByExtensionNumber(@Param("ext") String extensionNumber);

    @Query("SELECT COUNT(p) FROM PhoneNumber p WHERE p.orgId = :orgId AND p.status = 'active'")
    long countActiveByOrgId(@Param("orgId") Long orgId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PhoneNumber p WHERE p.id IN :ids ORDER BY p.id ASC")
    List<PhoneNumber> findByIdsForUpdate(@Param("ids") List<Long> ids);

    @Query("SELECT p.status, COUNT(p) FROM PhoneNumber p GROUP BY p.status")
    List<Object[]> countGroupByStatus();

    @Query("SELECT p.orgId, COUNT(p) FROM PhoneNumber p WHERE p.orgId IS NOT NULL GROUP BY p.orgId")
    List<Object[]> countByOrgIdGroupBy();

    @Query("SELECT COUNT(p) FROM PhoneNumber p WHERE p.orgId = :orgId")
    long countByOrgId(@Param("orgId") Long orgId);

    @Query("SELECT p.status, COUNT(p) FROM PhoneNumber p WHERE p.orgId = :orgId GROUP BY p.status")
    List<Object[]> countByOrgIdGroupByStatus(@Param("orgId") Long orgId);

    @Query("SELECT p.phoneNumber FROM PhoneNumber p")
    List<String> findAllPhoneNumbers();
}