/*
 * SPDX-License-Identifier: MIT
 */
package io.github.janguenter.bluemap.rechiseledcreate.adapter.bluemap522;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChiselSpriteGeometryTest {

    private static final float EPSILON = 0.000_01F;

    @Test
    void exactInstalledSpriteProducesTheFrozenGeneratedItemExtrusion()
            throws IOException {
        BufferedImage image = exactChiselTexture();
        List<ChiselSpriteGeometry.Triangle> triangles =
                ChiselSpriteGeometry.triangles(image);
        assertEquals(108, triangles.size());

        for (ChiselSpriteGeometry.Triangle triangle : triangles) {
            for (ChiselSpriteGeometry.Vertex vertex : vertices(triangle)) {
                assertTrue(vertex.x() >= 0F && vertex.x() <= 1F);
                assertTrue(vertex.y() >= 0F && vertex.y() <= 1F);
                assertTrue(vertex.z() >= 7.5F / 16F
                        && vertex.z() <= 8.5F / 16F);
                assertTrue(vertex.u() >= 0F && vertex.u() <= 1F);
                assertTrue(vertex.v() >= 0F && vertex.v() <= 1F);
            }
            assertTrue(normalLengthSquared(triangle) > EPSILON);
        }
        assertTrue(normalZ(triangles.get(0)) > 0F);
        assertTrue(normalZ(triangles.get(2)) < 0F);
    }

    @Test
    void singlePixelUsesExactGeneratedItemFaceUvsAndWinding() {
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(3, 5, 0xFFFFFFFF);

        List<ChiselSpriteGeometry.Triangle> triangles =
                ChiselSpriteGeometry.triangles(image);
        assertEquals(12, triangles.size());

        float x0 = 3F / 16F;
        float x1 = 4F / 16F;
        float y0 = 10F / 16F;
        float y1 = 11F / 16F;
        float u0 = 3F / 16F;
        float u1 = 4F / 16F;
        float v0 = 5F / 16F;
        float v1 = 6F / 16F;

        assertQuad(triangles, 0,
                vertex(0F, 0F, 8.5F / 16F, 0F, 1F),
                vertex(1F, 0F, 8.5F / 16F, 1F, 1F),
                vertex(1F, 1F, 8.5F / 16F, 1F, 0F),
                vertex(0F, 1F, 8.5F / 16F, 0F, 0F));
        assertQuad(triangles, 2,
                vertex(1F, 0F, 7.5F / 16F, 1F, 1F),
                vertex(0F, 0F, 7.5F / 16F, 0F, 1F),
                vertex(0F, 1F, 7.5F / 16F, 0F, 0F),
                vertex(1F, 1F, 7.5F / 16F, 1F, 0F));
        assertQuad(triangles, 4,
                vertex(x0, y1, 8.5F / 16F, u0, v1),
                vertex(x1, y1, 8.5F / 16F, u1, v1),
                vertex(x1, y1, 7.5F / 16F, u1, v0),
                vertex(x0, y1, 7.5F / 16F, u0, v0));
        assertQuad(triangles, 6,
                vertex(x0, y0, 7.5F / 16F, u0, v1),
                vertex(x1, y0, 7.5F / 16F, u1, v1),
                vertex(x1, y0, 8.5F / 16F, u1, v0),
                vertex(x0, y0, 8.5F / 16F, u0, v0));
        assertQuad(triangles, 8,
                vertex(x0, y0, 7.5F / 16F, u1, v1),
                vertex(x0, y0, 8.5F / 16F, u0, v1),
                vertex(x0, y1, 8.5F / 16F, u0, v0),
                vertex(x0, y1, 7.5F / 16F, u1, v0));
        assertQuad(triangles, 10,
                vertex(x1, y0, 8.5F / 16F, u1, v1),
                vertex(x1, y0, 7.5F / 16F, u0, v1),
                vertex(x1, y1, 7.5F / 16F, u0, v0),
                vertex(x1, y1, 8.5F / 16F, u1, v0));
    }

    @Test
    void wrongDimensionsAndMissingImagesFailClosed() {
        assertTrue(ChiselSpriteGeometry.triangles(null).isEmpty());
        assertTrue(ChiselSpriteGeometry.triangles(
                new BufferedImage(15, 16, BufferedImage.TYPE_INT_ARGB)
        ).isEmpty());
        assertFalse(ChiselSpriteGeometry.triangles(
                new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB)
        ).isEmpty());
    }

    private static BufferedImage exactChiselTexture() throws IOException {
        Path jar = required("rechiseledJar");
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            ZipEntry entry = zip.getEntry("assets/rechiseled/textures/item/chisel.png");
            if (entry == null) {
                throw new AssertionError("exact chisel texture is missing");
            }
            try (InputStream input = zip.getInputStream(entry)) {
                BufferedImage image = ImageIO.read(input);
                if (image == null) {
                    throw new AssertionError("exact chisel texture is unreadable");
                }
                return image;
            }
        }
    }

    private static List<ChiselSpriteGeometry.Vertex> vertices(
            ChiselSpriteGeometry.Triangle triangle
    ) {
        return List.of(triangle.first(), triangle.second(), triangle.third());
    }

    private static ChiselSpriteGeometry.Vertex vertex(
            float x,
            float y,
            float z,
            float u,
            float v
    ) {
        return new ChiselSpriteGeometry.Vertex(x, y, z, u, v);
    }

    private static void assertQuad(
            List<ChiselSpriteGeometry.Triangle> triangles,
            int firstTriangle,
            ChiselSpriteGeometry.Vertex first,
            ChiselSpriteGeometry.Vertex second,
            ChiselSpriteGeometry.Vertex third,
            ChiselSpriteGeometry.Vertex fourth
    ) {
        assertTriangle(triangles.get(firstTriangle), first, second, third);
        assertTriangle(triangles.get(firstTriangle + 1), first, third, fourth);
    }

    private static void assertTriangle(
            ChiselSpriteGeometry.Triangle actual,
            ChiselSpriteGeometry.Vertex first,
            ChiselSpriteGeometry.Vertex second,
            ChiselSpriteGeometry.Vertex third
    ) {
        assertVertex(first, actual.first());
        assertVertex(second, actual.second());
        assertVertex(third, actual.third());
    }

    private static void assertVertex(
            ChiselSpriteGeometry.Vertex expected,
            ChiselSpriteGeometry.Vertex actual
    ) {
        assertEquals(expected.x(), actual.x(), EPSILON);
        assertEquals(expected.y(), actual.y(), EPSILON);
        assertEquals(expected.z(), actual.z(), EPSILON);
        assertEquals(expected.u(), actual.u(), EPSILON);
        assertEquals(expected.v(), actual.v(), EPSILON);
    }

    private static float normalLengthSquared(ChiselSpriteGeometry.Triangle triangle) {
        ChiselSpriteGeometry.Vertex a = triangle.first();
        ChiselSpriteGeometry.Vertex b = triangle.second();
        ChiselSpriteGeometry.Vertex c = triangle.third();
        float abx = b.x() - a.x();
        float aby = b.y() - a.y();
        float abz = b.z() - a.z();
        float acx = c.x() - a.x();
        float acy = c.y() - a.y();
        float acz = c.z() - a.z();
        float x = aby * acz - abz * acy;
        float y = abz * acx - abx * acz;
        float z = abx * acy - aby * acx;
        return x * x + y * y + z * z;
    }

    private static float normalZ(ChiselSpriteGeometry.Triangle triangle) {
        ChiselSpriteGeometry.Vertex a = triangle.first();
        ChiselSpriteGeometry.Vertex b = triangle.second();
        ChiselSpriteGeometry.Vertex c = triangle.third();
        return (b.x() - a.x()) * (c.y() - a.y())
                - (b.y() - a.y()) * (c.x() - a.x());
    }

    private static Path required(String property) {
        String value = System.getProperty(property);
        if (value == null || value.isBlank()) {
            throw new AssertionError("missing exact test artifact property: " + property);
        }
        return Path.of(value);
    }
}
