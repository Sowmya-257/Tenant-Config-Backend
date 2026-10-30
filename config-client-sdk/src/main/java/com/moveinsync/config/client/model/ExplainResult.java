package com.moveinsync.config.client.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;

/**
 * Explains how a setting value was resolved across the hierarchy levels.
 */
public class ExplainResult {

    private final String key;
    private final String resolvedValue;
    private final Level resolvedLevel;
    private final String author;
    private final Long version;
    private final List<LevelCandidate> evaluatedLevels;

    // Constructs an explanation of the hierarchical resolution process.
    @JsonCreator
    public ExplainResult(@JsonProperty("key") String key,
                         @JsonProperty("resolvedValue") String resolvedValue,
                         @JsonProperty("resolvedLevel") Level resolvedLevel,
                         @JsonProperty("author") String author,
                         @JsonProperty("version") Long version,
                         @JsonProperty("evaluatedLevels") List<LevelCandidate> evaluatedLevels) {
        this.key = key;
        this.resolvedValue = resolvedValue;
        this.resolvedLevel = resolvedLevel;
        this.author = author;
        this.version = version;
        this.evaluatedLevels = evaluatedLevels != null ? evaluatedLevels : Collections.emptyList();
    }

    // Returns the key that was explained.
    public String getKey() {
        return key;
    }

    // Returns the winning value chosen by the hierarchy.
    public String getResolvedValue() {
        return resolvedValue;
    }

    // Returns the level that won (SHIFT, OFFICE, CLIENT, or GLOBAL).
    public Level getResolvedLevel() {
        return resolvedLevel;
    }

    // Returns the author of the winning configuration.
    public String getAuthor() {
        return author;
    }

    // Returns the version of the winning configuration.
    public Long getVersion() {
        return version;
    }

    // Returns the list of all levels that were inspected during resolution.
    public List<LevelCandidate> getEvaluatedLevels() {
        return evaluatedLevels;
    }

    /**
     * Details about a specific level inspected during resolution.
     */
    public static class LevelCandidate {
        private final Level level;
        private final String scopeIdentifier;
        private final String value;
        private final boolean winning;

        // Records a candidate level checked during the resolution chain.
        @JsonCreator
        public LevelCandidate(@JsonProperty("level") Level level,
                              @JsonProperty("scopeIdentifier") String scopeIdentifier,
                              @JsonProperty("value") String value,
                              @JsonProperty("winning") boolean winning) {
            this.level = level;
            this.scopeIdentifier = scopeIdentifier;
            this.value = value;
            this.winning = winning;
        }

        // Returns the level of this candidate.
        public Level getLevel() {
            return level;
        }

        // Returns the scope identifier like office name or shift code.
        public String getScopeIdentifier() {
            return scopeIdentifier;
        }

        // Returns the setting value configured at this candidate level.
        public String getValue() {
            return value;
        }

        // Returns true if this level's value was selected as the winner.
        public boolean isWinning() {
            return winning;
        }
    }
}
