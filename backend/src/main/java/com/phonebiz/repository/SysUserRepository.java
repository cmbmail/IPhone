package com.phonebiz.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.phonebiz.entity.SysUser;

@Repository
public interface SysUserRepository extends JpaRepository<SysUser, Long> {

    Optional<SysUser> findByUsername(String username);

    Optional<SysUser> findByEmployeeNo(String employeeNo);

    boolean existsByUsername(String username);

    @Query("SELECT u FROM SysUser u WHERE u.status = 'active'")
    List<SysUser> findAllActive();

    @Query("SELECT u FROM SysUser u WHERE u.roleId = :roleId")
    List<SysUser> findByRoleId(@Param("roleId") Long roleId);

    @Query("SELECT COUNT(u) FROM SysUser u WHERE u.roleId = :roleId")
    long countByRoleId(@Param("roleId") Long roleId);

    @Query("SELECT u FROM SysUser u WHERE u.employeeNo IN :employeeNos")
    List<SysUser> findAllByEmployeeNoIn(@Param("employeeNos") List<String> employeeNos);

    @Modifying
    @Query("UPDATE SysUser u SET u.loginFailCount = :count WHERE u.username = :username")
    void updateLoginFailCount(@Param("username") String username, @Param("count") int count);


    @Modifying
    @Query("UPDATE SysUser u SET u.lockedUntil = :lockedUntil WHERE u.username = :username")
    void updateLockedUntil(@Param("username") String username, @Param("lockedUntil") LocalDateTime lockedUntil);


    @Modifying
    @Query("UPDATE SysUser u SET u.lastLoginAt = :lastLoginAt, u.loginFailCount = 0 WHERE u.username = :username")
    void updateLastLoginAt(@Param("username") String username, @Param("lastLoginAt") LocalDateTime lastLoginAt);


    @Modifying
    @Query("UPDATE SysUser u SET u.passwordHash = :passwordHash, u.passwordChangedAt = :changedAt WHERE u.username = :username")
    void updatePassword(@Param("username") String username,
                        @Param("passwordHash") String passwordHash,
                        @Param("changedAt") LocalDateTime changedAt);
}
