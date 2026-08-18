/*
 * SPDX-License-Identifier: MIT
 */
package io.github.janguenter.bluemap.rechiseledcreate.profile;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.bluecolored.bluemap.core.util.Key;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForcedDisconnectedOpacityContractTest {

    @Test
    void exactTileZeroOpacitySeparatesQuartzCubesFromWindowPillars()
            throws IOException {
        Set<String> opaque = new HashSet<>();
        Set<String> transparent = new HashSet<>();
        try (ZipFile bridge = new ZipFile(required("bridgeJar").toFile())) {
            for (Map.Entry<String, RechiseledCreateDefinition> definition
                    : RechiseledCreate111Fusion1312Profile
                            .DISCONNECTED_DEFINITIONS.entrySet()) {
                boolean allFacesOpaque = allSelectedModelTexturesOpaque(
                        bridge, definition.getKey()
                );
                if (allFacesOpaque) {
                    opaque.add(definition.getKey());
                } else {
                    transparent.add(definition.getKey());
                }
            }
        }
        assertEquals(Set.of(
                "rechiseledcreate:rose_quartz_bricks",
                "rechiseledcreate:rose_quartz_chiseled",
                "rechiseledcreate:rose_quartz_crushed",
                "rechiseledcreate:rose_quartz_polished_block",
                "rechiseledcreate:rose_quartz_squares"
        ), opaque);
        assertEquals(73, transparent.size());
        assertTrue(transparent.stream().allMatch(id -> id.contains("_window_")));
    }

    private static boolean allSelectedModelTexturesOpaque(
            ZipFile bridge,
            String blockId
    ) throws IOException {
        String block = blockId.substring(blockId.indexOf(':') + 1);
        JsonObject variants = object(bridge,
                "assets/rechiseledcreate/blockstates/" + block + ".json")
                .getAsJsonObject("variants");
        assertNotNull(variants);
        boolean opaque = true;
        for (Map.Entry<String, JsonElement> variant : variants.entrySet()) {
            JsonElement selected = variant.getValue();
            String model = selected.getAsJsonObject().get("model").getAsString();
            String value = model.substring(model.indexOf(':') + 1);
            JsonObject textures = object(bridge,
                    "assets/rechiseledcreate/models/" + value + ".json")
                    .getAsJsonObject("textures");
            assertNotNull(textures);
            for (Map.Entry<String, JsonElement> textureEntry : textures.entrySet()) {
                JsonElement textureValue = textureEntry.getValue();
                String texture = textureValue.getAsString();
                if (texture.startsWith("#")) {
                    continue;
                }
                Key key = Key.parse(texture);
                TextureCatalog.Entry catalog =
                        RechiseledCreate111Fusion1312Profile.TEXTURES.get(key);
                assertNotNull(catalog, texture);
                opaque &= tileZeroOpaque(bridge, key, catalog);
            }
        }
        return opaque;
    }

    private static boolean tileZeroOpaque(
            ZipFile bridge,
            Key key,
            TextureCatalog.Entry catalog
    ) throws IOException {
        String path = "assets/" + key.getNamespace() + "/textures/"
                + key.getValue() + ".png";
        ZipEntry entry = bridge.getEntry(path);
        assertNotNull(entry, path);
        BufferedImage image;
        try (InputStream input = bridge.getInputStream(entry)) {
            image = ImageIO.read(input);
        }
        assertNotNull(image, path);
        int width = catalog.width() / catalog.layout().columns();
        int height = catalog.height() / catalog.layout().physicalRows();
        assertEquals(16, width);
        assertEquals(16, height);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (image.getRGB(x, y) >>> 24 != 255) {
                    return false;
                }
            }
        }
        return true;
    }

    private static JsonObject object(ZipFile zip, String path) throws IOException {
        ZipEntry entry = zip.getEntry(path);
        assertNotNull(entry, path);
        String json;
        try (InputStream input = zip.getInputStream(entry)) {
            json = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
        return JsonParser.parseString(json).getAsJsonObject();
    }

    private static Path required(String property) {
        String value = System.getProperty(property);
        if (value == null || value.isBlank()) {
            throw new AssertionError("missing exact test artifact property: " + property);
        }
        return Path.of(value);
    }
}
