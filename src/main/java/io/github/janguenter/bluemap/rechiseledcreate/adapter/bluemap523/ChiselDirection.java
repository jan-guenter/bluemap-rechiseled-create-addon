/* SPDX-License-Identifier: MIT */
package io.github.janguenter.bluemap.rechiseledcreate.adapter.bluemap523;

import java.util.Locale;
import java.util.Optional;

/** Dependency-free direction subset for the mechanical-chisel contract. */
enum ChiselDirection {
    DOWN(Axis.Y),
    UP(Axis.Y),
    NORTH(Axis.Z),
    SOUTH(Axis.Z),
    WEST(Axis.X),
    EAST(Axis.X);

    private final Axis axis;

    ChiselDirection(Axis axis) {
        this.axis = axis;
    }

    Axis axis() {
        return axis;
    }

    boolean horizontal() {
        return axis != Axis.Y;
    }

    ChiselDirection opposite() {
        return switch (this) {
            case DOWN -> UP;
            case UP -> DOWN;
            case NORTH -> SOUTH;
            case SOUTH -> NORTH;
            case WEST -> EAST;
            case EAST -> WEST;
        };
    }

    float horizontalAngle() {
        return switch (this) {
            case SOUTH, UP, DOWN -> 0F;
            case NORTH -> 180F;
            case WEST -> -90F;
            case EAST -> 90F;
        };
    }

    float verticalAngle() {
        return switch (this) {
            case UP -> -90F;
            case DOWN -> 90F;
            default -> 0F;
        };
    }

    static Optional<ChiselDirection> parse(String value) {
        if (value == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(valueOf(value.toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    enum Axis {
        X,
        Y,
        Z
    }
}
