/*
 * SPDX-License-Identifier: MIT
 */
package io.github.janguenter.bluemap.rechiseledcreate.adapter.bluemap522;

import de.bluecolored.bluemap.core.map.TextureGallery;
import de.bluecolored.bluemap.core.map.hires.MaxCapacityReachedException;
import de.bluecolored.bluemap.core.map.hires.RenderSettings;
import de.bluecolored.bluemap.core.map.hires.TileModelView;
import de.bluecolored.bluemap.core.map.hires.block.BlockRenderer;
import de.bluecolored.bluemap.core.map.hires.block.ResourceModelRenderer;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.ResourcePack;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.blockstate.Variant;
import de.bluecolored.bluemap.core.util.math.Color;
import de.bluecolored.bluemap.core.world.BlockState;
import de.bluecolored.bluemap.core.world.block.BlockNeighborhood;
import io.github.janguenter.bluemap.rechiseledcreate.activation.RechiseledCreateRuntime;
import io.github.janguenter.bluemap.rechiseledcreate.profile.RechiseledCreate111Fusion1312Profile;

import java.util.function.Function;

/** Generic Fusion dispatch with atomic stock fallback for every routed block. */
final class RechiseledCreateRenderer implements BlockRenderer {

    private final ResourcePack resourcePack;
    private final RechiseledCreateRuntime runtime;
    private final ResourceModelRenderer stock;
    private final FusionModelEmitter emitter;
    private final MechanicalChiselEmitter chisel;
    private final BoundedDiagnostics diagnostics = new BoundedDiagnostics();

    RechiseledCreateRenderer(
            ResourcePack resourcePack,
            TextureGallery textureGallery,
            RenderSettings renderSettings,
            RechiseledCreateRuntime runtime
    ) {
        this.resourcePack = resourcePack;
        this.runtime = runtime;
        this.stock = new ResourceModelRenderer(resourcePack, textureGallery, renderSettings);
        this.emitter = new FusionModelEmitter(resourcePack, textureGallery, renderSettings);
        this.chisel = new MechanicalChiselEmitter(
                resourcePack, textureGallery, renderSettings
        );
    }

    @Override
    public void render(
            BlockNeighborhood block,
            Variant dispatch,
            TileModelView target,
            Color mapColor
    ) {
        int start = target.getStart();
        Color initialMapColor = new Color().set(mapColor);
        String blockId = block.getBlockState().getId().getFormatted();
        FusionProgramCatalog catalog = runtime.catalog();
        if (!runtime.route().isActive() || catalog == null
                || !RechiseledCreate111Fusion1312Profile.ROUTED_BLOCKS.contains(blockId)) {
            diagnostics.report("inactive-or-unknown-dispatch");
            renderStock(block, target, mapColor);
            return;
        }
        try {
            if (RechiseledCreate111Fusion1312Profile.MECHANICAL_CHISEL_ID.equals(blockId)) {
                boolean success = forEachOriginalVariant(
                        block,
                        target,
                        variant -> {
                            stock.render(block, variant, target, mapColor);
                            return chisel.emit(block, target);
                        }
                );
                if (!success) {
                    diagnostics.report("mechanical-chisel-render-failed");
                    resetAndRenderStock(block, target, start, mapColor, initialMapColor);
                }
                return;
            }
            boolean disconnected = RechiseledCreate111Fusion1312Profile
                    .FORCED_DISCONNECTED_BLOCKS.contains(blockId);
            boolean success = forEachOriginalVariant(
                    block,
                    target,
                    variant -> disconnected
                            ? emitter.renderDisconnected(block, variant, target, mapColor)
                            : emitter.render(block, variant, target, mapColor, catalog)
            );
            if (!success) {
                diagnostics.report("resource-render-failed");
                resetAndRenderStock(block, target, start, mapColor, initialMapColor);
            }
        } catch (MaxCapacityReachedException exception) {
            throw exception;
        } catch (IllegalArgumentException exception) {
            diagnostics.report("malformed-state-or-resource");
            resetAndRenderStock(block, target, start, mapColor, initialMapColor);
        } catch (RuntimeException exception) {
            diagnostics.report("contained-render-failure");
            resetAndRenderStock(block, target, start, mapColor, initialMapColor);
        }
    }

    private boolean forEachOriginalVariant(
            BlockNeighborhood block,
            TileModelView target,
            Function<Variant, Boolean> renderer
    ) {
        de.bluecolored.bluemap.core.resources.pack.resourcepack.blockstate.BlockState state =
                resourcePack.getBlockStates().get(block.getBlockState().getId());
        if (state == null) {
            return false;
        }
        boolean[] selected = {false};
        boolean[] success = {true};
        state.forEach(
                block.getBlockState(), block.getX(), block.getY(), block.getZ(),
                variant -> {
                    if (success[0]) {
                        selected[0] = true;
                        target.initialize();
                        success[0] = renderer.apply(variant);
                    }
                }
        );
        return selected[0] && success[0];
    }

    private void resetAndRenderStock(
            BlockNeighborhood block,
            TileModelView target,
            int start,
            Color mapColor,
            Color initialMapColor
    ) {
        target.getTileModel().reset(start);
        target.initialize(start);
        mapColor.set(initialMapColor);
        renderStock(block, target, mapColor);
    }

    private void renderStock(
            BlockNeighborhood block,
            TileModelView target,
            Color mapColor
    ) {
        forEachOriginalVariant(block, target, variant -> {
            stock.render(block, variant, target, mapColor);
            return true;
        });
    }

    static boolean sameCompleteState(BlockState first, BlockState second) {
        return first.equals(second);
    }
}
