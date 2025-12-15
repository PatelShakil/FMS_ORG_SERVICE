package com.shakilpatel.org.entity;

import com.shakilpatel.org.enums.ApprovalStatus;
import com.shakilpatel.org.enums.ApprovalStatusConverter;
import com.shakilpatel.org.enums.OrgType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "organizations", schema = "org_service")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Organization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "org_name", nullable = false, length = 200)
    private String orgName;

    @Column(name = "org_email", nullable = false, unique = true, length = 150)
    private String orgEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "org_type", nullable = false)
    private OrgType orgType;

    @Column(length = 100)
    private String sector;

    @Column(name = "registration_no", length = 100)
    private String registrationNo;

    @Column(length = 20)
    private String phone;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String state;

    @Column(length = 10)
    private String pincode;

    @Column(length = 200)
    private String website;

    // Status
    @Column(name = "status", columnDefinition = "approval_status")
    @Convert(converter = ApprovalStatusConverter.class)
    private ApprovalStatus status = ApprovalStatus.PENDING;



    @Column(name = "approved_by")
    private Long approvedBy;

    @Column(name = "approved_at",columnDefinition = "TIMESTAMP")
    @Convert(converter = LocalDateTimeConverter.class)
    private LocalDateTime approvedAt = LocalDateTime.now();

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    // Audit
    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP")
    @Convert(converter = LocalDateTimeConverter.class)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", columnDefinition = "TIMESTAMP")
    @Convert(converter = LocalDateTimeConverter.class)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(columnDefinition = "JSONB")
    private String metadata;

    // ✅ ADD: Let JPA handle timestamps
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

}

