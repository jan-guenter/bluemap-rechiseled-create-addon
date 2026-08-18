/*
 * SPDX-License-Identifier: MIT
 */
package io.github.janguenter.bluemap.rechiseledcreate.adapter.bluemap522;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** Process-bounded diagnostics that never include world, player, or NBT values. */
final class BoundedDiagnostics {

    private static final int MAX_MESSAGES = 16;

    private final Set<String> seen = ConcurrentHashMap.newKeySet();
    private final AtomicInteger emitted = new AtomicInteger();

    void report(String key) {
        if (!seen.add(key)) {
            return;
        }
        int number = emitted.incrementAndGet();
        if (number <= MAX_MESSAGES) {
            System.err.println(
                    "BlueMap Rechiseled: Create add-on used stock fallback: " + key + "."
            );
        }
    }
}
