package com.cmbchina.phonebiz.enums;

public enum PhoneStatus {
    UNASSIGNED("未分配"),
    ASSIGNED("已分配"),
    IN_USE("使用中"),
    SUSPENDED("已停用"),
    RECYCLED("已回收");

    private final String description;

    PhoneStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
