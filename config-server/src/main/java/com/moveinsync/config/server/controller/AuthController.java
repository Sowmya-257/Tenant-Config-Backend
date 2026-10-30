package com.moveinsync.config.server.controller;

import com.moveinsync.config.server.security.JwtTokenProvider;
import com.moveinsync.config.server.security.UserRole;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Authentication controller providing JWT tokens for employee, operator, and admin personas.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final JwtTokenProvider tokenProvider;

    // Injects the JWT token provider.
    public AuthController(JwtTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
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
