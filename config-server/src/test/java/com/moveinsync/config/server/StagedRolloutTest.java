package com.moveinsync.config.server;

import com.moveinsync.config.client.model.Level;
import com.moveinsync.config.client.model.ResolvedSetting;
import com.moveinsync.config.server.domain.StagedRollout;
import com.moveinsync.config.server.dto.ChangeSetItemRequest;
import com.moveinsync.config.server.dto.ChangeSetRequest;
import com.moveinsync.config.server.service.ChangeSetService;
import com.moveinsync.config.server.service.HierarchyResolutionService;
import com.moveinsync.config.server.service.StagedRolloutService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests phased canary rollouts to pilot offices and subsequent tenant-wide promotion.
 */
@SpringBootTest
@ActiveProfiles("test")
class StagedRolloutTest {

    @Autowired
    private StagedRolloutService stagedRolloutService;

    @Autowired
    private ChangeSetService changeSetService;

    @Autowired
    private HierarchyResolutionService resolutionService;

    @Test
    @DisplayName("Staged rollout deploys to pilot offices first and promotes to all offices later")
    void testPhasedCanaryAndPromotion() {
        String tenant = "TENANT_STAGED";

        // Initial client-wide value: 60m
        changeSetService.applyChangeSet(tenant, new ChangeSetRequest(
                "corp_admin", "Base config",
                List.of(new ChangeSetItemRequest("booking.cutoff.mins", Level.CLIENT, null, null, "60m", false))
        ));

        // Start staged canary rollout to BLR-01 and BLR-02 with canary value: 30m
        StagedRollout rollout = stagedRolloutService.createRollout(
                tenant, "booking.cutoff.mins", "30m",
                List.of("BLR-01", "BLR-02"), "operator", "Bangalore traffic pilot"
        );

        assertThat(rollout.getStatus()).isEqualTo(StagedRollout.RolloutStatus.CANARY_ACTIVE);

        // Pilot office BLR-01 receives canary 30m
        ResolvedSetting blr = resolutionService.resolve(tenant, "booking.cutoff.mins", "BLR-01", null);
        assertThat(blr.getValue()).isEqualTo("30m");
        assertThat(blr.getResolvedLevel()).isEqualTo(Level.OFFICE);

        // Other office HYD-01 still uses base 60m
        ResolvedSetting hyd = resolutionService.resolve(tenant, "booking.cutoff.mins", "HYD-01", null);
        assertThat(hyd.getValue()).isEqualTo("60m");
        assertThat(hyd.getResolvedLevel()).isEqualTo(Level.CLIENT);

        // Promote rollout to everyone
        StagedRollout promoted = stagedRolloutService.promoteRollout(rollout.getId(), "corp_admin", "Successful pilot");
        assertThat(promoted.getStatus()).isEqualTo(StagedRollout.RolloutStatus.PROMOTED);

        // Now HYD-01 also receives promoted 30m
        ResolvedSetting hydAfter = resolutionService.resolve(tenant, "booking.cutoff.mins", "HYD-01", null);
        assertThat(hydAfter.getValue()).isEqualTo("30m");
        assertThat(hydAfter.getResolvedLevel()).isEqualTo(Level.CLIENT);
    }
}
