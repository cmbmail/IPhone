CREATE TABLE phone_device (
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    mac_address     VARCHAR(12) NOT NULL COMMENT 'MAC地址（大写12位十六进制，去冒号存储）',
    model           VARCHAR(100) NULL COMMENT '型号',
    brand           VARCHAR(100) NULL COMMENT '品牌',
    purchase_date   DATE NULL COMMENT '购置日期',
    org_id          BIGINT UNSIGNED NOT NULL COMMENT '归属组织',
    assigned_to     VARCHAR(20) NULL COMMENT '分配使用人工号（可为空，表示组织公共设备）',
    status          ENUM('stock','active','inactive','repairing','retired') NOT NULL DEFAULT 'stock',
    remark          VARCHAR(500) NULL COMMENT '备注',
    version         INT NOT NULL DEFAULT 0 COMMENT '乐观锁',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by      VARCHAR(50) NOT NULL,
    updated_by      VARCHAR(50) NOT NULL,
    UNIQUE KEY uk_mac (mac_address),
    INDEX idx_org (org_id),
    INDEX idx_assigned (assigned_to),
    INDEX idx_status (status)
) COMMENT='电话机主表';

CREATE TABLE device_phone_mapping (
    id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    device_id   BIGINT UNSIGNED NOT NULL COMMENT 'phone_device.id',
    phone_id    BIGINT UNSIGNED NOT NULL COMMENT 'phone_number.id（对应的分机号载体）',
    line_order  INT NOT NULL DEFAULT 1 COMMENT '线路序号',
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_device_phone (device_id, phone_id),
    INDEX idx_phone (phone_id)
) COMMENT='话机-号码关联表（通过分机号绑定，phone_id对应的号码必须持有分机号）';

CREATE TABLE phone_device_history (
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    device_id       BIGINT UNSIGNED NOT NULL,
    mac_address     VARCHAR(12) NOT NULL COMMENT '操作时MAC快照',
    action          VARCHAR(30) NOT NULL COMMENT '分配/回收/维修/报废/恢复',
    from_status     VARCHAR(20) NULL,
    to_status       VARCHAR(20) NULL,
    from_assigned   VARCHAR(20) NULL,
    to_assigned     VARCHAR(20) NULL,
    operator        VARCHAR(50) NOT NULL,
    operated_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    remark          VARCHAR(500) NULL,
    INDEX idx_device (device_id),
    INDEX idx_operated_at (operated_at)
) COMMENT='话机操作历史';
