package com.sabrigur.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/** Writes audit log lines: who did what and when (NFR-005). The log time is added by the logger. */
@Service
public class AuditService {

    private static final Logger AUDIT = LoggerFactory.getLogger("AUDIT");

    public void log(String action, String details) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String user = auth == null ? "system" : auth.getName();
        AUDIT.info("user={} action={} {}", user, action, details);
    }
}
