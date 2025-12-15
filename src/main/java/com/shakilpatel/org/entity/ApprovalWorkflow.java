package com.shakilpatel.org.entity;

import com.shakilpatel.org.enums.ApprovalStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "approval_workflow", schema = "org_service")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalWorkflow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "org_id", nullable = false)
    private Long orgId;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status")
    private ApprovalStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false)
    private ApprovalStatus newStatus;

    @Column(name = "action_by", nullable = false)
    private Long actionBy;

    @Column(name = "action_at", nullable = false)
    private Instant actionAt = Instant.now();

    @Column(columnDefinition = "TEXT")
    private String comments;
}
