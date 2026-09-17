package com.orderflow.common.idempotency.service;

import com.orderflow.common.idempotency.domain.IdempotencyRecord;
import com.orderflow.common.idempotency.repository.IdempotencyRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

    @Mock
    private IdempotencyRecordRepository repository;

    @InjectMocks
    private IdempotencyServiceImpl idempotencyService;

    private IdempotencyRecord activeRecord;
    private IdempotencyRecord expiredRecord;

    @BeforeEach
    void setUp() {
        activeRecord = IdempotencyRecord.builder()
                .key("KEY-ACTIVE")
                .statusCode(200)
                .responseBody("{\"status\":\"ok\"}")
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        expiredRecord = IdempotencyRecord.builder()
                .key("KEY-EXPIRED")
                .statusCode(200)
                .responseBody("{\"status\":\"ok\"}")
                .createdAt(Instant.now().minusSeconds(7200))
                .expiresAt(Instant.now().minusSeconds(3600))
                .build();
    }

    @Test
    void findValidRecord_whenKeyBlank_returnsEmpty() {
        assertThat(idempotencyService.findValidRecord(null)).isEmpty();
        assertThat(idempotencyService.findValidRecord("   ")).isEmpty();
        verifyNoInteractions(repository);
    }

    @Test
    void findValidRecord_whenNotFound_returnsEmpty() {
        when(repository.findByKey("UNKNOWN")).thenReturn(Optional.empty());

        assertThat(idempotencyService.findValidRecord("UNKNOWN")).isEmpty();
    }

    @Test
    void findValidRecord_whenFoundAndNotExpired_returnsRecord() {
        when(repository.findByKey("KEY-ACTIVE")).thenReturn(Optional.of(activeRecord));

        Optional<IdempotencyRecord> result = idempotencyService.findValidRecord("KEY-ACTIVE");

        assertThat(result).isPresent();
        assertThat(result.get().getKey()).isEqualTo("KEY-ACTIVE");
        assertThat(result.get().getStatusCode()).isEqualTo(200);
        verify(repository, never()).delete(any());
    }

    @Test
    void findValidRecord_whenFoundAndExpired_deletesAndReturnsEmpty() {
        when(repository.findByKey("KEY-EXPIRED")).thenReturn(Optional.of(expiredRecord));

        Optional<IdempotencyRecord> result = idempotencyService.findValidRecord("KEY-EXPIRED");

        assertThat(result).isEmpty();
        verify(repository).delete(expiredRecord);
    }

    @Test
    void saveRecord_validKey_persistsAndReturnsRecord() {
        when(repository.save(any(IdempotencyRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        IdempotencyRecord record = idempotencyService.saveRecord("KEY-NEW", 201, "{\"id\":\"123\"}", 86400);

        assertThat(record.getKey()).isEqualTo("KEY-NEW");
        assertThat(record.getStatusCode()).isEqualTo(201);
        assertThat(record.getResponseBody()).isEqualTo("{\"id\":\"123\"}");
        assertThat(record.getExpiresAt()).isAfter(Instant.now());
        verify(repository).save(any(IdempotencyRecord.class));
    }

    @Test
    void saveRecord_blankKey_throwsException() {
        assertThatThrownBy(() -> idempotencyService.saveRecord("  ", 200, "{}", 3600))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be null or blank");
    }

    @Test
    void cleanupExpiredRecords_delegatesToRepository() {
        when(repository.deleteExpiredRecords(any(Instant.class))).thenReturn(5);

        int count = idempotencyService.cleanupExpiredRecords();

        assertThat(count).isEqualTo(5);
        verify(repository).deleteExpiredRecords(any(Instant.class));
    }
}
