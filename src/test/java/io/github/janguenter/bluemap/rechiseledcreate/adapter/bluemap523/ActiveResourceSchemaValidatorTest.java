/*
 * SPDX-License-Identifier: MIT
 */
package io.github.janguenter.bluemap.rechiseledcreate.adapter.bluemap523;

import io.github.janguenter.bluemap.rechiseledcreate.profile.TextureCatalog;
import io.github.janguenter.bluemap.rechiseledcreate.profile.TextureLayout;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActiveResourceSchemaValidatorTest {

    private static final TextureCatalog.Entry EXPECTED = new TextureCatalog.Entry(
            TextureLayout.SIMPLE, 64, 64, "0".repeat(64)
    );

    @Test
    void acceptsPixelOverridesButRejectsLayoutDimensionChanges(@TempDir Path temporary)
            throws IOException {
        Path changedPixels = temporary.resolve("changed.png");
        write(changedPixels, 64, 64, 0xff12ab34);
        assertTrue(ActiveResourceSchemaValidator.validTexture(changedPixels, EXPECTED));

        Path wrongDimensions = temporary.resolve("wrong.png");
        write(wrongDimensions, 63, 64, 0xff12ab34);
        assertFalse(ActiveResourceSchemaValidator.validTexture(wrongDimensions, EXPECTED));
    }

    @Test
    void validatesImagesThroughAnOpenZipFileSystem(@TempDir Path temporary)
            throws IOException {
        Path zip = temporary.resolve("resources.zip");
        URI uri = URI.create("jar:" + zip.toUri());
        try (FileSystem fileSystem = FileSystems.newFileSystem(uri, Map.of("create", "true"))) {
            Path image = fileSystem.getPath(
                    "/assets/rechiseledcreate/textures/block/example.png"
            );
            Files.createDirectories(image.getParent());
            write(image, 64, 64, 0xffabcdef);
            assertTrue(ActiveResourceSchemaValidator.validTexture(image, EXPECTED));
        }
    }

    @Test
    void alphaBearingOverrideTilesFailSafeOutOfFullCubeCulling() throws IOException {
        KeyAndTexture opaque = texture(16, 16, 0xffffffff);
        KeyAndTexture transparent = texture(16, 16, 0x00123456);
        assertTrue(RechiseledCreateResourceExtension.opaqueTexture(opaque.texture()));
        assertFalse(RechiseledCreateResourceExtension.opaqueTexture(transparent.texture()));
    }

    @Test
    void doubleSlabsUseTheOpaqueFullCubePropertyLane() {
        de.bluecolored.bluemap.core.world.BlockState doubled =
                new de.bluecolored.bluemap.core.world.BlockState(
                        de.bluecolored.bluemap.core.util.Key.parse(
                                "rechiseledcreate:test_slab_connecting"
                        ),
                        Map.of("type", "double")
                );
        de.bluecolored.bluemap.core.world.BlockState bottom =
                new de.bluecolored.bluemap.core.world.BlockState(
                        de.bluecolored.bluemap.core.util.Key.parse(
                                "rechiseledcreate:test_slab_connecting"
                        ),
                        Map.of("type", "bottom")
                );
        assertTrue(RechiseledCreateResourceExtension.isDoubleSlab(doubled));
        assertFalse(RechiseledCreateResourceExtension.isDoubleSlab(bottom));
    }

    @Test
    void mechanicalChiselExplicitlyDisablesSyntheticMissingCubeOcclusion() {
        de.bluecolored.bluemap.core.world.BlockState chisel =
                new de.bluecolored.bluemap.core.world.BlockState(
                        de.bluecolored.bluemap.core.util.Key.parse(
                                "rechiseledcreate:mechanical_chisel"
                        ),
                        Map.of(
                                "facing", "up",
                                "axis_along_first", "false",
                                "flipped", "false"
                        )
                );
        de.bluecolored.bluemap.core.world.BlockProperties.Builder builder =
                de.bluecolored.bluemap.core.world.BlockProperties.builder();
        assertTrue(RechiseledCreateResourceExtension.applyMechanicalChiselProperties(
                chisel, builder
        ));
        assertEquals(de.bluecolored.bluemap.core.util.Tristate.FALSE,
                builder.isCulling());
        assertEquals(de.bluecolored.bluemap.core.util.Tristate.FALSE,
                builder.isOccluding());
        assertEquals(de.bluecolored.bluemap.core.util.Tristate.FALSE,
                builder.isCullingIdentical());

        assertFalse(RechiseledCreateResourceExtension.applyMechanicalChiselProperties(
                new de.bluecolored.bluemap.core.world.BlockState(
                        de.bluecolored.bluemap.core.util.Key.parse(
                                "rechiseledcreate:rose_quartz_bricks"
                        ),
                        Map.of()
                ),
                de.bluecolored.bluemap.core.world.BlockProperties.builder()
        ));
    }

    @Test
    void fullOpacitySkipsOnlyTheProvenUnreachableTile41() {
        TextureCatalog.Entry full = new TextureCatalog.Entry(
                TextureLayout.FULL, 128, 128, "0".repeat(64)
        );
        assertTrue(RechiseledCreateResourceExtension.reachableTile(full, 40));
        assertFalse(RechiseledCreateResourceExtension.reachableTile(full, 41));
        assertTrue(RechiseledCreateResourceExtension.reachableTile(full, 47));
    }

    @Test
    void bakedAtlasRemapsMustPreserveExactSheetDimensions() {
        TextureCatalog.Entry entry = new TextureCatalog.Entry(
                TextureLayout.SIMPLE, 64, 64, "0".repeat(64)
        );
        assertTrue(RechiseledCreateResourceExtension.validBakedSheetDimensions(
                new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB), entry
        ));
        assertFalse(RechiseledCreateResourceExtension.validBakedSheetDimensions(
                new BufferedImage(128, 64, BufferedImage.TYPE_INT_ARGB), entry
        ));
    }

    @Test
    void synthesisSkipsPlainTexturesAndCropsOnlyAtlasTiles() {
        assertEquals(3_056, RechiseledCreateResourceExtension.syntheticTileCount());
        de.bluecolored.bluemap.core.util.Key plain =
                io.github.janguenter.bluemap.rechiseledcreate.profile
                        .RechiseledCreate111Fusion1312Profile.TEXTURES.entries()
                        .entrySet().stream()
                        .filter(entry -> entry.getValue().layout() == TextureLayout.PLAIN)
                        .map(Map.Entry::getKey)
                        .findFirst()
                        .orElseThrow();
        RechiseledCreateResourceExtension extension =
                new RechiseledCreateResourceExtension(null, null);
        assertEquals(plain, extension.tile(plain, 0));
        assertNull(extension.tile(plain, 1));
    }

    @Test
    void chiselItemModelChainRequiresExactParentsTextureAndFixedTransform() {
        byte[] item = json("""
                {"parent":"minecraft:item/handheld",
                 "textures":{"layer0":"rechiseled:item/chisel"}}
                """);
        byte[] handheld = json("{\"parent\":\"item/generated\"}");
        byte[] generated = json("""
                {"parent":"builtin/generated","display":{"fixed":{
                 "rotation":[0,180,0],"scale":[1,1,1]}}}
                """);
        assertTrue(ActiveResourceSchemaValidator.validChiselModelChain(
                item, handheld, generated
        ));
        assertFalse(ActiveResourceSchemaValidator.validChiselModelChain(
                item,
                json("{\"parent\":\"item/handheld\"}"),
                generated
        ));
        assertFalse(ActiveResourceSchemaValidator.validChiselModelChain(
                item,
                handheld,
                json("""
                        {"parent":"builtin/generated","display":{"fixed":{
                         "rotation":[0,0,0],"scale":[1,1,1]}}}
                        """)
        ));
    }

    private static KeyAndTexture texture(int width, int height, int color)
            throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(new java.awt.Color(color, true));
            graphics.fillRect(0, 0, width, height);
        } finally {
            graphics.dispose();
        }
        de.bluecolored.bluemap.core.util.Key key =
                de.bluecolored.bluemap.core.util.Key.parse("test:texture");
        return new KeyAndTexture(
                key,
                de.bluecolored.bluemap.core.resources.pack.resourcepack.texture.Texture.from(
                        key, image
                )
        );
    }

    private record KeyAndTexture(
            de.bluecolored.bluemap.core.util.Key key,
            de.bluecolored.bluemap.core.resources.pack.resourcepack.texture.Texture texture
    ) {
    }

    private static void write(Path path, int width, int height, int color) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(width / 2, height / 2, color);
        try (java.io.OutputStream output = Files.newOutputStream(path)) {
            ImageIO.write(image, "png", output);
        }
    }

    private static byte[] json(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }
}
