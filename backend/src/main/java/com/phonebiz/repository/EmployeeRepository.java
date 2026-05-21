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

    List<Employee> findByStatus(Employee.EmployeeStatus status);

    Page<Employee> findByOrgId(Long orgId, Pageable pageable);

    @Query("SELECT e FROM Employee e WHERE e.status = 'active' ORDER BY e.employeeNo")
    List<Employee> findAllActive();

    @Query("SELECT e FROM Employee e WHERE e.orgId IN :orgIds AND e.status = 'active'")
    List<Employee> findByOrgIdInAndStatusActive(@Param("orgIds") List<Long> orgIds);

    @Query("SELECT COUNT(e) FROM Employee e WHERE e.orgId = :orgId AND e.status = 'active'")
    long countActiveByOrgId(@Param("orgId") Long orgId);
}

