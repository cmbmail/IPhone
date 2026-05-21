-- ============================================================
-- PhoneBiz Phase 3 - 发票相关表
-- ============================================================

-- ----------------------------------------------------------
-- 发票主表
-- ----------------------------------------------------------
CREATE TABLE invoice (
    id                    BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    invoice_no            VARCHAR(100) NOT NULL COMMENT '发票号',
    bill_month            VARCHAR(7) NOT NULL COMMENT '发票对应账单月份',
    source_org_id         BIGINT UNSIGNED NOT NULL COMMENT '发票来源公司组织ID',
    source_org_name       VARCHAR(100) NOT NULL COMMENT '发票来源公司名称（从PDF识别）',
    recipient_org_id      BIGINT UNSIGNED NOT NULL COMMENT '接收方子公司组织ID',
    amount                DECIMAL(12, 2) NULL COMMENT '发票金额（可从PDF提取）',
    tax_amount            DECIMAL(12, 2) NULL COMMENT '税额',
    invoice_date          DATE NULL COMMENT '发票日期',
    status                ENUM('pending', 'distributed', 'read', 'confirmed') NOT NULL DEFAULT 'pending' COMMENT '状态',
    ocr_text              TEXT NULL COMMENT 'OCR识别的原始文本',
    ocr_confidence        DECIMAL(5, 4) NULL COMMENT 'OCR识别置信度',
    distribute_at         DATETIME NULL COMMENT '分发时间',
    read_at               DATETIME NULL COMMENT '接收方查看时间',
    confirmed_at          DATETIME NULL COMMENT '确认时间',
    created_at            DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_invoice_no (invoice_no),
    INDEX idx_bill_month (bill_month),
    INDEX idx_recipient (recipient_org_id),
    INDEX idx_status (status),
    FOREIGN KEY (source_org_id) REFERENCES org_structure(id),
    FOREIGN KEY (recipient_org_id) REFERENCES org_structure(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='发票主表';

-- ----------------------------------------------------------
-- 发票文件存储
-- ----------------------------------------------------------
CREATE TABLE invoice_file (
    id            BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    invoice_id    BIGINT UNSIGNED NOT NULL,
    file_name     VARCHAR(200) NOT NULL COMMENT '原始文件名（公司名称_序号.PDF）',
    file_path     VARCHAR(500) NOT NULL COMMENT '文件存储路径',
    file_size     BIGINT NOT NULL COMMENT '文件大小（字节）',
    md5           VARCHAR(32) NULL COMMENT '文件MD5校验',
    created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (invoice_id) REFERENCES invoice(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='发票文件';

-- ----------------------------------------------------------
-- 发票分发记录
-- ----------------------------------------------------------
CREATE TABLE invoice_distribution (
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    invoice_id      BIGINT UNSIGNED NOT NULL,
    recipient_user  VARCHAR(50) NOT NULL COMMENT '接收人账号',
    distribution_status ENUM('success', 'failed') NOT NULL DEFAULT 'success',
    fail_reason     VARCHAR(200) NULL,
    notified_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_invoice (invoice_id),
    FOREIGN KEY (invoice_id) REFERENCES invoice(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='发票分发记录';
