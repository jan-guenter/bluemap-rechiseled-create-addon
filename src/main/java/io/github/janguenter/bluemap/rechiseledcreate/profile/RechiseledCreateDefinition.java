/*
 * SPDX-License-Identifier: MIT
 */
package io.github.janguenter.bluemap.rechiseledcreate.profile;

import java.util.Objects;

/** Hash-locked metadata for one exact routed Rechiseled: Create block. */
public record RechiseledCreateDefinition(
        String blockId,
        ShapeFamily shape,
        int legalStates,
        String blockstateSha256,
        String directModelsSha256
) {

    public RechiseledCreateDefinition {
        Objects.requireNonNull(blockId, "blockId");
        Objects.requireNonNull(shape, "shape");
        Objects.requireNonNull(blockstateSha256, "blockstateSha256");
        Objects.requireNonNull(directModelsSha256, "directModelsSha256");
        if (!blockId.startsWith("rechiseledcreate:")
                || legalStates != shape.legalStates()
                || !blockstateSha256.matches("[0-9a-f]{64}")
                || !directModelsSha256.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException(
                    "malformed Rechiseled: Create rendering definition"
            );
        }
    }
}
