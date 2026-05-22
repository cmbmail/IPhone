package com.phonebiz.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.phonebiz.entity.OrgStructure;

@Repository
public interface OrgStructureRepository extends JpaRepository<OrgStructure, Long> {

    List<OrgStructure> findByParentIdIsNull();

    List<OrgStructure> findByParentId(Long parentId);

    List<OrgStructure> findByParentIdAndStatus(Long parentId, Integer status);

    List<OrgStructure> findByStatus(Integer status);

    Optional<OrgStructure> findByName(String name);

    boolean existsByParentIdAndName(Long parentId, String name);

    boolean existsByParentIdIsNullAndName(String name);

    @Query("SELECT o FROM OrgStructure o WHERE o.status = 1 ORDER BY o.sortOrder, o.name")
    List<OrgStructure> findAllActiveOrdered();

    @Query("SELECT o FROM OrgStructure o WHERE o.path LIKE :pathPrefix%")
    List<OrgStructure> findByPathStartingWith(@Param("pathPrefix") String pathPrefix);

    @Query("SELECT COUNT(o) FROM OrgStructure o WHERE o.parentId = :parentId")
    long countByParentId(@Param("parentId") Long parentId);

    @Modifying
    @Query("UPDATE OrgStructure o SET o.sortOrder = :sortOrder WHERE o.id = :id")
    void updateSortOrder(@Param("id") Long id, @Param("sortOrder") Integer sortOrder);
}
