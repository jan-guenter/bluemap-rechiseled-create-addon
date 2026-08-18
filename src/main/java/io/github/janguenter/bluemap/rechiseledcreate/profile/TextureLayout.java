/*
 * SPDX-License-Identifier: MIT
 */
package io.github.janguenter.bluemap.rechiseledcreate.profile;

/** Exact Fusion connected-sheet layouts present in the profile. */
public enum TextureLayout {
    PLAIN("plain", 1, 1, 1),
    PIECED("pieced", 5, 1, 1),
    FULL("full", 8, 6, 8),
    HORIZONTAL("horizontal", 4, 1, 1),
    VERTICAL("vertical", 1, 4, 4),
    SIMPLE("simple", 4, 4, 4);

    private final String wireName;
    private final int columns;
    private final int rows;
    private final int physicalRows;

    TextureLayout(String wireName, int columns, int rows, int physicalRows) {
        this.wireName = wireName;
        this.columns = columns;
        this.rows = rows;
        this.physicalRows = physicalRows;
    }

    public int columns() {
        return columns;
    }

    public int rows() {
        return rows;
    }

    public int physicalRows() {
        return physicalRows;
    }

    public static TextureLayout parse(String value) {
        for (TextureLayout layout : values()) {
            if (layout.wireName.equals(value)) {
                return layout;
            }
        }
        throw new IllegalArgumentException("unknown Fusion texture layout");
    }
}
