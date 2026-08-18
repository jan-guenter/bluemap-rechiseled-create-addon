/*
 * SPDX-License-Identifier: MIT
 */
package io.github.janguenter.bluemap.rechiseledcreate.model;

/** An exact signed world-axis vector used by the connected-texture frame. */
public record AxisVector(int x, int y, int z) {

    public AxisVector {
        if (Math.abs(x) + Math.abs(y) + Math.abs(z) > 2) {
            throw new IllegalArgumentException("axis-vector components are out of range");
        }
    }

    public AxisVector add(AxisVector other) {
        return new AxisVector(x + other.x, y + other.y, z + other.z);
    }

    public AxisVector negate() {
        return new AxisVector(-x, -y, -z);
    }

    public AxisVector subtract(AxisVector other) {
        return add(other.negate());
    }
}
