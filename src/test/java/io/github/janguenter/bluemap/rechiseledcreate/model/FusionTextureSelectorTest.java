/*
 * SPDX-License-Identifier: MIT
 */
package io.github.janguenter.bluemap.rechiseledcreate.model;

import io.github.janguenter.bluemap.rechiseledcreate.profile.TextureLayout;
import io.github.janguenter.bluemap.resource.fusion.model.FusionDirection;
import io.github.janguenter.bluemap.resource.fusion.model.FusionTextureLayout;
import io.github.janguenter.bluemap.resource.fusion.model.FusionTextureSelector;
import org.junit.jupiter.api.Test;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class FusionTextureSelectorTest {

    @Test
    void everyProfileLayoutMapsToTheSharedModule() {
        for (TextureLayout layout : TextureLayout.values()) {
            assertEquals(layout.name(), FusionTextureLayout.valueOf(layout.name()).name());
        }
    }

    @Test
    void locksAll256FullMasksAndNeverSelectsPaddingOrCell41()
            throws NoSuchAlgorithmException {
        byte[] selected = new byte[256];
        Set<Integer> tiles = new HashSet<>();
        for (int mask = 0; mask < 256; mask++) {
            int tile = FusionTextureSelector.tile(FusionTextureLayout.FULL, mask);
            selected[mask] = (byte) tile;
            tiles.add(tile);
        }
        assertEquals(
                "bfd54c79f43a7ed6c02e34f967b75aa7abba4a9b94d88f0226281734164fb3b5",
                HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(selected))
        );
        assertEquals(47, tiles.size());
        assertFalse(tiles.contains(41));
        assertFalse(tiles.stream().anyMatch(tile -> tile < 0 || tile >= 48));
    }

    @Test
    void locksSimpleHorizontalAndVerticalSelectorsForEveryMask() {
        int[] simple = {0, 7, 6, 14, 2, 5, 10, 8, 3, 15, 4, 12, 11, 13, 9, 1};
        for (int mask = 0; mask < 256; mask++) {
            int cardinal = bit(mask, 0) | bit(mask, 2) << 1
                    | bit(mask, 4) << 2 | bit(mask, 6) << 3;
            assertEquals(simple[cardinal],
                    FusionTextureSelector.tile(FusionTextureLayout.SIMPLE, mask));
            int horizontal = bit(mask, 6) == 1
                    ? bit(mask, 2) == 1 ? 2 : 3 : bit(mask, 2);
            int vertical = bit(mask, 0) == 1
                    ? bit(mask, 4) == 1 ? 2 : 3 : bit(mask, 4);
            assertEquals(horizontal,
                    FusionTextureSelector.tile(FusionTextureLayout.HORIZONTAL, mask));
            assertEquals(vertical,
                    FusionTextureSelector.tile(FusionTextureLayout.VERTICAL, mask));
        }
    }

    @Test
    void locksPiecedShortcutsAndCornerQuadrants() {
        assertEquals(0, FusionTextureSelector.tile(FusionTextureLayout.PIECED, 0x00));
        assertEquals(1, FusionTextureSelector.tile(FusionTextureLayout.PIECED, 0xff));
        assertEquals(2, FusionTextureSelector.tile(FusionTextureLayout.PIECED, 0x11));
        assertEquals(3, FusionTextureSelector.tile(FusionTextureLayout.PIECED, 0x44));
        assertEquals(4, FusionTextureSelector.tile(FusionTextureLayout.PIECED, 0x55));
        assertEquals(-1, FusionTextureSelector.tile(FusionTextureLayout.PIECED, 0x57));
        int[] expected = {0, 3, 2, 4, 0, 3, 2, 1};
        int[] actual = new int[8];
        for (int index = 0; index < 8; index++) {
            int mask = (index & 1) << FusionDirection.LEFT.bit()
                    | ((index >> 1) & 1) << FusionDirection.TOP.bit()
                    | ((index >> 2) & 1) << FusionDirection.TOP_LEFT.bit();
            actual[index] = FusionTextureSelector.piecedCorner(
                    mask, FusionDirection.TOP_LEFT
            );
        }
        assertArrayEquals(expected, actual);
    }

    private static int bit(int mask, int bit) {
        return mask >> bit & 1;
    }
}
