/* SPDX-License-Identifier: MIT */
package io.github.janguenter.bluemap.rechiseledcreate.adapter.bluemap523;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/** Frozen item/generated extrusion derived from the installed chisel sprite alpha. */
final class ChiselSpriteGeometry {

    private static final int SIZE = 16;
    private static final float FRONT_Z = 8.5F / 16F;
    private static final float BACK_Z = 7.5F / 16F;

    private ChiselSpriteGeometry() {
    }

    static List<Triangle> triangles(BufferedImage image) {
        if (image == null || image.getWidth() != SIZE || image.getHeight() != SIZE) {
            return List.of();
        }
        ArrayList<Triangle> result = new ArrayList<>();
        quad(result,
                vertex(0F, 0F, FRONT_Z, 0F, 1F),
                vertex(1F, 0F, FRONT_Z, 1F, 1F),
                vertex(1F, 1F, FRONT_Z, 1F, 0F),
                vertex(0F, 1F, FRONT_Z, 0F, 0F));
        quad(result,
                vertex(1F, 0F, BACK_Z, 1F, 1F),
                vertex(0F, 0F, BACK_Z, 0F, 1F),
                vertex(0F, 1F, BACK_Z, 0F, 0F),
                vertex(1F, 1F, BACK_Z, 1F, 0F));

        List<Span> spans = spans(image);
        for (Span span : spans) {
            addSpan(result, span);
        }
        return List.copyOf(result);
    }

    /** Mirrors ItemModelGenerator#getSpans, including its across-gap merging. */
    private static List<Span> spans(BufferedImage image) {
        ArrayList<Span> spans = new ArrayList<>();
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                if (!opaque(image, x, y)) {
                    continue;
                }
                checkTransition(spans, image, SpanFacing.UP, x, y);
                checkTransition(spans, image, SpanFacing.DOWN, x, y);
                checkTransition(spans, image, SpanFacing.LEFT, x, y);
                checkTransition(spans, image, SpanFacing.RIGHT, x, y);
            }
        }
        return spans;
    }

    private static void checkTransition(
            List<Span> spans,
            BufferedImage image,
            SpanFacing facing,
            int x,
            int y
    ) {
        if (!opaque(image, x + facing.xOffset, y + facing.yOffset)) {
            int anchor = facing.horizontal ? y : x;
            int coordinate = facing.horizontal ? x : y;
            for (int index = 0; index < spans.size(); index++) {
                Span current = spans.get(index);
                if (current.facing == facing && current.anchor == anchor) {
                    spans.set(index, current.expand(coordinate));
                    return;
                }
            }
            spans.add(new Span(facing, coordinate, coordinate, anchor));
        }
    }

    private static void addSpan(List<Triangle> output, Span span) {
        float rangeMin = span.min / 16F;
        float rangeMax = (span.max + 1F) / 16F;
        float anchorMin = span.anchor / 16F;
        float anchorMax = (span.anchor + 1F) / 16F;
        switch (span.facing) {
            case UP -> {
                float modelY = 1F - anchorMin;
                quad(output,
                        vertex(rangeMin, modelY, FRONT_Z, rangeMin, anchorMax),
                        vertex(rangeMax, modelY, FRONT_Z, rangeMax, anchorMax),
                        vertex(rangeMax, modelY, BACK_Z, rangeMax, anchorMin),
                        vertex(rangeMin, modelY, BACK_Z, rangeMin, anchorMin));
            }
            case DOWN -> {
                float modelY = 1F - anchorMax;
                quad(output,
                        vertex(rangeMin, modelY, BACK_Z, rangeMin, anchorMax),
                        vertex(rangeMax, modelY, BACK_Z, rangeMax, anchorMax),
                        vertex(rangeMax, modelY, FRONT_Z, rangeMax, anchorMin),
                        vertex(rangeMin, modelY, FRONT_Z, rangeMin, anchorMin));
            }
            case LEFT -> {
                float modelX = anchorMin;
                float maxY = 1F - rangeMin;
                float minY = 1F - rangeMax;
                quad(output,
                        vertex(modelX, minY, BACK_Z, anchorMax, rangeMax),
                        vertex(modelX, minY, FRONT_Z, anchorMin, rangeMax),
                        vertex(modelX, maxY, FRONT_Z, anchorMin, rangeMin),
                        vertex(modelX, maxY, BACK_Z, anchorMax, rangeMin));
            }
            case RIGHT -> {
                float modelX = anchorMax;
                float maxY = 1F - rangeMin;
                float minY = 1F - rangeMax;
                quad(output,
                        vertex(modelX, minY, FRONT_Z, anchorMax, rangeMax),
                        vertex(modelX, minY, BACK_Z, anchorMin, rangeMax),
                        vertex(modelX, maxY, BACK_Z, anchorMin, rangeMin),
                        vertex(modelX, maxY, FRONT_Z, anchorMax, rangeMin));
            }
        }
    }

    private static boolean opaque(BufferedImage image, int x, int y) {
        return x >= 0 && y >= 0 && x < SIZE && y < SIZE
                && (image.getRGB(x, y) >>> 24) != 0;
    }

    private static Vertex vertex(float x, float y, float z, float u, float v) {
        return new Vertex(x, y, z, u, v);
    }

    private static void quad(
            List<Triangle> output,
            Vertex first,
            Vertex second,
            Vertex third,
            Vertex fourth
    ) {
        output.add(new Triangle(first, second, third));
        output.add(new Triangle(first, third, fourth));
    }

    record Vertex(float x, float y, float z, float u, float v) {
    }

    record Triangle(Vertex first, Vertex second, Vertex third) {
    }

    private record Span(SpanFacing facing, int min, int max, int anchor) {
        private Span expand(int coordinate) {
            return new Span(facing, Math.min(min, coordinate),
                    Math.max(max, coordinate), anchor);
        }
    }

    private enum SpanFacing {
        UP(0, -1, true),
        DOWN(0, 1, true),
        LEFT(-1, 0, false),
        RIGHT(1, 0, false);

        private final int xOffset;
        private final int yOffset;
        private final boolean horizontal;

        SpanFacing(int xOffset, int yOffset, boolean horizontal) {
            this.xOffset = xOffset;
            this.yOffset = yOffset;
            this.horizontal = horizontal;
        }
    }
}
