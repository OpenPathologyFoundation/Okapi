package com.starling.auth.model.db;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(schema = "iam", name = "user_feedback")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserFeedbackEntity {

    @Id
    @GeneratedValue
    @org.hibernate.annotations.UuidGenerator
    @Column(name = "feedback_id")
    private UUID feedbackId;

    @Column(name = "identity_id", nullable = false)
    private UUID identityId;

    @Column(name = "category", nullable = false)
    @Builder.Default
    private String category = "general";

    @Column(name = "body", nullable = false)
    private String body;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "context", nullable = false)
    @Builder.Default
    private Map<String, Object> context = Map.of();

    @Column(name = "status", nullable = false)
    @Builder.Default
    private String status = "pending";

    @Column(name = "admin_notes")
    private String adminNotes;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "acknowledged_at")
    private OffsetDateTime acknowledgedAt;

    @Column(name = "archived_at")
    private OffsetDateTime archivedAt;
}
