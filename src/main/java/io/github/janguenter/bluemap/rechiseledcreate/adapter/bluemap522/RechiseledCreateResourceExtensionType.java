/*
 * SPDX-License-Identifier: MIT
 */
package io.github.janguenter.bluemap.rechiseledcreate.adapter.bluemap522;

import de.bluecolored.bluemap.core.resources.pack.resourcepack.ResourcePack;
import de.bluecolored.bluemap.core.util.Key;
import io.github.janguenter.bluemap.rechiseledcreate.activation.RechiseledCreateRuntime;

/** Resource-pack extension factory registered before resource loading begins. */
final class RechiseledCreateResourceExtensionType
        implements ResourcePack.Extension<RechiseledCreateResourceExtension> {

    static final Key KEY = Key.parse("bluemap_rechiseled_create:exact_profile");

    private final RechiseledCreateRuntime runtime;

    RechiseledCreateResourceExtensionType(RechiseledCreateRuntime runtime) {
        this.runtime = runtime;
    }

    @Override
    public Key getKey() {
        return KEY;
    }

    @Override
    public RechiseledCreateResourceExtension create(ResourcePack pack) {
        return new RechiseledCreateResourceExtension(pack, runtime);
    }
}
