/*
 * SPDX-License-Identifier: MIT
 */
package io.github.janguenter.bluemap.rechiseledcreate.model;

import de.bluecolored.bluemap.core.util.Direction;

import java.util.Arrays;

/** Eight possible orientations of an orthogonal texture on a final model face. */
public enum TextureOrientation {
    N0(0, 1, 2, 3, 4, 5, 6, 7),
    N90(6, 7, 0, 1, 2, 3, 4, 5),
    N180(4, 5, 6, 7, 0, 1, 2, 3),
    N270(2, 3, 4, 5, 6, 7, 0, 1),
    F0(6, 5, 4, 3, 2, 1, 0, 7),
    F90(0, 7, 6, 5, 4, 3, 2, 1),
    F180(2, 1, 0, 7, 6, 5, 4, 3),
    F270(4, 3, 2, 1, 0, 7, 6, 5);

    private final int[] directionMap;

    TextureOrientation(int... directionMap) {
        this.directionMap = directionMap;
    }

    public FusionDirection predicateDirection(FusionDirection maskDirection) {
        return FusionDirection.values()[directionMap[maskDirection.bit()]];
    }

    public static Frame classify(Direction face, AxisVector up, AxisVector right) {
        AxisVector baseUp = baseUp(face);
        AxisVector baseRight = baseRight(face);
        AxisVector[] candidatesUp = {
            baseUp, baseRight, baseUp.negate(), baseRight.negate(),
            baseRight.negate(), baseUp.negate(), baseRight, baseUp
        };
        AxisVector[] candidatesRight = {
            baseRight, baseUp.negate(), baseRight.negate(), baseUp,
            baseUp.negate(), baseRight, baseUp, baseRight.negate()
        };
        TextureOrientation[] orientations = values();
        for (int index = 0; index < orientations.length; index++) {
            if (candidatesUp[index].equals(up) && candidatesRight[index].equals(right)) {
                return new Frame(face, up, right, orientations[index]);
            }
        }
        throw new IllegalArgumentException(
                "texture axes do not form a supported orientation: "
                        + face + " " + Arrays.asList(up, right)
        );
    }

    public static AxisVector baseUp(Direction face) {
        return switch (face) {
            case DOWN -> new AxisVector(0, 0, 1);
            case UP -> new AxisVector(0, 0, -1);
            case NORTH, SOUTH, WEST, EAST -> new AxisVector(0, 1, 0);
        };
    }

    public static AxisVector baseRight(Direction face) {
        return switch (face) {
            case DOWN, UP, SOUTH -> new AxisVector(1, 0, 0);
            case NORTH -> new AxisVector(-1, 0, 0);
            case WEST -> new AxisVector(0, 0, 1);
            case EAST -> new AxisVector(0, 0, -1);
        };
    }

    /** Final world face and its actual increasing-texture axes. */
    public record Frame(
            Direction face,
            AxisVector up,
            AxisVector right,
            TextureOrientation orientation
    ) {

        public AxisVector offset(FusionDirection direction) {
            return switch (direction) {
                case TOP -> up;
                case TOP_RIGHT -> up.add(right);
                case RIGHT -> right;
                case BOTTOM_RIGHT -> right.subtract(up);
                case BOTTOM -> up.negate();
                case BOTTOM_LEFT -> up.negate().subtract(right);
                case LEFT -> right.negate();
                case TOP_LEFT -> up.subtract(right);
            };
        }

        public FusionDirection predicateDirection(FusionDirection maskDirection) {
            return orientation.predicateDirection(maskDirection);
        }
    }
}
