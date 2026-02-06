package com.okapi.auth.model.db;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(schema = "iam", name = "audit_event")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditEventEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "event_id")
    private UUID eventId;

    @Column(name = "occurred_at", nullable = false)
    @Builder.Default
    private OffsetDateTime occurredAt = OffsetDateTime.now();

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "actor_identity_id")
    private UUID actorIdentityId;

    @Column(name = "actor_provider_id")
    private String actorProviderId;

    @Column(name = "actor_external_subject")
    private String actorExternalSubject;

    @Column(name = "target_entity_type")
    private String targetEntityType;

    @Column(name = "target_entity_id")
    private UUID targetEntityId;

    @Column(name = "target_identity_id")
    private UUID targetIdentityId;

    @Column(name = "outcome")
    private String outcome;

    @Column(name = "outcome_reason")
    private String outcomeReason;

    @Column(name = "request_id")
    private UUID requestId;

    @Column(name = "session_id")
    private UUID sessionId;

    @JdbcTypeCode(SqlTypes.INET)
    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "details")
    private String details;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", nullable = false)
    @Builder.Default
    private Map<String, Object> metadata = java.util.Collections.emptyMap();
}
