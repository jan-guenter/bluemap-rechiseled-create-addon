/*
 * SPDX-License-Identifier: MIT
 */
package io.github.janguenter.bluemap.rechiseledcreate.model;

import java.util.Locale;

/** Stable mask-bit order used by every exact Fusion sheet layout. */
public enum FusionDirection {
    TOP("top"),
    TOP_RIGHT("top_right"),
    RIGHT("right"),
    BOTTOM_RIGHT("bottom_right"),
    BOTTOM("bottom"),
    BOTTOM_LEFT("bottom_left"),
    LEFT("left"),
    TOP_LEFT("top_left");

    private final String wireName;

    FusionDirection(String wireName) {
        this.wireName = wireName;
    }

    public int bit() {
        return ordinal();
    }

    public String wireName() {
        return wireName;
    }

    public static FusionDirection parse(String value) {
        String normalized = value.toLowerCase(Locale.ROOT);
        for (FusionDirection direction : values()) {
            if (direction.wireName.equals(normalized)) {
                return direction;
            }
        }
        throw new IllegalArgumentException("unknown Fusion direction");
    }
}
