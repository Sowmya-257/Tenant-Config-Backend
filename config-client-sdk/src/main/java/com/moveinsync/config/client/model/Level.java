package com.moveinsync.config.client.model;

/**
 * Configuration hierarchy levels from lowest to highest specificity.
 */
public enum Level {
    GLOBAL(1),
    CLIENT(2),
    OFFICE(3),
    SHIFT(4);

    private final int specificity;

    // Sets the priority weight where higher number indicates more specific level.
    Level(int specificity) {
        this.specificity = specificity;
    }

    // Returns whether this level takes precedence over another level.
    public boolean isMoreSpecificThan(Level other) {
        return this.specificity > other.specificity;
    }

    // Returns the numeric specificity of this level.
    public int getSpecificity() {
        return specificity;
    }
}
