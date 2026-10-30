package com.moveinsync.config.client;

import com.moveinsync.config.client.model.TimeWindow;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for operating time windows and midnight crossover calculations.
 */
class TimeWindowTest {

    @Test
    @DisplayName("Night safety window spanning midnight (20:00 to 06:00)")
    void testOvernightWindow() {
        TimeWindow window = TimeWindow.parse("20:00-06:00");
        assertThat(window.spansOvernight()).isTrue();

        // 23:30 is inside window
        assertThat(window.isWithinWindow(LocalTime.of(23, 30))).isTrue();
        // 02:15 is inside window
        assertThat(window.isWithinWindow(LocalTime.of(2, 15))).isTrue();
        // 20:00 boundary is inside window
        assertThat(window.isWithinWindow(LocalTime.of(20, 0))).isTrue();
        // 14:00 afternoon is outside window
        assertThat(window.isWithinWindow(LocalTime.of(14, 0))).isFalse();
        // 06:00 morning boundary is outside window
        assertThat(window.isWithinWindow(LocalTime.of(6, 0))).isFalse();
    }

    @Test
    @DisplayName("Standard daytime window (09:00 to 18:00)")
    void testDayWindow() {
        TimeWindow window = TimeWindow.parse("09:00-18:00");
        assertThat(window.spansOvernight()).isFalse();

        assertThat(window.isWithinWindow(LocalTime.of(11, 0))).isTrue();
        assertThat(window.isWithinWindow(LocalTime.of(19, 0))).isFalse();
        assertThat(window.isWithinWindow(LocalTime.of(8, 0))).isFalse();
    }

    @Test
    @DisplayName("Invalid window format or identical start/end throws exception")
    void testInvalidWindow() {
        assertThatThrownBy(() -> TimeWindow.parse("invalid"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TimeWindow.parse("10:00-10:00"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
