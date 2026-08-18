/*
 * SPDX-License-Identifier: MIT
 */
package io.github.janguenter.bluemap.rechiseledcreate.adapter.bluemap522;

import de.bluecolored.bluemap.core.map.hires.block.BlockRendererType;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.ResourcePack;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.blockstate.Variant;
import de.bluecolored.bluemap.core.util.Keyed;
import de.bluecolored.bluemap.core.util.Registry;
import io.github.janguenter.bluemap.rechiseledcreate.activation.RechiseledCreateRuntime;

/** BlueMap 5.22 internal ABI boundary. */
public final class BlueMap522Adapter {

    private static final RechiseledCreateRuntime RUNTIME = RechiseledCreateRuntime.INSTANCE;
    static final de.bluecolored.bluemap.core.util.Key RENDERER_KEY =
            de.bluecolored.bluemap.core.util.Key.parse("bluemap_rechiseled_create:fusion_model");
    private static final BlockRendererType RENDERER = new BlockRendererType.Impl(
            RENDERER_KEY,
            (pack, gallery, settings) -> new RechiseledCreateRenderer(pack, gallery, settings, RUNTIME)
    );
    private static final ResourcePack.Extension<RechiseledCreateResourceExtension> EXTENSION =
            new RechiseledCreateResourceExtensionType(RUNTIME);

    private BlueMap522Adapter() {
    }

    public static synchronized boolean install() {
        if (!canRegister(BlockRendererType.REGISTRY, RENDERER)
                || !canRegister(ResourcePack.Extension.REGISTRY, EXTENSION)) {
            RUNTIME.disable("registry-collision");
            return false;
        }
        if (!register(BlockRendererType.REGISTRY, RENDERER)
                || !register(ResourcePack.Extension.REGISTRY, EXTENSION)) {
            RUNTIME.disable("registry-collision");
            return false;
        }
        return true;
    }

    static boolean isExpectedDispatch(Variant variant) {
        return variant != null
                && variant.getRenderer() == RENDERER
                && ResourcePack.MISSING_BLOCK_MODEL.equals(variant.getModel())
                && !variant.isTransformed()
                && !variant.isUvlock()
                && Double.compare(variant.getWeight(), 1D) == 0;
    }

    static RechiseledCreateResourceExtension extension(ResourcePack resourcePack) {
        return resourcePack.getExtension(EXTENSION);
    }

    private static <T extends Keyed> boolean canRegister(Registry<T> registry, T candidate) {
        T existing = registry.get(candidate.getKey());
        return existing == null || existing == candidate;
    }

    private static <T extends Keyed> boolean register(Registry<T> registry, T candidate) {
        T existing = registry.get(candidate.getKey());
        if (existing == null) {
            registry.register(candidate);
            existing = registry.get(candidate.getKey());
        }
        return existing == candidate;
    }
}
