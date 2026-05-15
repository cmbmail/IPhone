package com.cmbchina.phonebiz.enums;

public enum WorkOrderType {
    ASSIGN("号码分配"),
    SUSPEND("号码停用"),
    RESUME("号码恢复"),
    RECYCLE("号码回收"),
    REASSIGN("号码重分配"),
    OTHER("其他");

    private final String description;

    WorkOrderType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
