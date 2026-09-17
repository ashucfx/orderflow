package com.orderflow.common.idempotency.service;

import com.orderflow.common.idempotency.domain.IdempotencyRecord;

import java.util.Optional;

public interface IdempotencyService {

    Optional<IdempotencyRecord> findValidRecord(String key);

    IdempotencyRecord saveRecord(String key, int statusCode, String responseBody, long ttlSeconds);

    int cleanupExpiredRecords();
}
