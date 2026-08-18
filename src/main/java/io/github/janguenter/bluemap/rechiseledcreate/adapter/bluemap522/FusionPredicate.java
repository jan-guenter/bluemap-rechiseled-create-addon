/*
 * SPDX-License-Identifier: MIT
 *
 * This bounded predicate tree is independently authored from the exact JSON
 * schema and observable behavior. It contains no Fusion implementation code.
 */
package io.github.janguenter.bluemap.rechiseledcreate.adapter.bluemap522;

import de.bluecolored.bluemap.core.world.BlockState;
import io.github.janguenter.bluemap.rechiseledcreate.model.FusionDirection;

import java.util.List;
import java.util.Map;
import java.util.Set;

/** Exact native-state predicate lane used by the Rechiseled: Create profile. */
sealed interface FusionPredicate permits FusionPredicate.Any, FusionPredicate.All,
        FusionPredicate.DirectionIn, FusionPredicate.MatchBlock,
        FusionPredicate.MatchState, FusionPredicate.SameState,
        FusionPredicate.Never {

    boolean test(BlockState own, BlockState neighbor, FusionDirection direction);

    record Any(List<FusionPredicate> predicates) implements FusionPredicate {
        public Any {
            predicates = List.copyOf(predicates);
        }

        @Override
        public boolean test(BlockState own, BlockState neighbor, FusionDirection direction) {
            for (FusionPredicate predicate : predicates) {
                if (predicate.test(own, neighbor, direction)) {
                    return true;
                }
            }
            return false;
        }
    }

    record All(List<FusionPredicate> predicates) implements FusionPredicate {
        public All {
            predicates = List.copyOf(predicates);
        }

        @Override
        public boolean test(BlockState own, BlockState neighbor, FusionDirection direction) {
            for (FusionPredicate predicate : predicates) {
                if (!predicate.test(own, neighbor, direction)) {
                    return false;
                }
            }
            return true;
        }
    }

    record DirectionIn(Set<FusionDirection> directions) implements FusionPredicate {
        public DirectionIn {
            directions = Set.copyOf(directions);
        }

        @Override
        public boolean test(BlockState own, BlockState neighbor, FusionDirection direction) {
            return directions.contains(direction);
        }
    }

    record MatchBlock(String blockId) implements FusionPredicate {
        @Override
        public boolean test(BlockState own, BlockState neighbor, FusionDirection direction) {
            return blockId.equals(neighbor.getId().getFormatted());
        }
    }

    record MatchState(String blockId, Map<String, Set<String>> properties)
            implements FusionPredicate {
        public MatchState {
            properties = Map.copyOf(properties);
        }

        @Override
        public boolean test(BlockState own, BlockState neighbor, FusionDirection direction) {
            if (!blockId.equals(neighbor.getId().getFormatted())) {
                return false;
            }
            for (Map.Entry<String, Set<String>> entry : properties.entrySet()) {
                if (!entry.getValue().contains(neighbor.getProperties().get(entry.getKey()))) {
                    return false;
                }
            }
            return true;
        }
    }

    record SameState() implements FusionPredicate {
        @Override
        public boolean test(BlockState own, BlockState neighbor, FusionDirection direction) {
            return !neighbor.isAir() && own.equals(neighbor);
        }
    }

    record Never() implements FusionPredicate {
        @Override
        public boolean test(BlockState own, BlockState neighbor, FusionDirection direction) {
            return false;
        }
    }
}
