/*
 * SPDX-License-Identifier: MIT
 */
package io.github.janguenter.bluemap.rechiseledcreate.profile;

import java.util.Locale;

/** Stable geometry families in the exact routed Rechiseled: Create roster. */
public enum ShapeFamily {
    FULL("full", 1),
    AXIS("axis", 3),
    SLAB("slab", 6),
    STAIRS("stairs", 80);

    private final String wireName;
    private final int legalStates;

    ShapeFamily(String wireName, int legalStates) {
        this.wireName = wireName;
        this.legalStates = legalStates;
    }

    public String wireName() {
        return wireName;
    }

    public int legalStates() {
        return legalStates;
    }

    public static ShapeFamily parse(String value) {
        for (ShapeFamily family : values()) {
            if (family.wireName.equals(value.toLowerCase(Locale.ROOT))) {
                return family;
            }
        }
        throw new IllegalArgumentException("unknown Rechiseled: Create shape family");
    }
}
