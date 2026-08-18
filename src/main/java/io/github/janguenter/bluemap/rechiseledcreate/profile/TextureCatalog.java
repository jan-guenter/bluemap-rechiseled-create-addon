/*
 * SPDX-License-Identifier: MIT
 */
package io.github.janguenter.bluemap.rechiseledcreate.profile;

import de.bluecolored.bluemap.core.util.Key;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Compact layout metadata derived from exact installed texture metadata. */
public final class TextureCatalog {

    private static final int MAX_BYTES = 256 * 1024;
    private final Map<Key, Entry> entries;

    private TextureCatalog(Map<Key, Entry> entries) {
        this.entries = Collections.unmodifiableMap(new LinkedHashMap<>(entries));
    }

    public static TextureCatalog load(String resource, int expectedRows, String expectedSha256) {
        byte[] raw = DefinitionCatalog.read(resource, MAX_BYTES);
        if (!expectedSha256.equals(DefinitionCatalog.sha256(raw))) {
            throw new IllegalStateException("texture catalog integrity mismatch");
        }
        Map<Key, Entry> entries = new LinkedHashMap<>();
        String previous = null;
        for (String line : new String(raw, StandardCharsets.US_ASCII).split("\n", -1)) {
            if (line.isEmpty()) {
                continue;
            }
            String[] fields = line.split("\t", -1);
            if (fields.length != 5) {
                throw new IllegalStateException("texture catalog row shape changed");
            }
            Key key = Key.parse(fields[0]);
            Entry entry = new Entry(
                    TextureLayout.parse(fields[1]),
                    Integer.parseInt(fields[2]),
                    Integer.parseInt(fields[3]),
                    fields[4]
            );
            if (previous != null && previous.compareTo(key.getFormatted()) >= 0) {
                throw new IllegalStateException("texture catalog is not sorted");
            }
            if (entries.put(key, entry) != null) {
                throw new IllegalStateException("texture catalog repeats a key");
            }
            previous = key.getFormatted();
        }
        if (entries.size() != expectedRows) {
            throw new IllegalStateException("texture catalog row count changed");
        }
        return new TextureCatalog(entries);
    }

    public Entry get(Key key) {
        return entries.get(key);
    }

    public Set<Key> keys() {
        return entries.keySet();
    }

    public Map<Key, Entry> entries() {
        return entries;
    }

    public record Entry(TextureLayout layout, int width, int height, String metadataSha256) {
        public Entry {
            if (width < 1 || height < 1 || !(metadataSha256.equals("-")
                    || metadataSha256.matches("[0-9a-f]{64}"))) {
                throw new IllegalArgumentException("malformed texture catalog row");
            }
        }
    }
}
