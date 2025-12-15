package com.shakilpatel.org.kafka.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrgApprovedEvent {

    private String eventId;
    private String eventType = "ORG_APPROVED";
    private Long orgId;
    private Long userId;
    private Long approvedBy;
    private LocalDateTime approvedAt;
}
