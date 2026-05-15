package com.phonebiz.dto;

import lombok.Data;

@Data
public class UpdateOrgRequest {

    private String name;

    private String type;

    private String status;
}
