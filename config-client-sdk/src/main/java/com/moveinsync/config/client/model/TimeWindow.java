package com.moveinsync.config.client.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Represents a daily operating time window like a night safety window.
 */
public class TimeWindow {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final LocalTime startTime;
    private final LocalTime endTime;

    // Creates a time window given start and end times.
    @JsonCreator
    public TimeWindow(@JsonProperty("startTime") LocalTime startTime,
                      @JsonProperty("endTime") LocalTime endTime) {
        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException("Start time and end time cannot be null");
        }
        if (startTime.equals(endTime)) {
            throw new IllegalArgumentException("Start time and end time cannot be identical");
        }
        this.startTime = startTime;
        this.endTime = endTime;
    }

    // Parses a formatted string like 20:00-06:00 into a TimeWindow object.
    public static TimeWindow parse(String value) {
        if (value == null || !value.contains("-")) {
            throw new IllegalArgumentException("Time window must follow format HH:mm-HH:mm, e.g., 20:00-06:00");
        }
        String[] parts = value.trim().split("-");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Time window must contain exactly one hyphen separator");
        }
        LocalTime start = LocalTime.parse(parts[0].trim(), FORMATTER);
        LocalTime end = LocalTime.parse(parts[1].trim(), FORMATTER);
        return new TimeWindow(start, end);
    }

    // Checks if a given time falls inside this window, handling midnight crossover.
    public boolean isWithinWindow(LocalTime checkTime) {
        if (checkTime == null) {
            return false;
        }
        if (startTime.isBefore(endTime)) {
            // Standard day window (e.g., 09:00 to 18:00)
            return !checkTime.isBefore(startTime) && checkTime.isBefore(endTime);
        } else {
            // Overnight window spanning midnight (e.g., 20:00 to 06:00)
            return !checkTime.isBefore(startTime) || checkTime.isBefore(endTime);
        }
    }

    // Returns whether this window spans past midnight into the next day.
    public boolean spansOvernight() {
        return startTime.isAfter(endTime);
    }

    // Returns the window start time.
    public LocalTime getStartTime() {
        return startTime;
    }

    // Returns the window end time.
    public LocalTime getEndTime() {
        return endTime;
    }

    // Formats the window as a human-readable HH:mm-HH:mm string.
    @Override
    public String toString() {
        return startTime.format(FORMATTER) + "-" + endTime.format(FORMATTER);
    }

    // Checks equality based on matching start and end times.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TimeWindow that)) return false;
        return Objects.equals(startTime, that.startTime) && Objects.equals(endTime, that.endTime);
    }

    // Generates a hash code for map storage and comparison.
    @Override
    public int hashCode() {
        return Objects.hash(startTime, endTime);
    }
}
