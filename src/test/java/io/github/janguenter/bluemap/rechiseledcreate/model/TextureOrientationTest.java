/*
 * SPDX-License-Identifier: MIT
 */
package io.github.janguenter.bluemap.rechiseledcreate.model;

import de.bluecolored.bluemap.core.util.Direction;
import io.github.janguenter.bluemap.resource.fusion.model.AxisVector;
import io.github.janguenter.bluemap.resource.fusion.model.FusionDirection;
import io.github.janguenter.bluemap.resource.fusion.model.TextureOrientation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TextureOrientationTest {

    @Test
    void classifiesAllEightFramesOnEveryFinalFace() {
        for (Direction face : Direction.values()) {
            AxisVector up = TextureOrientation.baseUp(face);
            AxisVector right = TextureOrientation.baseRight(face);
            assertFrame(face, up, right, TextureOrientation.N0);
            assertFrame(face, right, up.negate(), TextureOrientation.N90);
            assertFrame(face, up.negate(), right.negate(), TextureOrientation.N180);
            assertFrame(face, right.negate(), up, TextureOrientation.N270);
            assertFrame(face, right.negate(), up.negate(), TextureOrientation.F0);
            assertFrame(face, up.negate(), right, TextureOrientation.F90);
            assertFrame(face, right, up, TextureOrientation.F180);
            assertFrame(face, up, right.negate(), TextureOrientation.F270);
        }
    }

    @Test
    void locksWorldNeighborOffsetsAndPredicateRemapping() {
        TextureOrientation.Frame frame = TextureOrientation.classify(
                Direction.UP, new AxisVector(0, 0, -1), new AxisVector(1, 0, 0)
        );
        assertEquals(new AxisVector(0, 0, -1), frame.offset(FusionDirection.TOP));
        assertEquals(new AxisVector(1, 0, -1), frame.offset(FusionDirection.TOP_RIGHT));
        assertEquals(new AxisVector(-1, 0, 1), frame.offset(FusionDirection.BOTTOM_LEFT));
        for (FusionDirection direction : FusionDirection.values()) {
            assertEquals(direction, frame.predicateDirection(direction));
        }
        TextureOrientation.Frame rotated = TextureOrientation.classify(
                Direction.UP, new AxisVector(1, 0, 0), new AxisVector(0, 0, 1)
        );
        assertEquals(TextureOrientation.N90, rotated.orientation());
        assertEquals(FusionDirection.LEFT,
                rotated.predicateDirection(FusionDirection.TOP));
    }

    private static void assertFrame(
            Direction face,
            AxisVector up,
            AxisVector right,
            TextureOrientation expected
    ) {
        assertEquals(expected, TextureOrientation.classify(face, up, right).orientation());
    }
}
