package com.phonebiz.dto;

import lombok.Data;

@Data
public class ImportOrgItem {
    private String name;
    private Long parentId;  // null for root
    private String type;    // group/subsidiary/dept
}
