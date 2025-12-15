package com.shakilpatel.org.kafka.events;

import com.shakilpatel.org.enums.OrgType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrgRegistrationEvent {

    private String eventId;
    private String eventType = "ORG_REGISTRATION_REQUESTED";
    private Long userId;
    private String email;
    private OrgData orgData;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrgData {

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
    }
}
