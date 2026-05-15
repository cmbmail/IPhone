package com.cmbchina.phonebiz.enums;

public enum WorkOrderStatus {
    PENDING("待处理"),
    PROCESSING("处理中"),
    APPROVED("已审批"),
    REJECTED("已拒绝"),
    COMPLETED("已完成"),
    CANCELLED("已取消");

    private final String description;

    WorkOrderStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
