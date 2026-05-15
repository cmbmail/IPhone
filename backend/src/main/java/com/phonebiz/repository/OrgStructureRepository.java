package com.phonebiz.repository;

import com.phonebiz.entity.OrgStructure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrgStructureRepository extends JpaRepository<OrgStructure, Long> {

    List<OrgStructure> findByParentIdIsNull();

    List<OrgStructure> findByParentId(Long parentId);

    List<OrgStructure> findByParentIdAndStatus(Long parentId, OrgStructure.OrgStatus status);

    List<OrgStructure> findByStatus(OrgStructure.OrgStatus status);

    boolean existsByParentIdAndName(Long parentId, String name);

    boolean existsByParentIdIsNullAndName(String name);

    @Query("SELECT o FROM OrgStructure o WHERE o.status = 'active' ORDER BY o.level, o.name")
    List<OrgStructure> findAllActiveOrderByLevelAndName();

    @Query("SELECT o FROM OrgStructure o WHERE o.path LIKE :pathPrefix%")
    List<OrgStructure> findByPathStartingWith(@Param("pathPrefix") String pathPrefix);

    @Query("SELECT COUNT(o) FROM OrgStructure o WHERE o.parentId = :parentId")
    long countByParentId(@Param("parentId") Long parentId);
}
