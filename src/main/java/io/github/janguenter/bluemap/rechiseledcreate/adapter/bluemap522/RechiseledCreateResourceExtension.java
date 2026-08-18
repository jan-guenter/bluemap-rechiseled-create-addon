/*
 * SPDX-License-Identifier: MIT
 */
package io.github.janguenter.bluemap.rechiseledcreate.adapter.bluemap522;

import de.bluecolored.bluemap.core.resources.ResourcePath;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.ResourcePack;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.ResourcePackExtension;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.blockstate.Variant;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.blockstate.VariantSet;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.blockstate.Variants;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.model.Element;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.model.Face;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.model.Model;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.texture.Texture;
import de.bluecolored.bluemap.core.util.Direction;
import de.bluecolored.bluemap.core.util.Key;
import de.bluecolored.bluemap.core.world.BlockProperties;
import de.bluecolored.bluemap.core.world.BlockState;
import io.github.janguenter.bluemap.rechiseledcreate.activation.RechiseledCreateRuntime;
import io.github.janguenter.bluemap.rechiseledcreate.profile.ExactModArtifactDetector;
import io.github.janguenter.bluemap.rechiseledcreate.profile.ProfileDisablement;
import io.github.janguenter.bluemap.rechiseledcreate.profile.RechiseledCreate111Fusion1312Profile;
import io.github.janguenter.bluemap.rechiseledcreate.profile.RechiseledCreateDefinition;
import io.github.janguenter.bluemap.rechiseledcreate.profile.TextureCatalog;
import io.github.janguenter.bluemap.rechiseledcreate.profile.TextureLayout;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** Exact artifact/schema activation, tile cropping, and allowlist routing. */
final class RechiseledCreateResourceExtension implements ResourcePackExtension {

    static final Key SYNTHETIC = Key.parse("bluemap_rechiseled_create:fusion_model");

    private final ResourcePack resourcePack;
    private final RechiseledCreateRuntime runtime;
    private Map<TileKey, Key> tileKeys = Map.of();
    private Map<Key, Boolean> opaqueSheets = Map.of();

    RechiseledCreateResourceExtension(ResourcePack resourcePack, RechiseledCreateRuntime runtime) {
        this.resourcePack = resourcePack;
        this.runtime = runtime;
    }

    @Override
    public void loadResources(Iterable<Path> roots) throws IOException, InterruptedException {
        if (ProfileDisablement.current().isDisabled(
                RechiseledCreate111Fusion1312Profile.PROFILE_ID
        )) {
            runtime.inactive("operator-disabled");
            return;
        }
        if (!ExactModArtifactDetector.matchesRequiredTuple(roots)) {
            runtime.inactive("exact-artifact-tuple-missing");
            return;
        }
        ActiveResourceSchemaValidator.Result schema = ActiveResourceSchemaValidator.validate(
                resourcePack,
                roots,
                RechiseledCreate111Fusion1312Profile.RESOURCES,
                RechiseledCreate111Fusion1312Profile.TEXTURES,
                RechiseledCreate111Fusion1312Profile.CHISEL_RESOURCES,
                RechiseledCreate111Fusion1312Profile.CHISEL_TEXTURES
        );
        if (!schema.valid()) {
            runtime.inactive(schema.reason());
            return;
        }
        de.bluecolored.bluemap.core.resources.pack.resourcepack.blockstate.BlockState dispatch =
                resourcePack.getBlockStates().get(SYNTHETIC);
        if (!validDispatch(dispatch)) {
            runtime.inactive("synthetic-dispatch-invalid");
            return;
        }
        runtime.activate(schema.catalog());
    }

    @Override
    public Set<Key> collectUsedTextureKeys() {
        if (!runtime.route().isActive()) {
            return Set.of();
        }
        Set<Key> used = new LinkedHashSet<>(
                RechiseledCreate111Fusion1312Profile.TEXTURES.keys()
        );
        used.addAll(RechiseledCreate111Fusion1312Profile.CHISEL_TEXTURES.keys());
        used.addAll(plannedTiles().values());
        return Set.copyOf(used);
    }

    @Override
    public void bake() {
        if (!runtime.route().isActive()) {
            return;
        }
        try {
            Map<TileKey, Key> planned = plannedTiles();
            for (Key output : planned.values()) {
                if (resourcePack.getTextures().get(output) != null) {
                    runtime.inactive("synthetic-texture-collision");
                    return;
                }
            }
            Map<Key, Texture> generated = cropTiles(planned);
            if (generated.size() != planned.size()) {
                runtime.inactive("required-texture-invalid");
                return;
            }
            generated.forEach(resourcePack.getTextures()::put);
            tileKeys = Map.copyOf(planned);
            opaqueSheets = sheetOpacity();
        } catch (IOException | RuntimeException exception) {
            runtime.inactive("required-texture-invalid");
        }
    }

    @Override
    public Key getBlockStateKey(Key key) {
        return runtime.route().isActive()
                && RechiseledCreate111Fusion1312Profile.ROUTED_BLOCKS.contains(key.getFormatted())
                ? SYNTHETIC : key;
    }

    @Override
    public void getBlockProperties(BlockState state, BlockProperties.Builder builder) {
        if (!runtime.route().isActive()) {
            return;
        }
        if (applyMechanicalChiselProperties(state, builder)) {
            return;
        }
        RechiseledCreateDefinition definition = RechiseledCreate111Fusion1312Profile.DEFINITIONS.get(
                state.getId().getFormatted()
        );
        if (definition == null) {
            definition = RechiseledCreate111Fusion1312Profile.DISCONNECTED_DEFINITIONS.get(
                    state.getId().getFormatted()
            );
        }
        if (definition == null) {
            return;
        }
        switch (definition.shape()) {
            case FULL, AXIS -> {
                boolean opaque = opaqueFullState(state);
                builder.culling(opaque).occluding(opaque).cullingIdentical(false);
            }
            case SLAB -> {
                boolean opaque = isDoubleSlab(state) && opaqueFullState(state);
                builder.culling(opaque).occluding(opaque).cullingIdentical(false);
            }
            case STAIRS -> builder.culling(false).occluding(false).cullingIdentical(false);
        }
    }

    static boolean applyMechanicalChiselProperties(
            BlockState state,
            BlockProperties.Builder builder
    ) {
        if (!RechiseledCreate111Fusion1312Profile.MECHANICAL_CHISEL_ID.equals(
                state.getId().getFormatted()
        )) {
            return false;
        }
        builder.culling(false).occluding(false).cullingIdentical(false);
        return true;
    }

    static boolean isDoubleSlab(BlockState state) {
        return "double".equals(state.getProperties().get("type"));
    }

    private boolean opaqueFullState(BlockState state) {
        de.bluecolored.bluemap.core.resources.pack.resourcepack.blockstate.BlockState original =
                resourcePack.getBlockStates().get(state.getId());
        FusionProgramCatalog catalog = runtime.catalog();
        if (original == null || catalog == null) {
            return false;
        }
        if (RechiseledCreate111Fusion1312Profile.FORCED_DISCONNECTED_BLOCKS.contains(
                state.getId().getFormatted()
        )) {
            return opaqueForcedDisconnectedState(original, state);
        }
        boolean[] selected = {false};
        boolean[] opaque = {true};
        original.forEach(state, 0, 0, 0, variant -> {
            FusionProgramCatalog.Program program = catalog.get(variant.getModel());
            if (program == null) {
                opaque[0] = false;
                return;
            }
            selected[0] = true;
            for (String value : program.textures().values()) {
                if (value.startsWith("#")) {
                    continue;
                }
                Key source = Key.parse(value);
                if (!opaqueSheets.getOrDefault(source, false)) {
                    opaque[0] = false;
                }
            }
        });
        return selected[0] && opaque[0];
    }

    private boolean opaqueForcedDisconnectedState(
            de.bluecolored.bluemap.core.resources.pack.resourcepack.blockstate.BlockState original,
            BlockState state
    ) {
        boolean[] selected = {false};
        boolean[] opaque = {true};
        original.forEach(state, 0, 0, 0, variant -> {
            Model model = variant.getModel().getResource(resourcePack.getModels()::get);
            if (model == null || !opaqueDisconnectedModel(model)) {
                opaque[0] = false;
                return;
            }
            selected[0] = true;
        });
        return selected[0] && opaque[0];
    }

    private boolean opaqueDisconnectedModel(Model model) {
        Element[] elements = model.getElements();
        if (elements == null || !model.isOccluding()) {
            return false;
        }
        int faces = 0;
        for (Element element : elements) {
            if (element == null) {
                continue;
            }
            for (Direction direction : Direction.values()) {
                Face face = element.getFaces().get(direction);
                if (face == null) {
                    continue;
                }
                faces++;
                ResourcePath<Texture> source = face.getTexture().getTexturePath(
                        model.getTextures()::get
                );
                Key selected = source == null ? null : tile(source, 0);
                Texture texture = selected == null
                        ? null : resourcePack.getTextures().get(selected);
                if (!opaqueTexture(texture)) {
                    return false;
                }
            }
        }
        return faces >= Direction.values().length;
    }

    private Map<Key, Boolean> sheetOpacity() {
        Map<Key, Boolean> opacity = new LinkedHashMap<>();
        for (Key source : RechiseledCreate111Fusion1312Profile.TEXTURES.keys()) {
            TextureCatalog.Entry entry = RechiseledCreate111Fusion1312Profile.TEXTURES.get(source);
            boolean opaque = true;
            int count = entry.layout().columns() * entry.layout().rows();
            for (int index = 0; index < count; index++) {
                if (!reachableTile(entry, index)) {
                    continue;
                }
                Key output = tile(source, index);
                Texture texture = output == null ? null : resourcePack.getTextures().get(output);
                if (!opaqueTexture(texture)) {
                    opaque = false;
                    break;
                }
            }
            opacity.put(source, opaque);
        }
        return Map.copyOf(opacity);
    }

    static boolean reachableTile(TextureCatalog.Entry entry, int index) {
        return !(entry.layout() == io.github.janguenter.bluemap.rechiseledcreate.profile.TextureLayout.FULL
                && index == 41);
    }

    static boolean opaqueTexture(Texture texture) {
        return texture != null && !texture.isHalfTransparent()
                && texture.getColorStraight().a >= 1F;
    }

    Key tile(Key source, int index) {
        TextureCatalog.Entry entry = RechiseledCreate111Fusion1312Profile.TEXTURES.get(source);
        if (entry != null && entry.layout() == TextureLayout.PLAIN && index == 0) {
            return source;
        }
        return tileKeys.get(new TileKey(source, index));
    }

    private Map<TileKey, Key> plannedTiles() {
        Map<TileKey, Key> planned = new LinkedHashMap<>();
        Set<Key> outputs = new LinkedHashSet<>();
        for (Key source : RechiseledCreate111Fusion1312Profile.TEXTURES.keys()) {
            TextureCatalog.Entry entry = RechiseledCreate111Fusion1312Profile.TEXTURES.get(source);
            if (entry.layout() == TextureLayout.PLAIN) {
                continue;
            }
            int count = entry.layout().columns() * entry.layout().rows();
            for (int index = 0; index < count; index++) {
                Key output = Key.parse("bluemap_rechiseled_create:tiles/"
                        + source.getNamespace() + "/" + source.getValue() + "/" + index);
                if (!outputs.add(output)) {
                    throw new IllegalArgumentException("synthetic texture key collision");
                }
                planned.put(new TileKey(source, index), output);
            }
        }
        return planned;
    }

    static int syntheticTileCount() {
        return RechiseledCreate111Fusion1312Profile.TEXTURES.entries().values().stream()
                .filter(entry -> entry.layout() != TextureLayout.PLAIN)
                .mapToInt(entry -> entry.layout().columns() * entry.layout().rows())
                .sum();
    }

    private Map<Key, Texture> cropTiles(Map<TileKey, Key> planned) throws IOException {
        Map<Key, Texture> generated = new LinkedHashMap<>();
        for (Map.Entry<TileKey, Key> request : planned.entrySet()) {
            TileKey tile = request.getKey();
            Texture sourceTexture = resourcePack.getTextures().get(tile.source());
            TextureCatalog.Entry entry = RechiseledCreate111Fusion1312Profile.TEXTURES.get(tile.source());
            if (sourceTexture == null || entry == null) {
                throw new IOException("required source sheet is missing");
            }
            BufferedImage sheet = sourceTexture.getTextureImage();
            if (!validBakedSheetDimensions(sheet, entry)) {
                throw new IOException("baked source sheet dimensions changed");
            }
            int tileWidth = entry.width() / entry.layout().columns();
            int tileHeight = entry.height() / entry.layout().physicalRows();
            int x = tile.index() % entry.layout().columns() * tileWidth;
            int y = tile.index() / entry.layout().columns() * tileHeight;
            if (tile.index() >= entry.layout().columns() * entry.layout().rows()
                    || x + tileWidth > sheet.getWidth()
                    || y + tileHeight > sheet.getHeight()) {
                throw new IOException("sheet tile leaves active logical crop");
            }
            BufferedImage copy = new BufferedImage(
                    tileWidth, tileHeight, BufferedImage.TYPE_INT_ARGB
            );
            Graphics2D graphics = copy.createGraphics();
            try {
                graphics.drawImage(sheet, -x, -y, null);
            } finally {
                graphics.dispose();
            }
            generated.put(request.getValue(), Texture.from(request.getValue(), copy));
        }
        return generated;
    }

    static boolean validBakedSheetDimensions(
            BufferedImage sheet,
            TextureCatalog.Entry entry
    ) {
        return sheet.getWidth() == entry.width() && sheet.getHeight() == entry.height();
    }

    private static boolean validDispatch(
            de.bluecolored.bluemap.core.resources.pack.resourcepack.blockstate.BlockState state
    ) {
        if (state == null || state.getMultipart() != null) {
            return false;
        }
        Variants variants = state.getVariants();
        if (variants == null || variants.getDefaultVariant() == null) {
            return false;
        }
        VariantSet set = variants.getDefaultVariant();
        if (set.getVariants().length != 1) {
            return false;
        }
        Variant variant = set.getVariants()[0];
        return BlueMap522Adapter.isExpectedDispatch(variant);
    }

    private record TileKey(Key source, int index) {
    }
}
