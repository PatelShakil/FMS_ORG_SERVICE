package com.shakilpatel.org.rest;

import com.shakilpatel.org.dto.ApiResponse;
import com.shakilpatel.org.dto.ApprovalRequest;
import com.shakilpatel.org.dto.OrgRequest;
import com.shakilpatel.org.dto.OrgResponse;
import com.shakilpatel.org.ejb.KafkaProducerBean;
import com.shakilpatel.org.ejb.OrgBean;
import com.shakilpatel.org.entity.Organization;
import com.shakilpatel.org.enums.ApprovalStatus;
import com.shakilpatel.org.kafka.events.OrgApprovedEvent;
import com.shakilpatel.org.kafka.events.OrgRejectedEvent;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Path("/org")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class OrgResource {

    @Inject
    private OrgBean orgBean;

    @Inject
    private KafkaProducerBean kafkaProducer;

    @Inject
    private JsonWebToken jwt;

    @Context
    private HttpServletRequest request;

    @ConfigProperty(name = "kafka.producer.topic.approved")
    private String approvedTopic;

    @ConfigProperty(name = "kafka.producer.topic.rejected")
    private String rejectedTopic;

    // Get organization by ID
    @GET
    @Path("/{id}")
    @RolesAllowed({"PLATFORM_ADMIN", "ORG_OWNER", "ORG_ADMIN"})
    public Response getOrg(@PathParam("id") Long id) {
        Optional<Organization> org = orgBean.findById(id);

        if (org.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(ApiResponse.error("Organization not found"))
                    .build();
        }

        return Response.ok(ApiResponse.success("Organization retrieved", toResponse(org.get()))).build();
    }

    // List all organizations
    @GET
    @Path("/list")
    @RolesAllowed("PLATFORM_ADMIN")
    public Response listOrgs(@QueryParam("status") String status) {
        List<Organization> orgs;

        if (status != null && !status.isEmpty()) {
            try {
                ApprovalStatus approvalStatus = ApprovalStatus.valueOf(status.toUpperCase());
                orgs = orgBean.listByStatus(approvalStatus);
            } catch (IllegalArgumentException e) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(ApiResponse.error("Invalid status: " + status))
                        .build();
            }
        } else {
            orgs = orgBean.listAll();
        }

        List<OrgResponse> responses = orgs.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return Response.ok(ApiResponse.success("Organizations retrieved", responses)).build();
    }

    // Get my organization (for org owners/admins)
    @GET
    @Path("/my")
    @RolesAllowed({"ORG_OWNER", "ORG_ADMIN"})
    public Response getMyOrg() {
        Long userId = Long.parseLong(jwt.getSubject());

        // Try to find org created by this user
        Optional<Organization> org = orgBean.findByCreatedBy(userId);

        if (org.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(ApiResponse.error("No organization found for this user"))
                    .build();
        }

        return Response.ok(ApiResponse.success("Organization retrieved", toResponse(org.get()))).build();
    }

    // Update organization
    @PUT
    @Path("/{id}")
    @RolesAllowed({"ORG_OWNER", "ORG_ADMIN"})
    public Response updateOrg(@PathParam("id") Long id, OrgRequest request) {
        Optional<Organization> existing = orgBean.findById(id);

        if (existing.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(ApiResponse.error("Organization not found"))
                    .build();
        }

        Organization org = existing.get();
        Long userId = Long.parseLong(jwt.getSubject());

        // Check ownership
        if (!org.getCreatedBy().equals(userId)) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(ApiResponse.error("You can only update your own organization"))
                    .build();
        }

        // Update fields
        org.setOrgName(request.getOrgName());
        org.setPhone(request.getPhone());
        org.setAddress(request.getAddress());
        org.setCity(request.getCity());
        org.setState(request.getState());
        org.setPincode(request.getPincode());
        org.setWebsite(request.getWebsite());
        org.setSector(request.getSector());
        org.setUpdatedAt(LocalDateTime.now());

        String ipAddress = getClientIp();
        Organization updated = orgBean.updateOrganization(org, userId, ipAddress);

        return Response.ok(ApiResponse.success("Organization updated", toResponse(updated))).build();
    }

    // Approve organization
    @PUT
    @Path("/{id}/approve")
    @RolesAllowed("PLATFORM_ADMIN")
    public Response approveOrg(@PathParam("id") Long id, ApprovalRequest approvalRequest) {
        Optional<Organization> existing = orgBean.findById(id);

        if (existing.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(ApiResponse.error("Organization not found"))
                    .build();
        }

        Organization org = existing.get();

        if (org.getStatus() != ApprovalStatus.PENDING) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ApiResponse.error("Only PENDING organizations can be approved"))
                    .build();
        }

        Long adminId = Long.parseLong(jwt.getSubject());
        String ipAddress = getClientIp();
        String comments = approvalRequest != null ? approvalRequest.getComments() : null;

        Organization approved = orgBean.approve(id, adminId, comments, ipAddress);

        // Publish org.approved event
        OrgApprovedEvent event = new OrgApprovedEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setOrgId(approved.getId());
        event.setUserId(approved.getCreatedBy());
        event.setApprovedBy(adminId);
        event.setApprovedAt(approved.getApprovedAt());

        approvedTopic = ConfigProvider.getConfig().getValue("kafka.producer.topic.approved", String.class);
        kafkaProducer.sendEvent(approvedTopic, event);

        return Response.ok(ApiResponse.success("Organization approved", toResponse(approved))).build();
    }

    // Reject organization
    @PUT
    @Path("/{id}/reject")
    @RolesAllowed("PLATFORM_ADMIN")
    public Response rejectOrg(@PathParam("id") Long id, ApprovalRequest approvalRequest) {
        Optional<Organization> existing = orgBean.findById(id);

        if (existing.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(ApiResponse.error("Organization not found"))
                    .build();
        }

        Organization org = existing.get();

        if (org.getStatus() != ApprovalStatus.PENDING) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ApiResponse.error("Only PENDING organizations can be rejected"))
                    .build();
        }

        Long adminId = Long.parseLong(jwt.getSubject());
        String ipAddress = getClientIp();
        String reason = approvalRequest != null ? approvalRequest.getRejectionReason() : "No reason provided";
        String comments = approvalRequest != null ? approvalRequest.getComments() : null;

        Organization rejected = orgBean.reject(id, adminId, reason, comments, ipAddress);

        // Publish org.rejected event
        OrgRejectedEvent event = new OrgRejectedEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setOrgId(rejected.getId());
        event.setUserId(rejected.getCreatedBy());
        event.setRejectedBy(adminId);
        event.setRejectionReason(reason);
        event.setRejectedAt(Instant.now());

        rejectedTopic = ConfigProvider.getConfig().getValue("kafka.producer.topic.rejected", String.class);
        kafkaProducer.sendEvent(rejectedTopic, event);

        return Response.ok(ApiResponse.success("Organization rejected", toResponse(rejected))).build();
    }

    // Soft delete
    @DELETE
    @Path("/{id}")
    @RolesAllowed("PLATFORM_ADMIN")
    public Response deleteOrg(@PathParam("id") Long id) {
        Optional<Organization> existing = orgBean.findById(id);

        if (existing.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(ApiResponse.error("Organization not found"))
                    .build();
        }

        Long adminId = Long.parseLong(jwt.getSubject());
        String ipAddress = getClientIp();

        orgBean.delete(id, adminId, ipAddress);

        return Response.ok(ApiResponse.success("Organization deleted", null)).build();
    }

    // Helper: Convert to response DTO
    private OrgResponse toResponse(Organization org) {
        return new OrgResponse(
                org.getId(),
                org.getOrgName(),
                org.getOrgEmail(),
                org.getOrgType(),
                org.getSector(),
                org.getRegistrationNo(),
                org.getPhone(),
                org.getAddress(),
                org.getCity(),
                org.getState(),
                org.getPincode(),
                org.getWebsite(),
                org.getStatus(),
                org.getApprovedBy(),
                org.getApprovedAt(),
                org.getRejectionReason(),
                org.getCreatedBy(),
                org.getCreatedAt(),
                org.getUpdatedAt(),
                org.getIsActive()
        );
    }

    // Helper: Get client IP
    private String getClientIp() {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
