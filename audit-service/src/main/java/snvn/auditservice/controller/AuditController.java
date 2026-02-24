package snvn.auditservice.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import snvn.auditservice.model.AuditLog;
import snvn.auditservice.service.AuditService;

import java.util.List;

/**
 * Audit Controller for handling audit log requests
 */
@RestController
@RequestMapping("/api/audit")
public class AuditController {

    @Autowired
    private AuditService auditService;

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Audit Service is running");
    }

    /**
     * Get all audit logs
     */
    @GetMapping("/logs")
    public ResponseEntity<List<AuditLog>> getAllLogs() {
        return ResponseEntity.ok(auditService.getAllAuditLogs());
    }

    /**
     * Get audit logs by entity type
     */
    @GetMapping("/logs/entity/{entityType}")
    public ResponseEntity<List<AuditLog>> getLogsByEntityType(@PathVariable String entityType) {
        return ResponseEntity.ok(auditService.getAuditLogsByEntityType(entityType));
    }

    /**
     * Get audit logs by user
     */
    @GetMapping("/logs/user/{userId}")
    public ResponseEntity<List<AuditLog>> getLogsByUser(@PathVariable String userId) {
        return ResponseEntity.ok(auditService.getAuditLogsByUserId(userId));
    }
}

