package com.shakilpatel.org.dto;

import lombok.Data;

@Data
public class ApprovalRequest {

    private String comments;
    private String rejectionReason; // Only for rejection
}
