package com.phonebiz.service;

import org.springframework.stereotype.Service;

@Service
public class AuditLogger {

    public void log(String action, String operator, String target, String detail) {
        // TODO: M15 will implement real audit logging
    }
}
