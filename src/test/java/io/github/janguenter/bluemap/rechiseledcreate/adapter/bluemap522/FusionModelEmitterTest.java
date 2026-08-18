/*
 * SPDX-License-Identifier: MIT
 */
package io.github.janguenter.bluemap.rechiseledcreate.adapter.bluemap522;

import io.github.janguenter.bluemap.rechiseledcreate.model.FusionTextureSelector;
import io.github.janguenter.bluemap.rechiseledcreate.profile.TextureLayout;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FusionModelEmitterTest {

    @Test
    void piecedClippingDropsZeroAreaSlabSeams() {
        assertEquals(4, FusionModelEmitter.piecedPartCount(0F, 0F, 1F, 1F));
        assertEquals(2, FusionModelEmitter.piecedPartCount(0F, 0.5F, 1F, 1F));
        assertEquals(1, FusionModelEmitter.piecedPartCount(0F, 0.5F, 0.5F, 1F));
        assertEquals(0, FusionModelEmitter.piecedPartCount(0F, 0.5F, 1F, 0.5F));
    }

    @Test
    void forcedDisconnectedModelsAlwaysSelectTileZero() {
        for (TextureLayout layout : TextureLayout.values()) {
            for (int mask = 0; mask < 256; mask++) {
                assertEquals(
                        FusionTextureSelector.tile(layout, 0),
                        FusionModelEmitter.selectedTile(layout, mask, true)
                );
                assertEquals(
                        FusionTextureSelector.tile(layout, mask),
                        FusionModelEmitter.selectedTile(layout, mask, false)
                );
            }
        }
    }
}
