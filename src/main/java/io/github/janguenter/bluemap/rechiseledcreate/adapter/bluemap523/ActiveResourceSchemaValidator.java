/*
 * SPDX-License-Identifier: MIT
 */
package io.github.janguenter.bluemap.rechiseledcreate.adapter.bluemap523;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.ResourcePack;
import de.bluecolored.bluemap.core.util.Key;
import io.github.janguenter.bluemap.rechiseledcreate.profile.RechiseledCreate111Fusion1312Profile;
import io.github.janguenter.bluemap.rechiseledcreate.profile.ResourceManifest;
import io.github.janguenter.bluemap.rechiseledcreate.profile.TextureCatalog;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** Validates the first-wins active exact resource closure and compiles its model programs. */
final class ActiveResourceSchemaValidator {

    private static final int BUFFER_SIZE = 64 * 1024;
    private static final int MAX_MODEL_BYTES = 256 * 1024;

    private ActiveResourceSchemaValidator() {
    }

    static Result validate(
            ResourcePack resourcePack,
            Iterable<Path> roots,
            ResourceManifest manifest,
            TextureCatalog textures,
            ResourceManifest extraManifest,
            TextureCatalog extraTextures
    ) throws IOException, InterruptedException {
        Map<String, Capture> active = new LinkedHashMap<>();
        Map<String, Capture> activeExtra = new LinkedHashMap<>();
        Map<String, Capture> activeHostModels = new LinkedHashMap<>();
        for (Path root : roots) {
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
            resourcePack.loadResourcePath(root, activeRoot -> {
                collect(activeRoot, manifest, textures, active);
                collect(activeRoot, extraManifest, extraTextures, activeExtra);
                collect(
                        activeRoot,
                        RechiseledCreate111Fusion1312Profile.HOST_MODELS,
                        textures,
                        activeHostModels
                );
            });
        }
        if (active.size() != manifest.entries().size()) {
            return Result.invalid("required-resource-closure-missing");
        }
        if (activeExtra.size() != extraManifest.entries().size()) {
            return Result.invalid("mechanical-chisel-resource-closure-missing");
        }
        if (activeHostModels.size()
                != RechiseledCreate111Fusion1312Profile.HOST_MODELS.entries().size()) {
            return Result.invalid("host-geometry-abi-missing");
        }
        for (Capture capture : activeHostModels.values()) {
            if (!capture.valid()) {
                return Result.invalid("host-geometry-abi-mismatch");
            }
        }
        if (!validChiselModelChain(activeExtra, activeHostModels)) {
            return Result.invalid("mechanical-chisel-model-chain-mismatch");
        }

        Map<String, byte[]> models = new HashMap<>();
        Set<Key> pixelOverrides = new LinkedHashSet<>();
        String invalid = validateCaptures(
                manifest, active, textures, models, pixelOverrides
        );
        if (invalid != null) {
            return Result.invalid(invalid);
        }
        invalid = validateCaptures(
                extraManifest, activeExtra, extraTextures, null, pixelOverrides
        );
        if (invalid != null) {
            return Result.invalid(invalid);
        }
        try {
            return Result.success(FusionProgramCatalog.parse(models), pixelOverrides);
        } catch (IllegalArgumentException exception) {
            return Result.invalid("active-fusion-schema-mismatch");
        }
    }

    private static boolean validChiselModelChain(
            Map<String, Capture> chisel,
            Map<String, Capture> host
    ) {
        return validChiselModelChain(
                modelBytes(chisel, "assets/rechiseled/models/item/chisel.json"),
                modelBytes(host, "assets/minecraft/models/item/handheld.json"),
                modelBytes(host, "assets/minecraft/models/item/generated.json")
        );
    }

    static boolean validChiselModelChain(
            byte[] itemBytes,
            byte[] handheldBytes,
            byte[] generatedBytes
    ) {
        try {
            JsonObject item = model(itemBytes);
            JsonObject handheld = model(handheldBytes);
            JsonObject generated = model(generatedBytes);
            if (item == null || handheld == null || generated == null
                    || !"minecraft:item/handheld".equals(string(item, "parent"))
                    || !"rechiseled:item/chisel".equals(
                    string(item.getAsJsonObject("textures"), "layer0"))
                    || !"item/generated".equals(string(handheld, "parent"))
                    || !"builtin/generated".equals(string(generated, "parent"))) {
                return false;
            }
            JsonObject fixed = generated.getAsJsonObject("display")
                    .getAsJsonObject("fixed");
            return vector(fixed.getAsJsonArray("rotation"), 0F, 180F, 0F)
                    && vector(fixed.getAsJsonArray("scale"), 1F, 1F, 1F);
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private static byte[] modelBytes(Map<String, Capture> captures, String path) {
        Capture capture = captures.get(path);
        return capture == null ? null : capture.modelBytes();
    }

    private static JsonObject model(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        return JsonParser.parseString(new String(
                bytes, StandardCharsets.UTF_8
        )).getAsJsonObject();
    }

    private static String string(JsonObject object, String member) {
        return object == null || !object.has(member)
                ? null : object.get(member).getAsString();
    }

    private static boolean vector(JsonArray array, float x, float y, float z) {
        return array != null && array.size() == 3
                && Float.compare(array.get(0).getAsFloat(), x) == 0
                && Float.compare(array.get(1).getAsFloat(), y) == 0
                && Float.compare(array.get(2).getAsFloat(), z) == 0;
    }

    private static String validateCaptures(
            ResourceManifest manifest,
            Map<String, Capture> active,
            TextureCatalog textures,
            Map<String, byte[]> models,
            Set<Key> pixelOverrides
    ) throws InterruptedException {
        for (ResourceManifest.Entry entry : manifest.entries().values()) {
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
            Capture capture = active.get(entry.path());
            if (capture == null || !capture.valid()) {
                return "active-resource-integrity-mismatch";
            }
            if (entry.kind().equals("model") && models != null) {
                models.put(entry.path(), capture.modelBytes());
            } else if (entry.kind().equals("texture") && capture.pixelOverride()) {
                pixelOverrides.add(textureKey(entry.path()));
            }
        }
        return null;
    }

    private static void collect(
            Path root,
            ResourceManifest manifest,
            TextureCatalog textures,
            Map<String, Capture> active
    ) throws IOException {
        for (ResourceManifest.Entry entry : manifest.entries().values()) {
            String path = entry.path();
            if (active.containsKey(path)) {
                continue;
            }
            Path candidate = root.resolve(path);
            if (Files.isRegularFile(candidate)) {
                active.put(path, capture(candidate, entry, textures));
            }
        }
    }

    private static Capture capture(
            Path resource,
            ResourceManifest.Entry entry,
            TextureCatalog textures
    ) throws IOException {
        if (entry.kind().equals("texture")) {
            TextureCatalog.Entry texture = textures.get(textureKey(entry.path()));
            boolean valid = texture != null && validTexture(resource, texture);
            boolean pixelOverride = valid && !entry.sha256().equals(sha256(resource));
            return new Capture(valid, null, pixelOverride);
        }
        boolean valid = Files.size(resource) == entry.size()
                && entry.sha256().equals(sha256(resource));
        if (!valid || !entry.kind().equals("model")) {
            return new Capture(valid, null, false);
        }
        if (entry.size() > MAX_MODEL_BYTES) {
            return new Capture(false, null, false);
        }
        return new Capture(true, Files.readAllBytes(resource), false);
    }

    static boolean validTexture(Path resource, TextureCatalog.Entry texture) throws IOException {
        BufferedImage image;
        try (InputStream input = Files.newInputStream(resource)) {
            image = ImageIO.read(input);
        }
        return image != null && image.getWidth() == texture.width()
                && image.getHeight() == texture.height();
    }

    private static Key textureKey(String path) {
        String prefix = "assets/";
        String marker = "/textures/";
        if (!path.startsWith(prefix) || !path.endsWith(".png")) {
            throw new IllegalArgumentException("malformed texture manifest path");
        }
        int split = path.indexOf(marker, prefix.length());
        if (split <= prefix.length() || split + marker.length() >= path.length() - 4) {
            throw new IllegalArgumentException("malformed texture manifest path");
        }
        String namespace = path.substring(prefix.length(), split);
        String value = path.substring(split + marker.length(), path.length() - 4);
        if (!namespace.matches("[a-z0-9_.-]+")
                || value.contains("..") || value.startsWith("/") || value.endsWith("/")) {
            throw new IllegalArgumentException("malformed texture manifest path");
        }
        return Key.parse(namespace + ":" + value);
    }

    private static String sha256(Path path) throws IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
        byte[] buffer = new byte[BUFFER_SIZE];
        try (InputStream input = Files.newInputStream(path)) {
            int read;
            while ((read = input.read(buffer)) >= 0) {
                digest.update(buffer, 0, read);
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    record Result(
            boolean valid,
            String reason,
            FusionProgramCatalog catalog,
            Set<Key> pixelOverrides
    ) {

        private static Result success(
                FusionProgramCatalog catalog,
                Set<Key> pixelOverrides
        ) {
            return new Result(
                    true, "exact-active-schema", catalog, Set.copyOf(pixelOverrides)
            );
        }

        private static Result invalid(String reason) {
            return new Result(false, reason, null, Set.of());
        }
    }

    private record Capture(boolean valid, byte[] modelBytes, boolean pixelOverride) {
    }
}
