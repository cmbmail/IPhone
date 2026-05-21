package com.phonebiz.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.phonebiz.entity.SysFeatureFlag;

@Repository
public interface SysFeatureFlagRepository extends JpaRepository<SysFeatureFlag, Long> {
    
    Optional<SysFeatureFlag> findByFeatureKey(String featureKey);
    
    List<SysFeatureFlag> findByIsEnabled(Boolean isEnabled);
    
    boolean existsByFeatureKey(String featureKey);
}

