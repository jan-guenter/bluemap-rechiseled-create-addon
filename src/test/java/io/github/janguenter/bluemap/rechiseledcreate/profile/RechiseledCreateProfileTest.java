/*
 * SPDX-License-Identifier: MIT
 */
package io.github.janguenter.bluemap.rechiseledcreate.profile;

import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RechiseledCreateProfileTest {

    @Test
    void locksRouteAndLegalStateCensus() {
        Map<ShapeFamily, Long> connected = RechiseledCreate111Fusion1312Profile.DEFINITIONS.values()
                .stream()
                .collect(Collectors.groupingBy(
                        RechiseledCreateDefinition::shape,
                        () -> new EnumMap<>(ShapeFamily.class),
                        Collectors.counting()
                ));
        assertEquals(Map.of(
                ShapeFamily.FULL, 34L,
                ShapeFamily.AXIS, 73L,
                ShapeFamily.SLAB, 28L,
                ShapeFamily.STAIRS, 28L
        ), connected);
        Map<ShapeFamily, Long> disconnected = RechiseledCreate111Fusion1312Profile
                .DISCONNECTED_DEFINITIONS.values().stream()
                .collect(Collectors.groupingBy(
                        RechiseledCreateDefinition::shape,
                        () -> new EnumMap<>(ShapeFamily.class),
                        Collectors.counting()
                ));
        assertEquals(Map.of(
                ShapeFamily.FULL, 5L,
                ShapeFamily.AXIS, 73L
        ), disconnected);
        int connectedStates = RechiseledCreate111Fusion1312Profile.DEFINITIONS.values()
                .stream().mapToInt(RechiseledCreateDefinition::legalStates).sum();
        int disconnectedStates = RechiseledCreate111Fusion1312Profile
                .DISCONNECTED_DEFINITIONS.values().stream()
                .mapToInt(RechiseledCreateDefinition::legalStates).sum();
        assertEquals(2_661, connectedStates);
        assertEquals(224, disconnectedStates);
        assertEquals(242, RechiseledCreate111Fusion1312Profile.ROUTED_BLOCKS.size());
        assertEquals(163, RechiseledCreate111Fusion1312Profile.FUSION_ROUTED_BLOCKS.size());
        assertEquals(78,
                RechiseledCreate111Fusion1312Profile.FORCED_DISCONNECTED_BLOCKS.size());
        assertTrue(RechiseledCreate111Fusion1312Profile.ROUTED_BLOCKS.stream()
                .allMatch(id -> id.startsWith("rechiseledcreate:")));
        assertTrue(RechiseledCreate111Fusion1312Profile.FUSION_ROUTED_BLOCKS.stream()
                .allMatch(id -> id.endsWith("_connecting")));
        assertTrue(RechiseledCreate111Fusion1312Profile.FORCED_DISCONNECTED_BLOCKS.stream()
                .noneMatch(id -> id.endsWith("_connecting")));
        assertTrue(RechiseledCreate111Fusion1312Profile.ROUTED_BLOCKS.contains(
                RechiseledCreate111Fusion1312Profile.MECHANICAL_CHISEL_ID
        ));
        assertFalse(RechiseledCreate111Fusion1312Profile.ROUTED_BLOCKS.stream()
                .anyMatch(id -> id.startsWith("fusion:")));
    }

    @Test
    void locksInstalledResourceClosureAndLayoutCensus() {
        Map<String, Long> resources = RechiseledCreate111Fusion1312Profile.RESOURCES.entries()
                .values().stream()
                .collect(Collectors.groupingBy(
                        ResourceManifest.Entry::kind, Collectors.counting()
                ));
        assertEquals(Map.of(
                "blockstate", 241L,
                "model", 513L,
                "texture", 180L,
                "metadata", 107L
        ), resources);
        Map<TextureLayout, Long> layouts = RechiseledCreate111Fusion1312Profile.TEXTURES.entries()
                .values().stream()
                .collect(Collectors.groupingBy(
                        TextureCatalog.Entry::layout, Collectors.counting()
                ));
        assertEquals(Map.of(
                TextureLayout.PLAIN, 73L,
                TextureLayout.FULL, 42L,
                TextureLayout.SIMPLE, 65L
        ), layouts);
        assertEquals(6, TextureLayout.FULL.rows());
        assertEquals(8, TextureLayout.FULL.physicalRows());
        assertEquals(9, RechiseledCreate111Fusion1312Profile.HOST_MODELS.entries().size());
        assertTrue(RechiseledCreate111Fusion1312Profile.HOST_MODELS.entries().keySet().stream()
                .allMatch(path -> path.startsWith("assets/minecraft/models/")));
        assertTrue(RechiseledCreate111Fusion1312Profile.HOST_MODELS.entries().containsKey(
                "assets/minecraft/models/block/block.json"
        ));
        assertTrue(RechiseledCreate111Fusion1312Profile.HOST_MODELS.entries().containsKey(
                "assets/minecraft/models/item/handheld.json"
        ));
        assertTrue(RechiseledCreate111Fusion1312Profile.HOST_MODELS.entries().containsKey(
                "assets/minecraft/models/item/generated.json"
        ));
        RechiseledCreate111Fusion1312Profile.TEXTURES.entries().forEach((key, entry) -> {
            assertTrue(key.getFormatted().startsWith("rechiseledcreate:block/"));
            assertEquals(16, entry.width() / entry.layout().columns());
            assertEquals(16, entry.height() / entry.layout().physicalRows());
        });
        assertEquals(16,
                RechiseledCreate111Fusion1312Profile.CHISEL_RESOURCES.entries().size());
        assertEquals(9,
                RechiseledCreate111Fusion1312Profile.CHISEL_TEXTURES.entries().size());
        assertTrue(RechiseledCreate111Fusion1312Profile.CHISEL_TEXTURES.entries()
                .containsKey(de.bluecolored.bluemap.core.util.Key.parse(
                        "rechiseled:item/chisel"
                )));
        assertTrue(RechiseledCreate111Fusion1312Profile.CHISEL_RESOURCES.entries()
                .containsKey("assets/rechiseled/models/item/chisel.json"));
    }
}
