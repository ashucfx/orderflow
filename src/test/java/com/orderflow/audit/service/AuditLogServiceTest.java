package com.orderflow.audit.service;

import com.orderflow.audit.domain.AuditLog;
import com.orderflow.audit.repository.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditLogServiceImpl auditLogService;

    private UUID actorId;
    private UUID entityId;

    @BeforeEach
    void setUp() {
        actorId = UUID.randomUUID();
        entityId = UUID.randomUUID();
    }

    @Test
    void logEvent_savesAndReturnsAuditLog() {
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(inv -> {
            AuditLog record = inv.getArgument(0);
            record.setId(UUID.randomUUID());
            return record;
        });

        AuditLog result = auditLogService.logEvent(actorId, "ORDER_PLACED", "ORDER", entityId.toString(), "{\"amount\":100}");

        assertThat(result).isNotNull();
        assertThat(result.getId()).isNotNull();
        assertThat(result.getActorId()).isEqualTo(actorId);
        assertThat(result.getAction()).isEqualTo("ORDER_PLACED");
        assertThat(result.getEntityType()).isEqualTo("ORDER");
        assertThat(result.getEntityId()).isEqualTo(entityId.toString());
        assertThat(result.getMetadata()).isEqualTo("{\"amount\":100}");
        assertThat(result.getCreatedAt()).isNotNull();

        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    void getAuditLogsForEntity_returnsMatchingLogs() {
        AuditLog sample = AuditLog.builder()
                .id(UUID.randomUUID())
                .actorId(actorId)
                .action("ORDER_CONFIRMED")
                .entityType("ORDER")
                .entityId(entityId.toString())
                .build();

        when(auditLogRepository.findByEntityTypeAndEntityId("ORDER", entityId.toString()))
                .thenReturn(List.of(sample));

        List<AuditLog> results = auditLogService.getAuditLogsForEntity("ORDER", entityId.toString());

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getAction()).isEqualTo("ORDER_CONFIRMED");
    }
}
