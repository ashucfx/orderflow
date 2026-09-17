package com.orderflow.common.idempotency.service;

import com.orderflow.common.idempotency.domain.IdempotencyRecord;
import com.orderflow.common.idempotency.repository.IdempotencyRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyServiceImpl implements IdempotencyService {

    private final IdempotencyRecordRepository repository;

    @Override
    @Transactional
    public Optional<IdempotencyRecord> findValidRecord(String key) {
        if (key == null || key.isBlank()) {
            return Optional.empty();
        }

        String normalizedKey = key.trim();
        Optional<IdempotencyRecord> recordOpt = repository.findByKey(normalizedKey);

        if (recordOpt.isEmpty()) {
            return Optional.empty();
        }

        IdempotencyRecord record = recordOpt.get();
        if (record.isExpired()) {
            log.debug("Idempotency key '{}' has expired. Removing record.", normalizedKey);
            repository.delete(record);
            return Optional.empty();
        }

        log.debug("Found valid unexpired idempotency record for key '{}'", normalizedKey);
        return Optional.of(record);
    }

    @Override
    @Transactional
    public IdempotencyRecord saveRecord(String key, int statusCode, String responseBody, long ttlSeconds) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Idempotency key cannot be null or blank");
        }

        String normalizedKey = key.trim();
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(ttlSeconds > 0 ? ttlSeconds : 86400);

        IdempotencyRecord record = IdempotencyRecord.builder()
                .key(normalizedKey)
                .statusCode(statusCode)
                .responseBody(responseBody != null ? responseBody : "")
                .createdAt(now)
                .expiresAt(expiresAt)
                .build();

        log.debug("Saving idempotency record for key '{}', expires at {}", normalizedKey, expiresAt);
        return repository.save(record);
    }

    @Override
    @Transactional
    public int cleanupExpiredRecords() {
        int deletedCount = repository.deleteExpiredRecords(Instant.now());
        log.info("Cleaned up {} expired idempotency records", deletedCount);
        return deletedCount;
    }
}
