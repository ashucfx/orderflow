package com.orderflow.audit.service;

import com.orderflow.audit.domain.AuditLog;

import java.util.List;
import java.util.UUID;

public interface AuditLogService {

    AuditLog logEvent(UUID actorId, String action, String entityType, String entityId, String metadata);

    List<AuditLog> getAuditLogsForEntity(String entityType, String entityId);
}
