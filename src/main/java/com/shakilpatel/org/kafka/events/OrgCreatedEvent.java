package com.shakilpatel.org.kafka.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrgCreatedEvent {

    private String eventId;
    private String eventType = "ORG_CREATED";
    private Long orgId;
    private Long userId;
    private String orgName;
    private String status = "PENDING";
}
