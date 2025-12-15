package com.shakilpatel.org.kafka.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrgRejectedEvent {

    private String eventId;
    private String eventType = "ORG_REJECTED";
    private Long orgId;
    private Long userId;
    private Long rejectedBy;
    private String rejectionReason;
    private Instant rejectedAt;
}
