package com.moveinsync.config.server.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request payload for user authentication and role verification.
 */
public class LoginRequest {

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Password is required")
    private String password;

    private String tenantId;
    private String role;

    public LoginRequest() {}

    public LoginRequest(String username, String password, String tenantId, String role) {
        this.username = username;
        this.password = password;
        this.tenantId = tenantId;
        this.role = role;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
