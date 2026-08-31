/*
 * SPDX-License-Identifier: MIT
 */
package io.github.janguenter.bluemap.rechiseledcreate.adapter.bluemap523;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MechanicalChiselRenderPlanTest {

    private static final float EPSILON = 0.000_01F;

    @Test
    void selectsAllTwentyFourExactStatesAndOnlyVerticalTools() {
        int states = 0;
        int tools = 0;
        for (String facing : new String[]{
            "down", "up", "north", "south", "west", "east"
        }) {
            for (String along : new String[]{"false", "true"}) {
                for (String flipped : new String[]{"false", "true"}) {
                    MechanicalChiselRenderPlan plan = plan(facing, along, flipped);
                    assertTrue(plan.shaftTransform().finite());
                    if (plan.toolTransform().isPresent()) {
                        tools++;
                        assertTrue(plan.toolTransform().orElseThrow().finite());
                    }
                    states++;
                }
            }
        }
        assertEquals(24, states);
        assertEquals(8, tools);
    }

    @Test
    void horizontalStatesUseTheOppositeFacingHalfShaftAndNoTool() {
        MechanicalChiselRenderPlan north = plan("north", "false", "true");
        assertEquals("create:block/shaft_half", north.shaftModel());
        assertFalse(north.toolTransform().isPresent());
        assertPoint(north.shaftTransform().transform(0.2F, 0.3F, 0.4F),
                0.2F, 0.3F, 0.4F);

        MechanicalChiselRenderPlan east = plan("east", "true", "false");
        assertEquals("create:block/shaft_half", east.shaftModel());
        assertPoint(east.shaftTransform().transform(0.5F, 0.5F, 1F),
                0F, 0.5F, 0.5F);
    }

    @Test
    void verticalStatesUseTheExactDirectionalAxisShaftTransforms() {
        MechanicalChiselRenderPlan alongFirst = plan("up", "true", "false");
        MechanicalChiselRenderPlan alongSecond = plan("down", "false", "true");
        assertEquals("create:block/shaft", alongFirst.shaftModel());
        assertEquals("create:block/shaft", alongSecond.shaftModel());
        assertPoint(alongFirst.shaftTransform().transform(0.5F, 1F, 0.5F),
                1F, 0.5F, 0.5F);
        assertPoint(alongSecond.shaftTransform().transform(0.5F, 1F, 0.5F),
                0.5F, 0.5F, 1F);
    }

    @Test
    void locksNeutralToolPoseForAxisFlipAndDownBranches() {
        assertPoint(plan("up", "false", "false").toolTransform().orElseThrow()
                        .transform(0F, 0F, 0F),
                0.75F, 0.85F, 0F);
        assertPoint(plan("up", "true", "false").toolTransform().orElseThrow()
                        .transform(0F, 0F, 0F),
                0F, 0.85F, 0.25F);
        assertPoint(plan("up", "false", "true").toolTransform().orElseThrow()
                        .transform(0F, 0F, 0F),
                0.25F, 0.85F, 1F);
        assertPoint(plan("down", "false", "false").toolTransform().orElseThrow()
                        .transform(0F, 0F, 0F),
                0.75F, 0.15F, 1F);
    }

    @Test
    void malformedOrPartialStatesFailClosed() {
        assertTrue(MechanicalChiselRenderPlan.select(Map.of()).isEmpty());
        assertTrue(MechanicalChiselRenderPlan.select(Map.of(
                "facing", "sideways",
                "axis_along_first", "true",
                "flipped", "false"
        )).isEmpty());
        assertTrue(MechanicalChiselRenderPlan.select(Map.of(
                "facing", "up",
                "axis_along_first", "yes",
                "flipped", "false"
        )).isEmpty());
    }

    private static MechanicalChiselRenderPlan plan(
            String facing,
            String along,
            String flipped
    ) {
        return MechanicalChiselRenderPlan.select(Map.of(
                "facing", facing,
                "axis_along_first", along,
                "flipped", flipped
        )).orElseThrow();
    }

    private static void assertPoint(
            AffineTransform.Point point,
            float x,
            float y,
            float z
    ) {
        assertEquals(x, point.x(), EPSILON);
        assertEquals(y, point.y(), EPSILON);
        assertEquals(z, point.z(), EPSILON);
    }
}
