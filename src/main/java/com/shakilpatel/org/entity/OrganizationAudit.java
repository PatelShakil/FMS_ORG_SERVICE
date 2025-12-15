package com.shakilpatel.org.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "organization_audit", schema = "org_service")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "org_id", nullable = false)
    private Long orgId;

    @Column(nullable = false, length = 50)
    private String action;

    @Column(name = "changed_by", nullable = false)
    private Long changedBy;

    @Column(columnDefinition = "JSONB")
    private String changes;

    @Column(nullable = false)
    private Instant timestamp = Instant.now();

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;
}
