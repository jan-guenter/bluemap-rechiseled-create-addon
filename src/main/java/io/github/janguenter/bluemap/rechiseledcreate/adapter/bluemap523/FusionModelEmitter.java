/*
 * This file includes mechanics adapted from BlueMap, licensed under the MIT
 * License.
 *
 * Copyright (c) Blue (Lukas Rieger) <https://bluecolored.de>
 * Copyright (c) BlueMap contributors
 * Copyright (c) 2026 Jan Guenter and BlueMap Rechiseled Create Add-on contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * Geometry, lighting, UV-lock, AO, and model-selection mechanics are adapted
 * from BlueMap 5.22's MIT ResourceModelRenderer. Fusion program evaluation,
 * sheet selection, and UV clipping are independently authored from the exact
 * installed schema and observable behavior. See docs/PROVENANCE.md.
 */
package io.github.janguenter.bluemap.rechiseledcreate.adapter.bluemap523;

import com.flowpowered.math.TrigMath;
import com.flowpowered.math.vector.Vector3f;
import com.flowpowered.math.vector.Vector4f;
import de.bluecolored.bluemap.core.map.TextureGallery;
import de.bluecolored.bluemap.core.map.hires.RenderSettings;
import de.bluecolored.bluemap.core.map.hires.TileModel;
import de.bluecolored.bluemap.core.map.hires.TileModelView;
import de.bluecolored.bluemap.core.map.hires.block.color.BlockColorCalculator;
import de.bluecolored.bluemap.core.resources.ResourcePath;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.ResourcePack;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.blockstate.Variant;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.model.Element;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.model.Face;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.model.Model;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.texture.Texture;
import de.bluecolored.bluemap.core.util.Direction;
import de.bluecolored.bluemap.core.util.Key;
import de.bluecolored.bluemap.core.util.math.Color;
import de.bluecolored.bluemap.core.util.math.MatrixM4f;
import de.bluecolored.bluemap.core.util.math.VectorM3f;
import de.bluecolored.bluemap.core.world.BlockProperties;
import de.bluecolored.bluemap.core.world.BlockState;
import de.bluecolored.bluemap.core.world.LightData;
import de.bluecolored.bluemap.core.world.block.BlockNeighborhood;
import de.bluecolored.bluemap.core.world.block.ExtendedBlock;
import io.github.janguenter.bluemap.resource.fusion.model.AxisVector;
import io.github.janguenter.bluemap.resource.fusion.model.FusionDirection;
import io.github.janguenter.bluemap.resource.fusion.model.FusionTextureLayout;
import io.github.janguenter.bluemap.resource.fusion.model.FusionTextureSelector;
import io.github.janguenter.bluemap.resource.fusion.model.TextureOrientation;
import io.github.janguenter.bluemap.rechiseledcreate.profile.RechiseledCreate111Fusion1312Profile;
import io.github.janguenter.bluemap.rechiseledcreate.profile.TextureCatalog;
import io.github.janguenter.bluemap.rechiseledcreate.profile.TextureLayout;

import java.util.ArrayList;
import java.util.List;

/** Emits original BlueMap model geometry with exact connected-sheet substitution. */
final class FusionModelEmitter {

    private static final float BLOCK_SCALE = 1F / 16F;
    private static final float EPSILON = 0.0001F;
    private static final float[][] PIECED_BOUNDS = {
        {0F, 0.5F, 0F, 0.5F},
        {0.5F, 1F, 0F, 0.5F},
        {0.5F, 1F, 0.5F, 1F},
        {0F, 0.5F, 0.5F, 1F}
    };

    private final ResourcePack resourcePack;
    private final TextureGallery textureGallery;
    private final RenderSettings renderSettings;
    private final BlockColorCalculator blockColorCalculator;
    private final RechiseledCreateResourceExtension extension;
    private final Color tintColor = new Color();
    private final Color sampledColor = new Color();

    private BlockNeighborhood block;
    private Variant variant;
    private Model model;
    private FusionProgramCatalog.Program program;
    private boolean forcedDisconnected;
    private TileModelView target;
    private Color mapColor;
    private float mapColorOpacity;

    FusionModelEmitter(
            ResourcePack resourcePack,
            TextureGallery textureGallery,
            RenderSettings renderSettings
    ) {
        this.resourcePack = resourcePack;
        this.textureGallery = textureGallery;
        this.renderSettings = renderSettings;
        this.blockColorCalculator = resourcePack.createBlockColorCalculator();
        this.extension = BlueMap523Adapter.extension(resourcePack);
    }

    boolean render(
            BlockNeighborhood currentBlock,
            Variant currentVariant,
            TileModelView output,
            Color outputMapColor,
            FusionProgramCatalog catalog
    ) {
        return render(currentBlock, currentVariant, output, outputMapColor, catalog, false);
    }

    boolean renderDisconnected(
            BlockNeighborhood currentBlock,
            Variant currentVariant,
            TileModelView output,
            Color outputMapColor
    ) {
        return render(currentBlock, currentVariant, output, outputMapColor, null, true);
    }

    private boolean render(
            BlockNeighborhood currentBlock,
            Variant currentVariant,
            TileModelView output,
            Color outputMapColor,
            FusionProgramCatalog catalog,
            boolean disconnected
    ) {
        block = currentBlock;
        variant = currentVariant;
        target = output;
        mapColor = outputMapColor;
        model = variant.getModel().getResource(resourcePack.getModels()::get);
        program = catalog == null ? null : catalog.get(variant.getModel());
        forcedDisconnected = disconnected;
        if (model == null || (!forcedDisconnected && program == null) || extension == null) {
            return false;
        }
        Element[] elements = model.getElements();
        if (elements == null) {
            return false;
        }
        mapColorOpacity = 0F;
        tintColor.set(0F, 0F, 0F, -1F, true);
        int modelStart = target.getStart();
        for (Element element : elements) {
            if (element == null || !emitElement(element)) {
                return false;
            }
        }
        if (mapColor.a > 0F) {
            mapColor.flatten().straight();
            mapColor.a = mapColorOpacity;
        }
        target.initialize(modelStart);
        if (block.getProperties().isRandomOffset()) {
            float dx = (hashToFloat(block.getX(), block.getZ(), 123984) - 0.5F) * 0.75F;
            float dz = (hashToFloat(block.getX(), block.getZ(), 345542) - 0.5F) * 0.75F;
            target.translate(dx, 0F, dz);
        }
        return true;
    }

    private boolean emitElement(Element element) {
        Vector3f from = element.getFrom();
        Vector3f to = element.getTo();
        Vertex[] corners = corners(from, to);
        return emitFace(element, Direction.DOWN, corners[0], corners[2], corners[3], corners[1])
                && emitFace(element, Direction.UP, corners[5], corners[7], corners[6], corners[4])
                && emitFace(element, Direction.NORTH, corners[2], corners[0], corners[4], corners[6])
                && emitFace(element, Direction.SOUTH, corners[1], corners[3], corners[7], corners[5])
                && emitFace(element, Direction.WEST, corners[0], corners[1], corners[5], corners[4])
                && emitFace(element, Direction.EAST, corners[3], corners[2], corners[6], corners[7]);
    }

    private boolean emitFace(
            Element element,
            Direction localDirection,
            Vertex first,
            Vertex second,
            Vertex third,
            Vertex fourth
    ) {
        Face face = element.getFaces().get(localDirection);
        if (face == null) {
            return true;
        }
        Vertex[] vertices = {
            first.copy(), second.copy(), third.copy(), fourth.copy()
        };
        assignUvs(face, localDirection, vertices);
        transformPositions(element, vertices);
        Direction finalDirection = transformedDirection(localDirection, element.getRotation().getMatrix());
        TextureOrientation.Frame frame = textureFrame(finalDirection, vertices);

        if (renderSettings.isRenderTopOnly() && normal(finalDirection).y() < 0.01F) {
            return true;
        }
        if (face.getCullface() != null && culled(transformedDirection(
                face.getCullface(), new MatrixM4f()
        ))) {
            return true;
        }
        LightSample light = light(finalDirection, element.getLightEmission());
        if (block.isRemoveIfCave() && (renderSettings.isCaveDetectionUsesBlockLight()
                ? Math.max(light.sunlight(), light.blocklight()) : light.sunlight()) == 0) {
            return true;
        }

        String materialKey = face.getTexture().getReferenceName();
        ResourcePath<Texture> sourcePath = face.getTexture().getTexturePath(model.getTextures()::get);
        if (materialKey == null || sourcePath == null) {
            return false;
        }
        TextureCatalog.Entry texture = RechiseledCreate111Fusion1312Profile.TEXTURES.get(sourcePath);
        if (texture == null) {
            return false;
        }
        int mask = forcedDisconnected ? 0 : connections(program.predicate(materialKey), frame);
        int tile = selectedTile(texture.layout(), mask, forcedDisconnected);
        float[] ao = ambientOcclusion(element, localDirection, vertices);
        if (texture.layout() == TextureLayout.PIECED && tile < 0) {
            return emitPieced(vertices, ao, mask, frame, light, face);
        }
        Key output = extension.tile(sourcePath, tile);
        return output != null && emitPolygon(List.of(vertices), ao, output, light, face, 1F);
    }

    static int selectedTile(
            TextureLayout layout,
            int mask,
            boolean disconnected
    ) {
        return FusionTextureSelector.tile(
                FusionTextureLayout.valueOf(layout.name()), disconnected ? 0 : mask
        );
    }

    private boolean emitPieced(
            Vertex[] vertices,
            float[] ao,
            int mask,
            TextureOrientation.Frame frame,
            LightSample light,
            Face face
    ) {
        FusionDirection[] corners = {
            FusionDirection.TOP_LEFT,
            FusionDirection.TOP_RIGHT,
            FusionDirection.BOTTOM_RIGHT,
            FusionDirection.BOTTOM_LEFT
        };
        ResourcePath<Texture> source = face.getTexture().getTexturePath(model.getTextures()::get);
        float totalArea = uvArea(List.of(vertices));
        if (totalArea < EPSILON) {
            return false;
        }
        for (int index = 0; index < corners.length; index++) {
            List<Vertex> clipped = clip(List.of(vertices), PIECED_BOUNDS[index]);
            int selected = FusionTextureSelector.piecedCorner(mask, corners[index]);
            Key output = extension.tile(source, selected);
            if (!nonDegenerate(clipped)) {
                continue;
            }
            float area = uvArea(clipped);
            if (output == null || !emitPolygon(
                    clipped, ao, output, light, face, area / totalArea
            )) {
                return false;
            }
        }
        return true;
    }

    private boolean emitPolygon(
            List<Vertex> polygon,
            float[] originalAo,
            Key textureKey,
            LightSample light,
            Face face,
            float colorWeight
    ) {
        if (polygon.size() < 3) {
            return true;
        }
        Texture texture = resourcePack.getTextures().get(textureKey);
        if (texture == null) {
            return false;
        }
        TileModel mesh = target.getTileModel();
        int material = textureGallery.get(textureKey);
        int start = target.add(polygon.size() - 2);
        Vertex anchor = polygon.get(0);
        for (int triangle = 0; triangle < polygon.size() - 2; triangle++) {
            Vertex b = polygon.get(triangle + 1);
            Vertex c = polygon.get(triangle + 2);
            int index = start + triangle;
            mesh.setPositions(index,
                    anchor.x, anchor.y, anchor.z,
                    b.x, b.y, b.z,
                    c.x, c.y, c.z);
            mesh.setUvs(index, anchor.u, anchor.v, b.u, b.v, c.u, c.v);
            mesh.setMaterialIndex(index, material);
            setTint(mesh, index, face);
            mesh.setSunlight(index, light.sunlight());
            mesh.setBlocklight(index, light.blocklight());
            mesh.setAOs(index, interpolatedAo(anchor, originalAo),
                    interpolatedAo(b, originalAo), interpolatedAo(c, originalAo));
        }
        if (finalNormal(polygon).y() > 0.01F) {
            accumulateMapColor(texture, colorWeight, light, face);
        }
        return true;
    }

    private void setTint(TileModel mesh, int triangle, Face face) {
        if (face.getTintindex() >= 0) {
            if (tintColor.a < 0F) {
                blockColorCalculator.getBlockColor(block, tintColor);
            }
            mesh.setColor(triangle, tintColor.r, tintColor.g, tintColor.b);
        } else {
            mesh.setColor(triangle, 1F, 1F, 1F);
        }
    }

    private void accumulateMapColor(
            Texture texture,
            float weight,
            LightSample light,
            Face face
    ) {
        sampledColor.set(texture.getColorPremultiplied());
        if (face.getTintindex() >= 0 && tintColor.a >= 0F) {
            sampledColor.multiply(tintColor);
        }
        float combined = Math.max(light.sunlight(), light.blocklight()) / 15F;
        combined = (1F - renderSettings.getAmbientLight()) * combined
                + renderSettings.getAmbientLight();
        sampledColor.r *= combined * weight;
        sampledColor.g *= combined * weight;
        sampledColor.b *= combined * weight;
        sampledColor.a *= weight;
        mapColorOpacity = Math.max(mapColorOpacity, texture.getColorPremultiplied().a);
        mapColor.add(sampledColor);
    }

    private int connections(FusionPredicate predicate, TextureOrientation.Frame frame) {
        int mask = 0;
        BlockState own = block.getBlockState();
        for (FusionDirection direction : FusionDirection.values()) {
            AxisVector offset = frame.offset(direction);
            BlockState neighbor = block.getNeighborBlock(offset.x(), offset.y(), offset.z())
                    .getBlockState();
            FusionDirection predicateDirection = frame.predicateDirection(direction);
            if (predicate.test(own, neighbor, predicateDirection)) {
                mask |= 1 << direction.bit();
            }
        }
        return mask;
    }

    private boolean culled(Direction direction) {
        AxisVector axis = normal(direction);
        ExtendedBlock neighbor = block.getNeighborBlock(axis.x(), axis.y(), axis.z());
        BlockProperties properties = neighbor.getProperties();
        return properties.isCulling() || properties.getCullingIdentical()
                && neighbor.getBlockState().equals(block.getBlockState());
    }

    private LightSample light(Direction direction, int emission) {
        AxisVector axis = normal(direction);
        LightData own = block.getLightData();
        LightData neighbor = block.getNeighborBlock(axis.x(), axis.y(), axis.z()).getLightData();
        return new LightSample(
                Math.max(own.getSkyLight(), neighbor.getSkyLight()),
                Math.max(emission, Math.max(own.getBlockLight(), neighbor.getBlockLight()))
        );
    }

    private float[] ambientOcclusion(
            Element element,
            Direction localDirection,
            Vertex[] vertices
    ) {
        float[] result = {1F, 1F, 1F, 1F};
        if (!model.isAmbientocclusion()) {
            return result;
        }
        Direction finalDirection = transformedDirection(
                localDirection, element.getRotation().getMatrix()
        );
        for (int index = 0; index < vertices.length; index++) {
            result[index] = testAo(vertices[index], finalDirection);
            vertices[index].source = index;
            vertices[index].ao = result[index];
        }
        return result;
    }

    private float testAo(Vertex vertex, Direction direction) {
        AxisVector face = normal(direction);
        int x = boundary(vertex.x);
        int y = boundary(vertex.y);
        int z = boundary(vertex.z);
        int occluding = 0;
        if (x * face.x() + y * face.y() > 0
                && block.getNeighborBlock(x, y, 0).getProperties().isOccluding()) {
            occluding++;
        }
        if (x * face.x() + z * face.z() > 0
                && block.getNeighborBlock(x, 0, z).getProperties().isOccluding()) {
            occluding++;
        }
        if (y * face.y() + z * face.z() > 0
                && block.getNeighborBlock(0, y, z).getProperties().isOccluding()) {
            occluding++;
        }
        if (x * face.x() + y * face.y() + z * face.z() > 0
                && block.getNeighborBlock(x, y, z).getProperties().isOccluding()) {
            occluding++;
        }
        return Math.max(0F, Math.min(1F - Math.min(3, occluding) * 0.25F, 1F));
    }

    private static int boundary(float value) {
        if (Math.abs(value - 1F) < EPSILON) {
            return 1;
        }
        if (Math.abs(value) < EPSILON) {
            return -1;
        }
        return 0;
    }

    private static float interpolatedAo(Vertex vertex, float[] ao) {
        if (vertex.source >= 0) {
            return ao[vertex.source];
        }
        return vertex.ao;
    }

    private void assignUvs(Face face, Direction direction, Vertex[] vertices) {
        Vector4f raw = face.getUv();
        float[][] values = {
            {raw.getX() / 16F, raw.getW() / 16F},
            {raw.getZ() / 16F, raw.getW() / 16F},
            {raw.getZ() / 16F, raw.getY() / 16F},
            {raw.getX() / 16F, raw.getY() / 16F}
        };
        int steps = Math.floorMod(Math.floorDiv(face.getRotation(), 90), 4);
        for (int index = 0; index < vertices.length; index++) {
            float[] uv = values[(steps + index) % 4];
            vertices[index].u = uv[0];
            vertices[index].v = uv[1];
        }
        if (variant.isUvlock() && variant.isTransformed()) {
            float angle = uvLockRotation(direction);
            float cosine = TrigMath.cos(angle);
            float sine = TrigMath.sin(angle);
            for (Vertex vertex : vertices) {
                float u = vertex.u - 0.5F;
                float v = vertex.v - 0.5F;
                vertex.u = u * cosine - v * sine + 0.5F;
                vertex.v = u * sine + v * cosine + 0.5F;
            }
        }
    }

    private void transformPositions(Element element, Vertex[] vertices) {
        MatrixM4f elementTransform = element.getRotation().getMatrix();
        for (Vertex vertex : vertices) {
            VectorM3f position = new VectorM3f(vertex.x, vertex.y, vertex.z)
                    .transform(elementTransform).mul(BLOCK_SCALE);
            if (variant.isTransformed()) {
                position.transform(variant.getTransformMatrix());
            }
            vertex.x = position.x;
            vertex.y = position.y;
            vertex.z = position.z;
        }
    }

    private Direction transformedDirection(Direction direction, MatrixM4f elementTransform) {
        VectorM3f vector = new VectorM3f(0F, 0F, 0F).set(direction.toVector())
                .rotateAndScale(elementTransform);
        if (variant.isTransformed()) {
            vector.rotateAndScale(variant.getTransformMatrix());
        }
        return direction(axis(vector.x, vector.y, vector.z));
    }

    private TextureOrientation.Frame textureFrame(Direction face, Vertex[] vertices) {
        AxisVector right = null;
        AxisVector up = null;
        for (int first = 0; first < vertices.length; first++) {
            for (int second = first + 1; second < vertices.length; second++) {
                Vertex a = vertices[first];
                Vertex b = vertices[second];
                if (Math.abs(a.v - b.v) < EPSILON && Math.abs(a.u - b.u) > EPSILON) {
                    right = a.u < b.u ? axisBetween(a, b) : axisBetween(b, a);
                }
                if (Math.abs(a.u - b.u) < EPSILON && Math.abs(a.v - b.v) > EPSILON) {
                    up = a.v > b.v ? axisBetween(a, b) : axisBetween(b, a);
                }
            }
        }
        if (right == null || up == null) {
            throw new IllegalArgumentException("model face has no orthogonal texture frame");
        }
        return TextureOrientation.classify(face, up, right);
    }

    private float uvLockRotation(Direction direction) {
        VectorM3f rotatedNormal = new VectorM3f(0F, 0F, 0F).set(direction.toVector());
        VectorM3f rotatedUp = new VectorM3f(0F, 0F, 0F)
                .set(direction.getLocalUp().toVector());
        rotatedNormal.rotateAndScale(variant.getTransformMatrix());
        rotatedUp.rotateAndScale(variant.getTransformMatrix());
        VectorM3f projected = new VectorM3f(0F, 1F, 0F);
        float dot = projected.dot(rotatedNormal);
        projected.set(
                -rotatedNormal.x * dot,
                1F - rotatedNormal.y * dot,
                -rotatedNormal.z * dot
        );
        if (projected.lengthSquared() < 0.01) {
            Direction upDown = rotatedNormal.y > 0F ? Direction.UP : Direction.DOWN;
            projected.set(upDown.getLocalUp().toVector());
        } else {
            projected.normalize();
        }
        dot = rotatedUp.dot(projected);
        VectorM3f cross = new VectorM3f(rotatedUp.x, rotatedUp.y, rotatedUp.z).cross(projected);
        return (float) TrigMath.atan2(cross.dot(rotatedNormal), dot);
    }

    private static List<Vertex> clip(List<Vertex> source, float[] bounds) {
        List<Vertex> result = source;
        result = clipEdge(result, 0, bounds[0], true);
        result = clipEdge(result, 0, bounds[1], false);
        result = clipEdge(result, 1, bounds[2], true);
        return clipEdge(result, 1, bounds[3], false);
    }

    private static List<Vertex> clipEdge(
            List<Vertex> source,
            int axis,
            float boundary,
        boolean keepGreater
    ) {
        List<Vertex> output = new ArrayList<>();
        if (source.isEmpty()) {
            return output;
        }
        Vertex previous = source.get(source.size() - 1);
        boolean previousInside = inside(previous, axis, boundary, keepGreater);
        for (Vertex current : source) {
            boolean currentInside = inside(current, axis, boundary, keepGreater);
            if (currentInside != previousInside) {
                float previousValue = axis == 0 ? previous.u : previous.v;
                float currentValue = axis == 0 ? current.u : current.v;
                float fraction = (boundary - previousValue) / (currentValue - previousValue);
                output.add(Vertex.interpolate(previous, current, fraction));
            }
            if (currentInside) {
                output.add(current.copy());
            }
            previous = current;
            previousInside = currentInside;
        }
        return output;
    }

    private static boolean inside(Vertex vertex, int axis, float boundary, boolean greater) {
        float value = axis == 0 ? vertex.u : vertex.v;
        return greater ? value >= boundary - EPSILON : value <= boundary + EPSILON;
    }

    private static float uvArea(List<Vertex> polygon) {
        float twiceArea = 0F;
        for (int index = 0; index < polygon.size(); index++) {
            Vertex current = polygon.get(index);
            Vertex next = polygon.get((index + 1) % polygon.size());
            twiceArea += current.u * next.v - next.u * current.v;
        }
        return Math.abs(twiceArea) * 0.5F;
    }

    private static boolean nonDegenerate(List<Vertex> polygon) {
        return polygon.size() >= 3 && uvArea(polygon) > EPSILON;
    }

    static int piecedPartCount(float minU, float minV, float maxU, float maxV) {
        Vertex first = new Vertex(0F, 0F, 0F);
        first.u = minU;
        first.v = maxV;
        Vertex second = new Vertex(1F, 0F, 0F);
        second.u = maxU;
        second.v = maxV;
        Vertex third = new Vertex(1F, 1F, 0F);
        third.u = maxU;
        third.v = minV;
        Vertex fourth = new Vertex(0F, 1F, 0F);
        fourth.u = minU;
        fourth.v = minV;
        List<Vertex> face = List.of(first, second, third, fourth);
        int count = 0;
        for (float[] bounds : PIECED_BOUNDS) {
            if (nonDegenerate(clip(face, bounds))) {
                count++;
            }
        }
        return count;
    }

    private static AxisVector axisBetween(Vertex low, Vertex high) {
        return axis(high.x - low.x, high.y - low.y, high.z - low.z);
    }

    private static AxisVector axis(float x, float y, float z) {
        float maximum = Math.max(Math.abs(x), Math.max(Math.abs(y), Math.abs(z)));
        if (maximum < EPSILON) {
            throw new IllegalArgumentException("zero-length model direction");
        }
        int rx = Math.abs(x) > maximum - EPSILON ? x > 0F ? 1 : -1 : 0;
        int ry = Math.abs(y) > maximum - EPSILON ? y > 0F ? 1 : -1 : 0;
        int rz = Math.abs(z) > maximum - EPSILON ? z > 0F ? 1 : -1 : 0;
        if (Math.abs(rx) + Math.abs(ry) + Math.abs(rz) != 1) {
            throw new IllegalArgumentException("model direction is not axis-aligned");
        }
        return new AxisVector(rx, ry, rz);
    }

    private static Direction direction(AxisVector vector) {
        for (Direction direction : Direction.values()) {
            if (direction.toVector().getX() == vector.x()
                    && direction.toVector().getY() == vector.y()
                    && direction.toVector().getZ() == vector.z()) {
                return direction;
            }
        }
        throw new IllegalArgumentException("axis vector is not a face normal");
    }

    private static AxisVector normal(Direction direction) {
        return new AxisVector(
                direction.toVector().getX(),
                direction.toVector().getY(),
                direction.toVector().getZ()
        );
    }

    private static AxisVector finalNormal(List<Vertex> vertices) {
        Vertex a = vertices.get(0);
        Vertex b = vertices.get(1);
        Vertex c = vertices.get(2);
        float abx = b.x - a.x;
        float aby = b.y - a.y;
        float abz = b.z - a.z;
        float acx = c.x - a.x;
        float acy = c.y - a.y;
        float acz = c.z - a.z;
        return axis(aby * acz - abz * acy,
                abz * acx - abx * acz,
                abx * acy - aby * acx);
    }

    private static Vertex[] corners(Vector3f from, Vector3f to) {
        float minX = from.getX();
        float minY = from.getY();
        float minZ = from.getZ();
        float maxX = to.getX();
        float maxY = to.getY();
        float maxZ = to.getZ();
        return new Vertex[]{
            new Vertex(minX, minY, minZ), new Vertex(minX, minY, maxZ),
            new Vertex(maxX, minY, minZ), new Vertex(maxX, minY, maxZ),
            new Vertex(minX, maxY, minZ), new Vertex(minX, maxY, maxZ),
            new Vertex(maxX, maxY, minZ), new Vertex(maxX, maxY, maxZ)
        };
    }

    private static float hashToFloat(int x, int z, long seed) {
        long hash = x * 73428767L ^ z * 4382893L ^ seed * 457;
        return (hash * (hash + 456149) & 0x00ffffff) / (float) 0x01000000;
    }

    private record LightSample(int sunlight, int blocklight) {
    }

    private static final class Vertex {
        private float x;
        private float y;
        private float z;
        private float u;
        private float v;
        private float ao = 1F;
        private int source = -1;

        private Vertex(float x, float y, float z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        private Vertex copy() {
            Vertex copy = new Vertex(x, y, z);
            copy.u = u;
            copy.v = v;
            copy.ao = ao;
            copy.source = source;
            return copy;
        }

        private static Vertex interpolate(Vertex from, Vertex to, float fraction) {
            Vertex result = new Vertex(
                    lerp(from.x, to.x, fraction),
                    lerp(from.y, to.y, fraction),
                    lerp(from.z, to.z, fraction)
            );
            result.u = lerp(from.u, to.u, fraction);
            result.v = lerp(from.v, to.v, fraction);
            result.ao = lerp(from.ao, to.ao, fraction);
            return result;
        }

        private static float lerp(float start, float end, float fraction) {
            return start + (end - start) * fraction;
        }
    }
}
