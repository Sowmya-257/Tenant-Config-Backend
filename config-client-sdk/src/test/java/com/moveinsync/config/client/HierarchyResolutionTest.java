package com.moveinsync.config.client;

import com.moveinsync.config.client.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests verifying hierarchical override resolution and explain functionality.
 */
class HierarchyResolutionTest {

    private TenantConfigSnapshot snapshot;

    @BeforeEach
    void setUp() {
        Map<String, ResolvedSetting> globalDefaults = new HashMap<>();
        globalDefaults.put("booking.cutoff.mins", new ResolvedSetting(
                "booking.cutoff.mins", "60", SettingType.NUMBER, Level.GLOBAL, 1L, "sysadmin", "Global default", Instant.now()
        ));
        globalDefaults.put("cab.types.allowed", new ResolvedSetting(
                "cab.types.allowed", "SEDAN,SUV", SettingType.LIST, Level.GLOBAL, 1L, "sysadmin", "Global default", Instant.now()
        ));

        Map<ConfigKey, ResolvedSetting> overrides = new HashMap<>();
        // Client level override
        overrides.put(ConfigKey.of("booking.cutoff.mins"), new ResolvedSetting(
                "booking.cutoff.mins", "45", SettingType.NUMBER, Level.CLIENT, 2L, "corp_admin", "Client standard", Instant.now()
        ));
        // Office level override for BLR-01
        overrides.put(ConfigKey.of("booking.cutoff.mins", "BLR-01"), new ResolvedSetting(
                "booking.cutoff.mins", "30", SettingType.NUMBER, Level.OFFICE, 3L, "blr_operator", "Bangalore traffic rule", Instant.now()
        ));
        // Shift level override for BLR-01 night shift
        overrides.put(ConfigKey.of("booking.cutoff.mins", "BLR-01", "NIGHT"), new ResolvedSetting(
                "booking.cutoff.mins", "15", SettingType.NUMBER, Level.SHIFT, 4L, "night_marshal", "Fast night allocation", Instant.now()
        ));

        snapshot = new TenantConfigSnapshot("TENANT_CORP", 4L, Instant.now(), overrides, globalDefaults);
    }

    @Test
    @DisplayName("Shift level override takes highest precedence")
    void testShiftLevelWins() {
        ResolvedSetting resolved = snapshot.resolve("booking.cutoff.mins", "BLR-01", "NIGHT");
        assertThat(resolved).isNotNull();
        assertThat(resolved.getValue()).isEqualTo("15");
        assertThat(resolved.getResolvedLevel()).isEqualTo(Level.SHIFT);
        assertThat(resolved.getAuthor()).isEqualTo("night_marshal");
    }

    @Test
    @DisplayName("Office level override wins when shift has no override")
    void testOfficeLevelWinsWhenShiftMissing() {
        ResolvedSetting resolved = snapshot.resolve("booking.cutoff.mins", "BLR-01", "MORNING");
        assertThat(resolved).isNotNull();
        assertThat(resolved.getValue()).isEqualTo("30");
        assertThat(resolved.getResolvedLevel()).isEqualTo(Level.OFFICE);
        assertThat(resolved.getAuthor()).isEqualTo("blr_operator");
    }

    @Test
    @DisplayName("Client level override wins when office has no override")
    void testClientLevelWinsWhenOfficeMissing() {
        ResolvedSetting resolved = snapshot.resolve("booking.cutoff.mins", "HYD-01", "MORNING");
        assertThat(resolved).isNotNull();
        assertThat(resolved.getValue()).isEqualTo("45");
        assertThat(resolved.getResolvedLevel()).isEqualTo(Level.CLIENT);
        assertThat(resolved.getAuthor()).isEqualTo("corp_admin");
    }

    @Test
    @DisplayName("Global default wins when no overrides exist")
    void testGlobalDefaultWins() {
        ResolvedSetting resolved = snapshot.resolve("cab.types.allowed", "HYD-01", "MORNING");
        assertThat(resolved).isNotNull();
        assertThat(resolved.getValue()).isEqualTo("SEDAN,SUV");
        assertThat(resolved.getResolvedLevel()).isEqualTo(Level.GLOBAL);
    }

    @Test
    @DisplayName("Explain mode correctly reports all evaluated levels and winning level")
    void testExplainMode() {
        ExplainResult explain = snapshot.explain("booking.cutoff.mins", "BLR-01", "NIGHT");
        assertThat(explain.getKey()).isEqualTo("booking.cutoff.mins");
        assertThat(explain.getResolvedValue()).isEqualTo("15");
        assertThat(explain.getResolvedLevel()).isEqualTo(Level.SHIFT);
        assertThat(explain.getEvaluatedLevels()).hasSize(4);

        ExplainResult.LevelCandidate shiftCandidate = explain.getEvaluatedLevels().get(0);
        assertThat(shiftCandidate.getLevel()).isEqualTo(Level.SHIFT);
        assertThat(shiftCandidate.isWinning()).isTrue();
        assertThat(shiftCandidate.getValue()).isEqualTo("15");
    }

    @Test
    @DisplayName("Throws SettingNotFoundException when key is unknown everywhere")
    void testUnknownKeyThrows() {
        assertThatThrownBy(() -> snapshot.explain("unknown.secret.key", "BLR-01", "NIGHT"))
                .isInstanceOf(SettingNotFoundException.class)
                .hasMessageContaining("unknown.secret.key");
    }
}
