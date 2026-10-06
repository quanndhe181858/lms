package com.enterprise.lms.module.leave.service;

import com.enterprise.lms.module.leave.entity.SystemAuditLog;
import com.enterprise.lms.module.leave.repository.SystemAuditLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final SystemAuditLogRepository systemAuditLogRepository;
    private final ObjectMapper objectMapper;

    public void record(String entityName,
                       Long entityId,
                       String action,
                       Long actorUserId,
                       Object previousState,
                       Object newState,
                       String ipAddress,
                       Object details) {
        persist(entityName, entityId, action, actorUserId, previousState, newState, ipAddress, details);
    }

    @Async
    @Transactional
    public void recordAsync(String entityName,
                           Long entityId,
                           String action,
                           Long actorUserId,
                           Object previousState,
                           Object newState,
                           String ipAddress,
                           Object details) {
        persist(entityName, entityId, action, actorUserId, previousState, newState, ipAddress, details);
    }

    private void persist(String entityName,
                        Long entityId,
                        String action,
                        Long actorUserId,
                        Object previousState,
                        Object newState,
                        String ipAddress,
                        Object details) {
        if (entityName == null || entityName.isBlank()) {
            throw new IllegalArgumentException("ENTITY_NAME_REQUIRED");
        }
        if (entityId == null) {
            throw new IllegalArgumentException("ENTITY_ID_REQUIRED");
        }
        if (action == null || action.isBlank()) {
            throw new IllegalArgumentException("ACTION_REQUIRED");
        }

        SystemAuditLog auditLog = SystemAuditLog.builder()
                .entityName(entityName)
                .entityId(entityId)
                .action(action)
                .actorUserId(actorUserId)
                .previousState(toJson(previousState))
                .newState(toJson(newState))
                .detailsJson(toJson(details))
                .ipAddress(ipAddress)
                .build();

        systemAuditLogRepository.save(auditLog);
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return Objects.toString(value, null);
        }
    }
}
