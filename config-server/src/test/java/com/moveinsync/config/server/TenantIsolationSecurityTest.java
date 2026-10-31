package com.moveinsync.config.server;

import com.moveinsync.config.server.exception.TenantAccessDeniedException;
import com.moveinsync.config.server.security.TenantContext;
import com.moveinsync.config.server.security.UserRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests authentication and strict multi-tenant data isolation.
 */
class TenantIsolationSecurityTest {

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Employee or Operator of Client A cannot access Client B configurations")
    void testTenantIsolationEnforced() {
        TenantContext.set("CLIENT_A", UserRole.ROLE_OPERATOR);

        // Accessing own tenant CLIENT_A succeeds
        assertThatCode(() -> TenantContext.assertCanAccessTenant("CLIENT_A"))
                .doesNotThrowAnyException();

        // Accessing foreign tenant CLIENT_B is strictly blocked
        assertThatThrownBy(() -> TenantContext.assertCanAccessTenant("CLIENT_B"))
                .isInstanceOf(TenantAccessDeniedException.class)
                .hasMessageContaining("Cross-tenant access forbidden");
    }

    @Test
    @DisplayName("Admin user can access any tenant configuration")
    void testAdminCanAccessAnyTenant() {
        TenantContext.set("SYSTEM", UserRole.ROLE_ADMIN);

        assertThatCode(() -> TenantContext.assertCanAccessTenant("CLIENT_A"))
                .doesNotThrowAnyException();
        assertThatCode(() -> TenantContext.assertCanAccessTenant("CLIENT_B"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Employee role is strictly read-only and cannot mutate configurations")
    void testEmployeeCannotModifyConfigurations() {
        TenantContext.set("CLIENT_A", UserRole.ROLE_EMPLOYEE);

        // Employee can read own tenant
        assertThatCode(() -> TenantContext.assertCanAccessTenant("CLIENT_A"))
                .doesNotThrowAnyException();

        // Employee is blocked from mutating configurations
        assertThatThrownBy(() -> TenantContext.assertCanModifyTenant("CLIENT_A"))
                .isInstanceOf(TenantAccessDeniedException.class)
                .hasMessageContaining("read-only");
    }

    @Test
    @DisplayName("Operator and Admin can modify configurations for authorized tenants")
    void testOperatorAndAdminCanModifyConfigurations() {
        // Operator can modify own tenant
        TenantContext.set("CLIENT_A", UserRole.ROLE_OPERATOR);
        assertThatCode(() -> TenantContext.assertCanModifyTenant("CLIENT_A"))
                .doesNotThrowAnyException();

        // Admin can modify any tenant
        TenantContext.set("SYSTEM", UserRole.ROLE_ADMIN);
        assertThatCode(() -> TenantContext.assertCanModifyTenant("CLIENT_A"))
                .doesNotThrowAnyException();
        assertThatCode(() -> TenantContext.assertCanModifyTenant("CLIENT_B"))
                .doesNotThrowAnyException();
    }
}
