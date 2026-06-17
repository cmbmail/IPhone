package com.phonebiz.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.phonebiz.entity.Employee;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Optional<Employee> findByEmployeeNo(String employeeNo);

    boolean existsByEmployeeNo(String employeeNo);

    List<Employee> findByOrgId(Long orgId);

    List<Employee> findByStatus(Integer status);

    Page<Employee> findByOrgId(Long orgId, Pageable pageable);

    Page<Employee> findByStatus(Integer status, Pageable pageable);

    @Query("SELECT e FROM Employee e WHERE e.status = 1 ORDER BY e.employeeNo")
    List<Employee> findAllActive();

    @Query("SELECT e FROM Employee e WHERE e.status = 1 ORDER BY e.employeeNo")
    Page<Employee> findAllActivePaged(Pageable pageable);

    @Query("SELECT e FROM Employee e WHERE e.orgId IN :orgIds AND e.status = 1")
    List<Employee> findByOrgIdInAndStatusActive(@Param("orgIds") List<Long> orgIds);

    @Query("SELECT e FROM Employee e WHERE e.employeeNo IN :employeeNos")
    List<Employee> findAllByEmployeeNoIn(@Param("employeeNos") List<String> employeeNos);

    @Query("SELECT COUNT(e) FROM Employee e WHERE e.orgId = :orgId AND e.status = 1")
    long countActiveByOrgId(@Param("orgId") Long orgId);

    @Query("SELECT e FROM Employee e WHERE e.name LIKE %:keyword% OR e.employeeNo LIKE %:keyword%")
    List<Employee> searchByKeyword(@Param("keyword") String keyword);

    @Query("SELECT e FROM Employee e WHERE (e.name LIKE CONCAT('%',:keyword,'%') OR e.employeeNo LIKE CONCAT('%',:keyword,'%')) AND e.status = 1")
    Page<Employee> searchActiveByKeyword(@Param("keyword") String keyword, Pageable pageable);

}
