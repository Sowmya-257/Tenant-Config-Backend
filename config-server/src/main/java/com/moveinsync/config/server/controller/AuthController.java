package com.moveinsync.config.server.controller;

import com.moveinsync.config.server.dto.LoginRequest;
import com.moveinsync.config.server.security.JwtTokenProvider;
import com.moveinsync.config.server.security.UserRole;
import io.jsonwebtoken.Claims;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Authentication controller providing JWT tokens, credential validation,
 * and role-based access for employee, operator, and admin personas.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final JwtTokenProvider tokenProvider;

    // Standard pre-configured user credentials & roles
    private static final Map<String, String> CREDENTIALS = Map.of(
            "alice_admin", "admin123",
            "bob_ops", "operator123",
            "charlie_emp", "employee123"
    );

    private static final Map<String, UserRole> USER_ROLES = Map.of(
            "alice_admin", UserRole.ROLE_ADMIN,
            "bob_ops", UserRole.ROLE_OPERATOR,
            "charlie_emp", UserRole.ROLE_EMPLOYEE
    );

    // Injects the JWT token provider.
    public AuthController(JwtTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    /**
     * Authenticates credentials and returns a signed JWT token with assigned role and tenant.
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        String username = request.getUsername() != null ? request.getUsername().trim() : "";
        String password = request.getPassword() != null ? request.getPassword().trim() : "";
        String tenantId = (request.getTenantId() != null && !request.getTenantId().isBlank())
                ? request.getTenantId().trim().toUpperCase()
                : "MICROSOFT";

        if (username.isEmpty() || password.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "status", 400,
                    "error", "Bad Request",
                    "message", "Username and password cannot be empty"
            ));
        }

        UserRole resolvedRole;

        // Check if standard persona
        if (CREDENTIALS.containsKey(username.toLowerCase())) {
            String expectedPassword = CREDENTIALS.get(username.toLowerCase());
            if (!expectedPassword.equals(password)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                        "status", 401,
                        "error", "Unauthorized",
                        "message", "Invalid credentials. Incorrect password for user '" + username + "'."
                ));
            }
            resolvedRole = USER_ROLES.get(username.toLowerCase());
        } else {
            // Custom user authentication
            if (password.length() < 4) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                        "status", 400,
                        "error", "Bad Request",
                        "message", "Password must be at least 4 characters long"
                ));
            }

            // Role selection for custom users
            String requestedRole = request.getRole() != null ? request.getRole().trim().toUpperCase() : "ROLE_OPERATOR";
            if ("ROLE_ADMIN".equals(requestedRole)) {
                if (!"admin123".equals(password)) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                            "status", 401,
                            "error", "Unauthorized",
                            "message", "Administrator role requires valid admin master credentials"
                    ));
                }
                resolvedRole = UserRole.ROLE_ADMIN;
            } else if ("ROLE_EMPLOYEE".equals(requestedRole)) {
                resolvedRole = UserRole.ROLE_EMPLOYEE;
            } else {
                resolvedRole = UserRole.ROLE_OPERATOR;
            }
        }

        String token = tokenProvider.generateToken(username, tenantId, resolvedRole);

        return ResponseEntity.ok(Map.of(
                "token", token,
                "username", username,
                "tenantId", tenantId,
                "role", resolvedRole.name(),
                "status", "SUCCESS",
                "message", "Authenticated successfully as " + resolvedRole.name()
        ));
    }

    /**
     * Verifies an active JWT session token and returns decoded identity & role.
     */
    @GetMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyToken(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "token", required = false) String paramToken) {
        String token = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7).trim();
        } else if (paramToken != null && !paramToken.isBlank()) {
            token = paramToken.trim();
        }

        if (token == null || token.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "valid", false,
                    "message", "Missing authentication token"
            ));
        }

        try {
            Claims claims = tokenProvider.parseToken(token);
            return ResponseEntity.ok(Map.of(
                    "valid", true,
                    "username", claims.getSubject(),
                    "tenantId", claims.get("tenantId", String.class),
                    "role", claims.get("role", String.class)
            ));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "valid", false,
                    "message", "Token expired or invalid: " + ex.getMessage()
            ));
        }
    }

    // Issues a signed JWT token for authentication and tenant isolation testing.
    @PostMapping("/token")
    public ResponseEntity<Map<String, Object>> generateToken(@RequestParam String username,
                                                             @RequestParam String tenantId,
                                                             @RequestParam(defaultValue = "ROLE_OPERATOR") UserRole role) {
        String token = tokenProvider.generateToken(username, tenantId, role);
        return ResponseEntity.ok(Map.of(
                "token", token,
                "username", username,
                "tenantId", tenantId,
                "role", role.name()
        ));
    }
}
