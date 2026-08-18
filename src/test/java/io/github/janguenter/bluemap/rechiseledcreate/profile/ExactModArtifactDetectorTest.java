/*
 * SPDX-License-Identifier: MIT
 */
package io.github.janguenter.bluemap.rechiseledcreate.profile;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExactModArtifactDetectorTest {

    @Test
    void acceptsOnlyTheUniqueExactFourArtifactTuple(@TempDir Path temporary)
            throws IOException {
        Path bridge = required("bridgeJar");
        Path rechiseled = required("rechiseledJar");
        Path fusion = required("fusionJar");
        Path create = required("createJar");
        assertTrue(ExactModArtifactDetector.matchesRequiredTuple(List.of(
                bridge, rechiseled, fusion, create
        )));
        assertFalse(ExactModArtifactDetector.matchesRequiredTuple(List.of(
                bridge, rechiseled, fusion
        )));
        Path duplicate = temporary.resolve("duplicate-bridge.jar");
        Files.copy(bridge, duplicate);
        assertFalse(ExactModArtifactDetector.matchesRequiredTuple(List.of(
                bridge, duplicate, rechiseled, fusion, create
        )));
    }

    private static Path required(String property) {
        String value = System.getProperty(property);
        if (value == null || value.isBlank()) {
            throw new AssertionError("missing exact test artifact property: " + property);
        }
        return Path.of(value);
    }
}
