package com.shotaroi.loan.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Async
    public void log(Long actorCustomerId, String action, String details) {
        try {
            AuditLog auditLog = new AuditLog(actorCustomerId, action, details);
            auditLogRepository.save(auditLog);
            log.debug("Audit: actor={}, action={}", actorCustomerId, action);
        } catch (Exception e) {
            log.error("Failed to write audit log: {}", e.getMessage());
        }
    }

    public void logSync(Long actorCustomerId, String action, String details) {
        AuditLog auditLog = new AuditLog(actorCustomerId, action, details);
        auditLogRepository.save(auditLog);
        log.debug("Audit: actor={}, action={}", actorCustomerId, action);
    }
}
