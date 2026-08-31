/* SPDX-License-Identifier: MIT */
package io.github.janguenter.bluemap.rechiseledcreate.adapter.bluemap523;

import de.bluecolored.bluemap.core.map.TextureGallery;
import de.bluecolored.bluemap.core.map.hires.RenderSettings;
import de.bluecolored.bluemap.core.map.hires.TileModel;
import de.bluecolored.bluemap.core.map.hires.TileModelView;
import de.bluecolored.bluemap.core.map.hires.block.ResourceModelRenderer;
import de.bluecolored.bluemap.core.resources.ResourcePath;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.ResourcePack;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.blockstate.Variant;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.model.Model;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.texture.Texture;
import de.bluecolored.bluemap.core.util.Key;
import de.bluecolored.bluemap.core.util.math.Color;
import de.bluecolored.bluemap.core.world.LightData;
import de.bluecolored.bluemap.core.world.block.BlockNeighborhood;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

/** Emits the stable shaft and empty vertical chisel tool from installed resources. */
final class MechanicalChiselEmitter {

    private static final Key CHISEL_TEXTURE = Key.parse("rechiseled:item/chisel");

    private final ResourcePack resourcePack;
    private final TextureGallery textures;
    private final ResourceModelRenderer models;

    MechanicalChiselEmitter(
            ResourcePack resourcePack,
            TextureGallery textures,
            RenderSettings renderSettings
    ) {
        this.resourcePack = resourcePack;
        this.textures = textures;
        this.models = new ResourceModelRenderer(resourcePack, textures, renderSettings);
    }

    boolean emit(BlockNeighborhood block, TileModelView target) {
        MechanicalChiselRenderPlan plan = MechanicalChiselRenderPlan.select(
                block.getBlockState().getProperties()
        ).orElse(null);
        if (plan == null) {
            return false;
        }
        int start = target.getTileModel().size();
        if (!model(plan.shaftModel(), plan.shaftTransform(), block, target)) {
            reset(target, start);
            return false;
        }
        if (plan.toolTransform().isPresent()
                && !tool(plan.toolTransform().orElseThrow(), block, target)) {
            reset(target, start);
            return false;
        }
        return true;
    }

    private boolean model(
            String model,
            AffineTransform transform,
            BlockNeighborhood block,
            TileModelView target
    ) {
        int start = target.getTileModel().size();
        models.render(
                block,
                new Variant(new ResourcePath<Model>(model)),
                target.initialize(),
                new Color().set(0F, 0F, 0F, 0F, true)
        );
        if (target.getTileModel().size() == start) {
            return false;
        }
        apply(target.initialize(start), transform);
        return true;
    }

    private boolean tool(
            AffineTransform transform,
            BlockNeighborhood block,
            TileModelView target
    ) {
        Texture texture = resourcePack.getTextures().get(CHISEL_TEXTURE);
        if (texture == null) {
            return false;
        }
        BufferedImage image;
        try {
            image = texture.getTextureImage();
        } catch (IOException exception) {
            return false;
        }
        List<ChiselSpriteGeometry.Triangle> triangles =
                ChiselSpriteGeometry.triangles(image);
        if (triangles.isEmpty()) {
            return false;
        }
        int start = target.add(triangles.size());
        TileModel mesh = target.getTileModel();
        LightData light = block.getLightData();
        int material = textures.get(CHISEL_TEXTURE);
        for (int offset = 0; offset < triangles.size(); offset++) {
            int index = start + offset;
            ChiselSpriteGeometry.Triangle triangle = triangles.get(offset);
            vertexData(mesh, index, triangle);
            mesh.setMaterialIndex(index, material);
            mesh.setColor(index, 1F, 1F, 1F);
            mesh.setAOs(index, 1F, 1F, 1F);
            mesh.setSunlight(index, light.getSkyLight());
            mesh.setBlocklight(index, light.getBlockLight());
        }
        apply(target.initialize(start), transform);
        return true;
    }

    private static void vertexData(
            TileModel mesh,
            int index,
            ChiselSpriteGeometry.Triangle triangle
    ) {
        ChiselSpriteGeometry.Vertex first = triangle.first();
        ChiselSpriteGeometry.Vertex second = triangle.second();
        ChiselSpriteGeometry.Vertex third = triangle.third();
        mesh.setPositions(index,
                first.x(), first.y(), first.z(),
                second.x(), second.y(), second.z(),
                third.x(), third.y(), third.z());
        mesh.setUvs(index,
                first.u(), first.v(), second.u(), second.v(), third.u(), third.v());
    }

    private static void apply(TileModelView target, AffineTransform transform) {
        float[] value = transform.copyValues();
        target.transform(
                value[0], value[1], value[2], value[3],
                value[4], value[5], value[6], value[7],
                value[8], value[9], value[10], value[11],
                value[12], value[13], value[14], value[15]
        );
    }

    private static void reset(TileModelView target, int start) {
        target.getTileModel().reset(start);
        target.initialize(start);
    }
}
