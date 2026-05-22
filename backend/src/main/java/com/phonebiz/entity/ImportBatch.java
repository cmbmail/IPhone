package com.phonebiz.entity;

import java.time.LocalDateTime;
import jakarta.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "import_batch")
public class ImportBatch extends BaseEntity {

    public static final int BATCH_PENDING = 0;
    public static final int BATCH_PROCESSING = 1;
    public static final int BATCH_COMPLETED = 2;
    public static final int BATCH_FAILED = 3;


    @Column(name = "batch_id", unique = true, nullable = false, length = 32)
    private String batchId;

    @Column(name = "total_count", nullable = false)
    private Integer totalCount;

    @Column(name = "success_count", nullable = false)
    private Integer successCount;

    @Column(name = "fail_count", nullable = false)
    private Integer failCount;
    @Column(name = "status", nullable = false, length = 20)
    private Integer status;

    @Column(name = "error_detail", columnDefinition = "TEXT")
    private String errorDetail;

    @Column(name = "operator", length = 100)
    private String operator;
}
