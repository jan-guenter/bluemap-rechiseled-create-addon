/*
 * SPDX-License-Identifier: MIT
 */
package io.github.janguenter.bluemap.rechiseledcreate.profile;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Hash-only exact installed-resource closure; it contains no third-party bytes. */
public final class ResourceManifest {

    private static final int MAX_BYTES = 2 * 1024 * 1024;
    private final Map<String, Entry> entries;

    private ResourceManifest(Map<String, Entry> entries) {
        this.entries = Collections.unmodifiableMap(new LinkedHashMap<>(entries));
    }

    public static ResourceManifest load(
            String resource,
            int expectedRows,
            String expectedSha256,
            Set<String> namespaces
    ) {
        if (namespaces == null || namespaces.isEmpty()
                || namespaces.stream().anyMatch(value -> !value.matches("[a-z0-9_.-]+"))) {
            throw new IllegalArgumentException("invalid resource namespaces");
        }
        byte[] raw = DefinitionCatalog.read(resource, MAX_BYTES);
        if (!expectedSha256.equals(DefinitionCatalog.sha256(raw))) {
            throw new IllegalStateException("resource manifest integrity mismatch");
        }
        Map<String, Entry> entries = new LinkedHashMap<>();
        String previous = null;
        for (String line : new String(raw, StandardCharsets.US_ASCII).split("\n", -1)) {
            if (line.isEmpty()) {
                continue;
            }
            String[] fields = line.split("\t", -1);
            if (fields.length != 4) {
                throw new IllegalStateException("resource manifest row shape changed");
            }
            Entry entry = new Entry(fields[0], fields[1], Long.parseLong(fields[2]), fields[3]);
            boolean owned = namespaces.stream().anyMatch(namespace ->
                    entry.path().startsWith("assets/" + namespace + "/")
            );
            if (!owned) {
                throw new IllegalStateException("resource manifest leaves its namespaces");
            }
            if (previous != null && previous.compareTo(entry.path()) >= 0) {
                throw new IllegalStateException("resource manifest is not sorted");
            }
            if (entries.put(entry.path(), entry) != null) {
                throw new IllegalStateException("resource manifest repeats a path");
            }
            previous = entry.path();
        }
        if (entries.size() != expectedRows) {
            throw new IllegalStateException("resource manifest row count changed");
        }
        return new ResourceManifest(entries);
    }

    public Map<String, Entry> entries() {
        return entries;
    }

    public record Entry(String kind, String path, long size, String sha256) {
        public Entry {
            if (!kind.matches("blockstate|model|metadata|texture")
                    || !path.startsWith("assets/")
                    || size < 1 || !sha256.matches("[0-9a-f]{64}")) {
                throw new IllegalArgumentException("malformed resource manifest row");
            }
        }
    }
}
