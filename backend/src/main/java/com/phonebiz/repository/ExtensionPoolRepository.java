package com.phonebiz.repository;

import com.phonebiz.entity.ExtensionPool;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExtensionPoolRepository extends JpaRepository<ExtensionPool, Long> {

    List<ExtensionPool> findByOrgId(Long orgId);

    boolean existsByOrgId(Long orgId);
}
