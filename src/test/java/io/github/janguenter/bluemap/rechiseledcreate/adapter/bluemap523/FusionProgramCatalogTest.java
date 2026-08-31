/*
 * SPDX-License-Identifier: MIT
 */
package io.github.janguenter.bluemap.rechiseledcreate.adapter.bluemap523;

import de.bluecolored.bluemap.core.util.Key;
import io.github.janguenter.bluemap.rechiseledcreate.profile.RechiseledCreate111Fusion1312Profile;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FusionProgramCatalogTest {

    private static final Set<String> CUBE = Set.of(
            "down", "east", "north", "side", "south", "up", "west"
    );
    private static final Set<String> SHAPE = Set.of("bottom", "side", "top");
    private static final Set<String> HORIZONTAL_AXIS = Set.of(
            "all", "down", "east", "north", "side", "south", "up", "west"
    );

    @Test
    void parsesEveryExactCustomModelAndResolvesAllMaterialPredicates()
            throws IOException {
        Map<String, byte[]> models = exactModels(
                required("bridgeJar"), required("rechiseledJar")
        );
        FusionProgramCatalog catalog = FusionProgramCatalog.parse(models);
        assertEquals(432, catalog.size());

        Map<Set<String>, Long> keysets = catalog.programs().values().stream()
                .map(program -> program.connections().keySet())
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        assertEquals(135L, keysets.get(CUBE));
        assertEquals(224L, keysets.get(SHAPE));
        assertEquals(73L, keysets.get(HORIZONTAL_AXIS));

        for (FusionProgramCatalog.Program program : catalog.programs().values()) {
            assertFalse(program.textures().isEmpty());
            Set<String> materials = program.connections().keySet().equals(SHAPE)
                    ? SHAPE : Set.of("down", "east", "north", "south", "up", "west");
            for (String material : materials) {
                assertFalse(program.predicate(material) instanceof FusionPredicate.Never,
                        () -> "unresolved material " + material + " in " + program.model());
            }
        }
    }

    @Test
    void everyDirectVariantModelHasAnExactProgram() throws IOException {
        Path bridge = required("bridgeJar");
        FusionProgramCatalog catalog = FusionProgramCatalog.parse(exactModels(
                bridge, required("rechiseledJar")
        ));
        int selected = 0;
        Set<Key> unique = new HashSet<>();
        try (ZipFile zip = new ZipFile(bridge.toFile())) {
            for (String block : RechiseledCreate111Fusion1312Profile
                    .FUSION_ROUTED_BLOCKS) {
                String path = "assets/rechiseledcreate/blockstates/"
                        + block.substring(block.indexOf(':') + 1) + ".json";
                String json = new String(
                        zip.getInputStream(zip.getEntry(path)).readAllBytes(),
                        StandardCharsets.UTF_8
                );
                com.google.gson.JsonObject variants = com.google.gson.JsonParser.parseString(json)
                        .getAsJsonObject().getAsJsonObject("variants");
                for (Map.Entry<String, com.google.gson.JsonElement> entry
                        : variants.entrySet()) {
                    com.google.gson.JsonObject variant = entry.getValue().getAsJsonObject();
                    Key model = Key.parse(variant.get("model").getAsString());
                    assertNotNull(catalog.get(model), () -> "missing program for " + model);
                    unique.add(model);
                    selected++;
                }
            }
        }
        assertEquals(1_457, selected);
        assertEquals(432, unique.size());
    }

    private static Map<String, byte[]> exactModels(Path bridge, Path rechiseled)
            throws IOException {
        Map<String, byte[]> models = new HashMap<>();
        try (ZipFile bridgeZip = new ZipFile(bridge.toFile());
             ZipFile rechiseledZip = new ZipFile(rechiseled.toFile())) {
            RechiseledCreate111Fusion1312Profile.RESOURCES.entries().forEach((path, manifest) -> {
                if (!manifest.kind().equals("model")) {
                    return;
                }
                ZipFile zip = path.startsWith("assets/rechiseledcreate/")
                        ? bridgeZip : rechiseledZip;
                ZipEntry entry = zip.getEntry(path);
                assertNotNull(entry, path);
                try {
                    models.put(path, zip.getInputStream(entry).readAllBytes());
                } catch (IOException exception) {
                    throw new java.io.UncheckedIOException(exception);
                }
            });
        } catch (java.io.UncheckedIOException exception) {
            throw exception.getCause();
        }
        assertEquals(513, models.size());
        assertTrue(models.keySet().stream().allMatch(
                path -> path.startsWith("assets/rechiseledcreate/models/")
                        || path.startsWith("assets/rechiseled/models/")
        ));
        return models;
    }

    private static Path required(String property) {
        String value = System.getProperty(property);
        if (value == null || value.isBlank()) {
            throw new AssertionError("missing exact test artifact property: " + property);
        }
        return Path.of(value);
    }
}
