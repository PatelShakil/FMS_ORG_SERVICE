package com.shakilpatel.org.ejb;

import com.shakilpatel.org.entity.ApprovalWorkflow;
import com.shakilpatel.org.entity.Organization;
import com.shakilpatel.org.entity.OrganizationAudit;
import com.shakilpatel.org.enums.ApprovalStatus;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Stateless
public class OrgBean {

    @PersistenceContext(unitName = "OrgPU")
    private EntityManager em;

    // Create organization
// ✅ FIX: Create organization using native SQL with proper NULL handling
    public Organization createOrganization(Organization org) {
        // Set timestamps if not set
        if (org.getCreatedAt() == null) {
            org.setCreatedAt(LocalDateTime.now());
        }
        if (org.getUpdatedAt() == null) {
            org.setUpdatedAt(LocalDateTime.now());
        }

        // Use native SQL with explicit casting for NULL values
        String sql = "INSERT INTO org_service.organizations " +
                "(org_name, org_email, org_type, sector, registration_no, phone, address, " +
                "city, state, pincode, website, status, approved_by, approved_at, " +
                "rejection_reason, created_by, created_at, updated_at, is_active, metadata) " +
                "VALUES (?, ?, ?::org_service.org_type, ?, ?, ?, ?, ?, ?, ?, ?, " +
                "?::org_service.approval_status, " +
                "CAST(? AS BIGINT), " +           // ✅ FIX: Cast NULL to BIGINT
                "CAST(? AS TIMESTAMP), " +         // ✅ FIX: Cast NULL to TIMESTAMP
                "?, ?, ?, ?, ?, CAST(? AS JSONB)) " +  // ✅ FIX: Cast NULL to JSONB
                "RETURNING id";

        Query query = em.createNativeQuery(sql);
        query.setParameter(1, org.getOrgName());
        query.setParameter(2, org.getOrgEmail());
        query.setParameter(3, org.getOrgType().name());
        query.setParameter(4, org.getSector());
        query.setParameter(5, org.getRegistrationNo());
        query.setParameter(6, org.getPhone());
        query.setParameter(7, org.getAddress());
        query.setParameter(8, org.getCity());
        query.setParameter(9, org.getState());
        query.setParameter(10, org.getPincode());
        query.setParameter(11, org.getWebsite());
        query.setParameter(12, org.getStatus().name());
        query.setParameter(13, org.getApprovedBy());  // Can be null
        query.setParameter(14, org.getApprovedAt() != null ? Timestamp.valueOf(org.getApprovedAt()) : null);  // Can be null
        query.setParameter(15, org.getRejectionReason());
        query.setParameter(16, org.getCreatedBy());
        query.setParameter(17, Timestamp.valueOf(org.getCreatedAt()));
        query.setParameter(18, Timestamp.valueOf(org.getUpdatedAt()));
        query.setParameter(19, org.getIsActive());
        query.setParameter(20, org.getMetadata());  // Can be null

        // Execute and get generated ID
        Long generatedId = ((Number) query.getSingleResult()).longValue();
        org.setId(generatedId);
        em.flush();


        // Create audit log
        logAudit(org.getId(), "CREATE", org.getCreatedBy(), null, null);

        return org;
    }

    // Find by ID
    public Optional<Organization> findById(Long id) {
        Organization org = em.find(Organization.class, id);
        return Optional.ofNullable(org);
    }

    // Find by email
    public Optional<Organization> findByEmail(String email) {
        try {
            TypedQuery<Organization> query = em.createQuery(
                    "SELECT o FROM Organization o WHERE o.orgEmail = :email",
                    Organization.class
            );
            query.setParameter("email", email);
            return Optional.of(query.getSingleResult());
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    // Find by created user
    public Optional<Organization> findByCreatedBy(Long userId) {
        try {
            TypedQuery<Organization> query = em.createQuery(
                    "SELECT o FROM Organization o WHERE o.createdBy = :userId",
                    Organization.class
            );
            query.setParameter("userId", userId);
            return Optional.of(query.getSingleResult());
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    // List all
    public List<Organization> listAll() {
        return em.createQuery("SELECT o FROM Organization o ORDER BY o.createdAt DESC", Organization.class)
                .getResultList();
    }

    // List by status
    public List<Organization> listByStatus(ApprovalStatus status) {
        TypedQuery<Organization> query = em.createQuery(
                "SELECT o FROM Organization o "
                + "WHERE o.status = CAST(:status AS approval_status) "
                + "ORDER BY o.createdAt DESC",
                Organization.class
        );
        query.setParameter("status", status.name());
        return query.getResultList();
    }

    // Update organization
    public Organization updateOrganization(Organization org, Long changedBy, String ipAddress) {
        org.setUpdatedAt(LocalDateTime.now());
        Organization updated = em.merge(org);
        em.flush();

        logAudit(org.getId(), "UPDATE", changedBy, ipAddress, null);

        return updated;
    }

    // Approve
    public Organization approve(Long orgId, Long approvedBy, String comments, String ipAddress) {
        Organization org = em.find(Organization.class, orgId);
        if (org == null) {
            throw new IllegalArgumentException("Organization not found");
        }

        ApprovalStatus previousStatus = org.getStatus();

        String sql = "UPDATE org_service.organizations SET " +
                "status = ?::org_service.approval_status, " +
                "approved_by = ?, approved_at = ?, updated_at = ? WHERE id = ?";

        Query query = em.createNativeQuery(sql);
        query.setParameter(1, ApprovalStatus.APPROVED.name());
        query.setParameter(2, approvedBy);
        query.setParameter(3, Timestamp.valueOf(LocalDateTime.now()));
        query.setParameter(4, Timestamp.valueOf(LocalDateTime.now()));
        query.setParameter(5, orgId);
        query.executeUpdate();

        em.refresh(org);
        em.flush();

        logWorkflow(orgId, previousStatus, ApprovalStatus.APPROVED, approvedBy, comments);
        logAudit(orgId, "APPROVE", approvedBy, ipAddress, null);

        return org;
    }

    // Reject
    public Organization reject(Long orgId, Long rejectedBy, String reason, String comments, String ipAddress) {
        Organization org = em.find(Organization.class, orgId);
        if (org == null) {
            throw new IllegalArgumentException("Organization not found");
        }

        ApprovalStatus previousStatus = org.getStatus();

        String sql = "UPDATE org_service.organizations SET " +
                "status = ?::org_service.approval_status, rejection_reason = ?, updated_at = ? WHERE id = ?";

        Query query = em.createNativeQuery(sql);
        query.setParameter(1, ApprovalStatus.REJECTED.name());
        query.setParameter(2, reason);
        query.setParameter(3, Timestamp.valueOf(LocalDateTime.now()));
        query.setParameter(4, orgId);
        query.executeUpdate();

        em.refresh(org);
        em.flush();

        logWorkflow(orgId, previousStatus, ApprovalStatus.REJECTED, rejectedBy, comments);
        logAudit(orgId, "REJECT", rejectedBy, ipAddress, null);

        return org;
    }

    // Suspend
    public Organization suspend(Long orgId, Long suspendedBy, String reason, String ipAddress) {
        Organization org = em.find(Organization.class, orgId);
        if (org == null) {
            throw new IllegalArgumentException("Organization not found");
        }

        ApprovalStatus previousStatus = org.getStatus();

        String sql = "UPDATE org_service.organizations SET " +
                "status = ?::org_service.approval_status, rejection_reason = ?, updated_at = ? WHERE id = ?";

        Query query = em.createNativeQuery(sql);
        query.setParameter(1, ApprovalStatus.SUSPENDED.name());
        query.setParameter(2, reason);
        query.setParameter(3, Timestamp.valueOf(LocalDateTime.now()));
        query.setParameter(4, orgId);
        query.executeUpdate();

        em.refresh(org);
        em.flush();

        logWorkflow(orgId, previousStatus, ApprovalStatus.SUSPENDED, suspendedBy, reason);
        logAudit(orgId, "SUSPEND", suspendedBy, ipAddress, null);

        return org;
    }

    // Delete
    public void delete(Long orgId, Long deletedBy, String ipAddress) {
        String sql = "UPDATE org_service.organizations SET is_active = false, updated_at = ? WHERE id = ?";
        Query query = em.createNativeQuery(sql);
        query.setParameter(1, Timestamp.valueOf(LocalDateTime.now()));
        query.setParameter(2, orgId);
        query.executeUpdate();

        logAudit(orgId, "DELETE", deletedBy, ipAddress, null);
    }

    // Log workflow (NATIVE SQL)
    private void logWorkflow(Long orgId, ApprovalStatus previousStatus,
                             ApprovalStatus newStatus, Long actionBy, String comments) {
        String sql = "INSERT INTO org_service.approval_workflow " +
                "(org_id, previous_status, new_status, action_by, action_at, comments) " +
                "VALUES (?, CAST(? AS org_service.approval_status), ?::org_service.approval_status, ?, ?, ?)";

        Query query = em.createNativeQuery(sql);
        query.setParameter(1, orgId);
        query.setParameter(2, previousStatus != null ? previousStatus.name() : null);
        query.setParameter(3, newStatus.name());
        query.setParameter(4, actionBy);
        query.setParameter(5, Timestamp.valueOf(LocalDateTime.now()));
        query.setParameter(6, comments);
        query.executeUpdate();
    }

    // Log audit (NATIVE SQL - FIXED!)
    private void logAudit(Long orgId, String action, Long changedBy,
                          String ipAddress, String userAgent) {
        String sql = "INSERT INTO org_service.organization_audit " +
                "(org_id, action, changed_by, timestamp, ip_address, user_agent, changes) " +
                "VALUES (?, ?, ?, ?, CAST(? AS INET), ?, CAST(? AS JSONB))";

        Query query = em.createNativeQuery(sql);
        query.setParameter(1, orgId);
        query.setParameter(2, action);
        query.setParameter(3, changedBy);
        query.setParameter(4, Timestamp.valueOf(LocalDateTime.now()));
        query.setParameter(5, ipAddress);
        query.setParameter(6, userAgent);
        query.setParameter(7, null);  // changes column
        query.executeUpdate();
    }

}
