/*
 * SPDX-License-Identifier: MIT
 */
package io.github.janguenter.bluemap.rechiseledcreate.model;

import io.github.janguenter.bluemap.rechiseledcreate.profile.TextureLayout;

/** Independently authored selectors for the exact five connected-sheet layouts. */
public final class FusionTextureSelector {

    private static final int[] SIMPLE = {0, 7, 6, 14, 2, 5, 10, 8, 3, 15, 4, 12, 11, 13, 9, 1};
    private static final int[] PIECED_CORNERS = {0, 3, 2, 4, 0, 3, 2, 1};

    private FusionTextureSelector() {
    }

    public static int tile(TextureLayout layout, int mask) {
        if ((mask & ~0xff) != 0) {
            throw new IllegalArgumentException("connection mask must be one byte");
        }
        return switch (layout) {
            case PLAIN -> 0;
            case HORIZONTAL -> horizontal(mask);
            case VERTICAL -> vertical(mask);
            case SIMPLE -> simple(mask);
            case FULL -> full(mask);
            case PIECED -> piecedShortcut(mask);
        };
    }

    public static int piecedCorner(int mask, FusionDirection corner) {
        int index = switch (corner) {
            case TOP_LEFT -> bit(mask, FusionDirection.LEFT)
                    | bit(mask, FusionDirection.TOP) << 1
                    | bit(mask, FusionDirection.TOP_LEFT) << 2;
            case TOP_RIGHT -> bit(mask, FusionDirection.RIGHT)
                    | bit(mask, FusionDirection.TOP) << 1
                    | bit(mask, FusionDirection.TOP_RIGHT) << 2;
            case BOTTOM_LEFT -> bit(mask, FusionDirection.LEFT)
                    | bit(mask, FusionDirection.BOTTOM) << 1
                    | bit(mask, FusionDirection.BOTTOM_LEFT) << 2;
            case BOTTOM_RIGHT -> bit(mask, FusionDirection.RIGHT)
                    | bit(mask, FusionDirection.BOTTOM) << 1
                    | bit(mask, FusionDirection.BOTTOM_RIGHT) << 2;
            default -> throw new IllegalArgumentException("not a pieced corner");
        };
        return PIECED_CORNERS[index];
    }

    private static int horizontal(int mask) {
        boolean left = set(mask, FusionDirection.LEFT);
        boolean right = set(mask, FusionDirection.RIGHT);
        return left ? right ? 2 : 3 : right ? 1 : 0;
    }

    private static int vertical(int mask) {
        boolean top = set(mask, FusionDirection.TOP);
        boolean bottom = set(mask, FusionDirection.BOTTOM);
        return top ? bottom ? 2 : 3 : bottom ? 1 : 0;
    }

    private static int simple(int mask) {
        int cardinal = bit(mask, FusionDirection.TOP)
                | bit(mask, FusionDirection.RIGHT) << 1
                | bit(mask, FusionDirection.BOTTOM) << 2
                | bit(mask, FusionDirection.LEFT) << 3;
        return SIMPLE[cardinal];
    }

    private static int piecedShortcut(int mask) {
        int cardinals = cardinalCount(mask);
        if (cardinals == 0) {
            return 0;
        }
        if (mask == 0xff) {
            return 1;
        }
        if (cardinals == 2 && set(mask, FusionDirection.TOP)
                && set(mask, FusionDirection.BOTTOM)) {
            return 2;
        }
        if (cardinals == 2 && set(mask, FusionDirection.LEFT)
                && set(mask, FusionDirection.RIGHT)) {
            return 3;
        }
        if (cardinals == 4 && diagonalCount(mask) == 0) {
            return 4;
        }
        return -1;
    }

    private static int full(int mask) {
        boolean top = set(mask, FusionDirection.TOP);
        boolean right = set(mask, FusionDirection.RIGHT);
        boolean bottom = set(mask, FusionDirection.BOTTOM);
        boolean left = set(mask, FusionDirection.LEFT);
        boolean topRight = set(mask, FusionDirection.TOP_RIGHT);
        boolean bottomRight = set(mask, FusionDirection.BOTTOM_RIGHT);
        boolean bottomLeft = set(mask, FusionDirection.BOTTOM_LEFT);
        boolean topLeft = set(mask, FusionDirection.TOP_LEFT);
        int count = cardinalCount(mask);
        if (count == 0) {
            return 0;
        }
        if (count == 1) {
            if (left) {
                return 3;
            }
            if (top) {
                return 24;
            }
            if (right) {
                return 1;
            }
            return 8;
        }
        if (count == 2) {
            if (left && right) {
                return 2;
            }
            if (top && bottom) {
                return 16;
            }
            if (left && top) {
                return topLeft ? 27 : 13;
            }
            if (top && right) {
                return topRight ? 25 : 12;
            }
            if (right && bottom) {
                return bottomRight ? 9 : 4;
            }
            return bottomLeft ? 11 : 5;
        }
        if (count == 3) {
            if (!left) {
                return pair(topRight, bottomRight, 17, 20, 22, 6);
            }
            if (!top) {
                return pair(bottomLeft, bottomRight, 10, 23, 21, 7);
            }
            if (!right) {
                return pair(topLeft, bottomLeft, 19, 31, 29, 15);
            }
            return pair(topLeft, topRight, 26, 28, 30, 14);
        }
        int diagonals = diagonalCount(mask);
        if (diagonals == 4) {
            return 18;
        }
        if (diagonals == 3) {
            if (!topLeft) {
                return 47;
            }
            if (!topRight) {
                return 46;
            }
            if (!bottomLeft) {
                return 39;
            }
            return 38;
        }
        if (diagonals == 2) {
            if (topRight && bottomLeft) {
                return 32;
            }
            if (topLeft && bottomRight) {
                return 40;
            }
            if (bottomRight && bottomLeft) {
                return 35;
            }
            if (topLeft && bottomLeft) {
                return 43;
            }
            if (topLeft && topRight) {
                return 42;
            }
            return 34;
        }
        if (diagonals == 1) {
            if (topLeft) {
                return 45;
            }
            if (topRight) {
                return 44;
            }
            if (bottomRight) {
                return 36;
            }
            return 37;
        }
        return 33;
    }

    private static int pair(boolean first, boolean second, int both, int onlyFirst,
                            int onlySecond, int neither) {
        return first ? second ? both : onlyFirst : second ? onlySecond : neither;
    }

    private static int cardinalCount(int mask) {
        return bit(mask, FusionDirection.TOP) + bit(mask, FusionDirection.RIGHT)
                + bit(mask, FusionDirection.BOTTOM) + bit(mask, FusionDirection.LEFT);
    }

    private static int diagonalCount(int mask) {
        return bit(mask, FusionDirection.TOP_RIGHT)
                + bit(mask, FusionDirection.BOTTOM_RIGHT)
                + bit(mask, FusionDirection.BOTTOM_LEFT)
                + bit(mask, FusionDirection.TOP_LEFT);
    }

    private static int bit(int mask, FusionDirection direction) {
        return set(mask, direction) ? 1 : 0;
    }

    private static boolean set(int mask, FusionDirection direction) {
        return (mask & 1 << direction.bit()) != 0;
    }
}
