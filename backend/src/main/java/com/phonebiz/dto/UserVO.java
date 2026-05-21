package com.phonebiz.dto;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class UserVO {
    private Long id;
    private Long employeeId;
    private String name;
    private String username;
    private Long orgId;
    private String orgName;
    private Long roleId;
    private String roleName;
    private String roleCode;
    private String status;
    private LocalDateTime updatedAt;
}
