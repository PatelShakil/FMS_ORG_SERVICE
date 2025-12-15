package com.shakilpatel.org.dto;

import com.shakilpatel.org.enums.OrgType;
import lombok.Data;

@Data
public class OrgRequest {

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
