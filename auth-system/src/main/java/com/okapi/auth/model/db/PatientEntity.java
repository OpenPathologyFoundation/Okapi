package com.okapi.auth.model.db;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(
        schema = "core",
        name = "patients",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"mrn"})
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id")
    private UUID id;

    @Column(name = "mrn", nullable = false, unique = true)
    private String mrn;

    @Column(name = "given_name", nullable = false)
    private String givenName;

    @Column(name = "family_name", nullable = false)
    private String familyName;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "dob")
    private LocalDate dob;

    @Column(name = "sex")
    private String sex;

    @Column(name = "phone")
    private String phone;

    @Column(name = "email")
    private String email;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "address")
    private Map<String, Object> address;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    @Column(name = "is_test_patient", nullable = false)
    @Builder.Default
    private boolean isTestPatient = false;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata")
    @Builder.Default
    private Map<String, Object> metadata = Collections.emptyMap();

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
