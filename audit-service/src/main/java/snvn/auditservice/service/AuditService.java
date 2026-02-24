package snvn.auditservice.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import snvn.auditservice.model.AuditLog;
import snvn.auditservice.repository.AuditLogRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for handling audit business logic
 */
@Service
public class AuditService {

    @Autowired
    private AuditLogRepository auditLogRepository;

    /**
     * Create an audit log entry
     */
    public AuditLog createAuditLog(String entityType, String entityId, String action, String userId, String details) {
        AuditLog auditLog = new AuditLog();
        auditLog.setEntityType(entityType);
        auditLog.setEntityId(entityId);
        auditLog.setAction(action);
        auditLog.setUserId(userId);
        auditLog.setDetails(details);
        auditLog.setTimestamp(LocalDateTime.now());

        return auditLogRepository.save(auditLog);
    }

    /**
     * Get all audit logs
     */
    public List<AuditLog> getAllAuditLogs() {
        return auditLogRepository.findAll();
    }

    /**
     * Get audit logs by entity type
     */
    public List<AuditLog> getAuditLogsByEntityType(String entityType) {
        return auditLogRepository.findByEntityType(entityType);
    }

    /**
     * Get audit logs by user ID
     */
    public List<AuditLog> getAuditLogsByUserId(String userId) {
        return auditLogRepository.findByUserId(userId);
    }

    /**
     * Get audit logs by entity ID
     */
    public List<AuditLog> getAuditLogsByEntityId(String entityId) {
        return auditLogRepository.findByEntityId(entityId);
    }
}

