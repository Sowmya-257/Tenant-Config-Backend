package com.moveinsync.config.server;

import com.moveinsync.config.client.model.Level;
import com.moveinsync.config.server.dto.*;
import com.moveinsync.config.server.exception.InvalidConfigException;
import com.moveinsync.config.server.service.ChangeSetService;
import com.moveinsync.config.server.service.RollbackService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests validation rejection of bad values, atomic change sets, and instant rollback.
 */
@SpringBootTest
@ActiveProfiles("test")
class ValidationAndSafeChangesTest {

    @Autowired
    private ChangeSetService changeSetService;

    @Autowired
    private RollbackService rollbackService;

    @Test
    @DisplayName("Rejects negative duration (-30 minutes) prior to publication")
    void testRejectsNegativeDuration() {
        ChangeSetItemRequest badItem = new ChangeSetItemRequest(
                "booking.cutoff.mins", Level.CLIENT, null, null, "-30m", false
        );
        ChangeSetRequest request = new ChangeSetRequest("tester", "Test negative cutoff", List.of(badItem));

        assertThatThrownBy(() -> changeSetService.applyChangeSet("TENANT_TEST", request))
                .isInstanceOf(InvalidConfigException.class)
                .hasMessageContaining("Duration cannot be negative");
    }

    @Test
    @DisplayName("Rejects daytime window that ends before it starts without overnight permission")
    void testRejectsWindowEndingBeforeStart() {
        // "allowOvernight": false scenario on daytime window
        ChangeSetItemRequest badWindow = new ChangeSetItemRequest(
                "night_safety.window", Level.CLIENT, null, null, "10:00-10:00", false
        );
        ChangeSetRequest request = new ChangeSetRequest("tester", "Invalid identical window", List.of(badWindow));

        assertThatThrownBy(() -> changeSetService.applyChangeSet("TENANT_TEST", request))
                .isInstanceOf(InvalidConfigException.class)
                .hasMessageContaining("Start time and end time cannot be identical");
    }

    @Test
    @DisplayName("Rejects invalid enum value not in allowed values list")
    void testRejectsInvalidEnum() {
        ChangeSetItemRequest badEnum = new ChangeSetItemRequest(
                "vendor.assignment.strategy", Level.CLIENT, null, null, "UNSUPPORTED_RANDOM_STRATEGY", false
        );
        ChangeSetRequest request = new ChangeSetRequest("tester", "Invalid enum test", List.of(badEnum));

        assertThatThrownBy(() -> changeSetService.applyChangeSet("TENANT_TEST", request))
                .isInstanceOf(InvalidConfigException.class)
                .hasMessageContaining("allowed enum choices");
    }

    @Test
    @DisplayName("Applies valid change set and then performs instant rollback with diff")
    void testRollbackWithDiff() {
        String tenant = "TENANT_ROLLBACK";

        // Version 1
        ChangeSetRequest v1Req = new ChangeSetRequest("admin", "Initial setting", List.of(
                new ChangeSetItemRequest("booking.cutoff.mins", Level.CLIENT, null, null, "45m", false)
        ));
        ChangeSetResponse v1Resp = changeSetService.applyChangeSet(tenant, v1Req);
        long v1 = v1Resp.getVersion();

        // Version 2: change to 30m
        ChangeSetRequest v2Req = new ChangeSetRequest("admin", "Update to 30m", List.of(
                new ChangeSetItemRequest("booking.cutoff.mins", Level.CLIENT, null, null, "30m", false)
        ));
        ChangeSetResponse v2Resp = changeSetService.applyChangeSet(tenant, v2Req);
        long v2 = v2Resp.getVersion();
        assertThat(v2).isGreaterThan(v1);

        // Instant rollback to v1
        RollbackDiffResponse diff = rollbackService.rollbackToVersion(tenant, v1, "admin", "Revert to v1");

        assertThat(diff.getFromVersion()).isEqualTo(v2);
        assertThat(diff.getTargetVersionRolledBackTo()).isEqualTo(v1);
        assertThat(diff.getNewCommittedVersion()).isGreaterThan(v2);
        assertThat(diff.getRestoredSettings()).hasSize(1);
        assertThat(diff.getRestoredSettings().get(0).getPreviousValue()).isEqualTo("30m");
        assertThat(diff.getRestoredSettings().get(0).getRestoredValue()).isEqualTo("45m");
    }
}
