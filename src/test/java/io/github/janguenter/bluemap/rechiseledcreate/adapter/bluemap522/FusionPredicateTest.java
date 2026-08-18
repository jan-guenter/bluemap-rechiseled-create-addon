/*
 * SPDX-License-Identifier: MIT
 */
package io.github.janguenter.bluemap.rechiseledcreate.adapter.bluemap522;

import de.bluecolored.bluemap.core.util.Key;
import de.bluecolored.bluemap.core.world.BlockState;
import io.github.janguenter.bluemap.rechiseledcreate.model.FusionDirection;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FusionPredicateTest {

    private static final BlockState OWN = state(
            "rechiseledcreate:test_stairs_connecting",
            Map.of("facing", "east", "half", "bottom", "waterlogged", "false")
    );

    @Test
    void matchBlockIgnoresPropertiesAndMatchStateProjectsOnlyListedProperties() {
        BlockState neighbor = state(
                "rechiseledcreate:test_stairs_connecting",
                Map.of("facing", "west", "half", "bottom", "waterlogged", "true")
        );
        assertTrue(new FusionPredicate.MatchBlock(
                "rechiseledcreate:test_stairs_connecting"
        ).test(OWN, neighbor, FusionDirection.TOP));
        assertTrue(new FusionPredicate.MatchState(
                "rechiseledcreate:test_stairs_connecting", Map.of("half", Set.of("bottom"))
        ).test(OWN, neighbor, FusionDirection.TOP));
        assertFalse(new FusionPredicate.MatchState(
                "rechiseledcreate:test_stairs_connecting", Map.of("half", Set.of("top"))
        ).test(OWN, neighbor, FusionDirection.TOP));
    }

    @Test
    void sameStateIncludesEveryPersistedPropertyAndRejectsAir() {
        FusionPredicate predicate = new FusionPredicate.SameState();
        assertTrue(predicate.test(OWN, OWN, FusionDirection.RIGHT));
        assertFalse(predicate.test(OWN, state(
                "rechiseledcreate:test_stairs_connecting",
                Map.of("facing", "east", "half", "bottom", "waterlogged", "true")
        ), FusionDirection.RIGHT));
        assertFalse(predicate.test(OWN, BlockState.AIR, FusionDirection.RIGHT));
    }

    @Test
    void directionAndBooleanNodesUseOrdinaryListedOrderSemantics() {
        FusionPredicate direction = new FusionPredicate.DirectionIn(
                Set.of(FusionDirection.TOP, FusionDirection.TOP_LEFT)
        );
        FusionPredicate block = new FusionPredicate.MatchBlock(
                "rechiseledcreate:test_stairs_connecting"
        );
        assertTrue(new FusionPredicate.All(List.of(direction, block))
                .test(OWN, OWN, FusionDirection.TOP));
        assertFalse(new FusionPredicate.All(List.of(direction, block))
                .test(OWN, OWN, FusionDirection.BOTTOM));
        assertTrue(new FusionPredicate.Any(List.of(new FusionPredicate.Never(), block))
                .test(OWN, OWN, FusionDirection.BOTTOM));
        assertFalse(new FusionPredicate.Any(List.of())
                .test(OWN, OWN, FusionDirection.BOTTOM));
        assertTrue(new FusionPredicate.All(List.of())
                .test(OWN, OWN, FusionDirection.BOTTOM));
    }

    private static BlockState state(String id, Map<String, String> properties) {
        return new BlockState(Key.parse(id), properties);
    }
}
