package com.moveinsync.config.server.security;

import com.moveinsync.config.server.exception.TenantAccessDeniedException;

/**
 * Thread-local context maintaining tenant identity and verifying strict tenant isolation.
 */
public class TenantContext {

    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();
    private static final ThreadLocal<UserRole> CURRENT_ROLE = new ThreadLocal<>();

    // Sets the authenticated tenant ID and role on the current execution thread.
    public static void set(String tenantId, UserRole role) {
        CURRENT_TENANT.set(tenantId);
        CURRENT_ROLE.set(role);
    }

    // Returns the active tenant ID for this request.
    public static String getTenantId() {
        return CURRENT_TENANT.get();
    }

    // Returns the active user role for this request.
    public static UserRole getRole() {
        return CURRENT_ROLE.get();
    }

    // Enforces multi-tenant isolation, preventing cross-tenant configuration access.
    public static void assertCanAccessTenant(String targetTenantId) {
        String currentTenant = CURRENT_TENANT.get();
        UserRole currentRole = CURRENT_ROLE.get();

        // Admins and system-level requests can manage any tenant
        if (currentRole == UserRole.ROLE_ADMIN || "GLOBAL".equalsIgnoreCase(currentTenant) || "SYSTEM".equalsIgnoreCase(currentTenant)) {
            return;
        }

        // Dedicated tenant user can only access their own tenant
        if (currentTenant != null && currentTenant.equalsIgnoreCase(targetTenantId)) {
            return;
        }

        throw new TenantAccessDeniedException("Cross-tenant access forbidden: User from tenant '" +
                currentTenant + "' cannot access tenant '" + targetTenantId + "'");
    }

    // Clears the thread-local state to prevent context leakage across thread pools.
    public static void clear() {
        CURRENT_TENANT.remove();
        CURRENT_ROLE.remove();
    }
}
