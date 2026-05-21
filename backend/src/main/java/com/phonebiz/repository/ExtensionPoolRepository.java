package com.phonebiz.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.phonebiz.entity.ExtensionPool;

@Repository
public interface ExtensionPoolRepository extends JpaRepository<ExtensionPool, Long> {

    List<ExtensionPool> findByOrgId(Long orgId);

    boolean existsByOrgId(Long orgId);
}

