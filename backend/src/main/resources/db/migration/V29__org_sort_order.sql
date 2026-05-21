-- V29: org_structure add sort_order for drag-drop reordering
ALTER TABLE org_structure ADD COLUMN sort_order INT NOT NULL DEFAULT 0;

-- Initialize sort_order by existing order (id-based per parent)
SET @rn := 0;
UPDATE org_structure o SET sort_order = (
  SELECT rn FROM (
    SELECT id, (@rn := @rn + 1) AS rn
    FROM org_structure
    WHERE parent_id <=> o.parent_id
    ORDER BY id
  ) t WHERE t.id = o.id
);
