package com.shakilpatel.org.dto;

import com.shakilpatel.org.enums.ApprovalStatus;
import com.shakilpatel.org.enums.OrgType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrgResponse {

    private Long id;
    private String orgName;
    private String orgEmail;
    private OrgType orgType;
    private String sector;
    private String registrationNo;
    private String phone;
    private String address;
    private String city;
    private String state;
    private String pincode;
    private String website;
    private ApprovalStatus status;
    private Long approvedBy;
    private LocalDateTime approvedAt;
    private String rejectionReason;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean isActive;
}
