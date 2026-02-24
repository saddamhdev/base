package snvn.auditservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import snvn.auditservice.model.AuditLog;

import java.util.List;

/**
 * Repository for AuditLog entity
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByEntityType(String entityType);

    List<AuditLog> findByEntityId(String entityId);

    List<AuditLog> findByUserId(String userId);

    List<AuditLog> findByAction(String action);
}

