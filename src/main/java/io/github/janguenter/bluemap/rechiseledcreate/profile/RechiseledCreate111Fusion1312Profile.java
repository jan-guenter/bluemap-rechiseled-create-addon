/*
 * SPDX-License-Identifier: MIT
 */
package io.github.janguenter.bluemap.rechiseledcreate.profile;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** Exact All the Mons 1.2.0 Rechiseled: Create bridge profile. */
public final class RechiseledCreate111Fusion1312Profile {

    private static final String RESOURCE_ROOT =
            "/bluemap-rechiseled-create/profiles/rechiseledcreate/"
                    + "1.1.1-rechiseled-1.2.5-fusion-1.3.12-create-6.0.10/";

    public static final String PROFILE_ID = "rechiseledcreate-1.1.1-atm-1.2.0";
    public static final String BRIDGE_SHA256 =
            "ba89cd5d1221621ed226cc7f1c26dc84a660cc4f6d122753052429f96d71248d";
    public static final long BRIDGE_SIZE = 983_177L;
    public static final String RECHISELED_SHA256 =
            "7bf14cf8a4bfdc4b6c990126a75da29fd2bb7559d1c05b71e29c8fd5ae044435";
    public static final long RECHISELED_SIZE = 11_498_611L;
    public static final String FUSION_SHA256 =
            "17f5215648a98bcde4134577b013200dbf363273ae282449c51408ae8346f2fa";
    public static final long FUSION_SIZE = 923_270L;
    public static final String CREATE_SHA256 =
            "ef87fe5709f1ba1f5b8bb20a2925b5afb4669e178fd6d8bf10c167759eefe37a";
    public static final long CREATE_SIZE = 19_123_767L;
    public static final int ALL_BLOCK_COUNT = 242;
    public static final int FUSION_ROUTED_BLOCK_COUNT = 163;
    public static final int FUSION_ROUTED_STATE_COUNT = 2_661;
    public static final int ROUTED_BLOCK_COUNT = 242;
    public static final int ROUTED_STATE_COUNT = 2_909;
    public static final int FORCED_DISCONNECTED_BLOCK_COUNT = 78;
    public static final int FORCED_DISCONNECTED_STATE_COUNT = 224;
    public static final int DIRECT_MODEL_COUNT = 510;
    public static final int MODEL_COUNT = 513;
    public static final int TEXTURE_COUNT = 180;
    public static final int METADATA_COUNT = 107;
    public static final int RESOURCE_COUNT = 1_041;
    public static final int HOST_MODEL_COUNT = 9;
    public static final int CHISEL_RESOURCE_COUNT = 16;
    public static final int CHISEL_TEXTURE_COUNT = 9;
    public static final String MECHANICAL_CHISEL_ID = "rechiseledcreate:mechanical_chisel";
    public static final String DEFINITIONS_SHA256 =
            "6eb10848b97ec6b84f4d70857a781de60185631d757def1fd5d23ff502cbe779";
    public static final String RESOURCES_SHA256 =
            "a24f92113ced8ad9de0c24d01cf89f4ae3b10c24cd5f77f7ef0f4041980b22ed";
    public static final String TEXTURES_SHA256 =
            "c7a0d3ee167b2344054e72c740f4e374f0928d9f6dbda64a9c83d79bf1fb8001";
    public static final String HOST_MODELS_SHA256 =
            "edde9209002e63eb5a989daa489cb36edb579806473f66baac4dfe56c07c8b80";
    public static final String CHISEL_RESOURCES_SHA256 =
            "7399a454ec55f3803a54ed7d6f3649c3bd907682950f8078c173b26a3e49eecb";
    public static final String CHISEL_TEXTURES_SHA256 =
            "168b86e50c5a5e03438e46e3bd81fb54536c60a119d37875a621b93df2b78481";
    public static final String DISCONNECTED_DEFINITIONS_SHA256 =
            "62fe770aef64681963418cc386834c32f36a0ad1086deaa4aa26af152ac70958";

    public static final DefinitionCatalog CATALOG = DefinitionCatalog.load(
            RESOURCE_ROOT + "definitions.tsv",
            FUSION_ROUTED_BLOCK_COUNT,
            DEFINITIONS_SHA256
    );
    public static final Map<String, RechiseledCreateDefinition> DEFINITIONS = CATALOG.definitions();
    public static final Set<String> FUSION_ROUTED_BLOCKS = DEFINITIONS.keySet();
    public static final DefinitionCatalog DISCONNECTED_CATALOG = DefinitionCatalog.load(
            RESOURCE_ROOT + "disconnected-definitions.tsv",
            FORCED_DISCONNECTED_BLOCK_COUNT,
            DISCONNECTED_DEFINITIONS_SHA256
    );
    public static final Map<String, RechiseledCreateDefinition> DISCONNECTED_DEFINITIONS =
            DISCONNECTED_CATALOG.definitions();
    public static final Set<String> FORCED_DISCONNECTED_BLOCKS =
            DISCONNECTED_DEFINITIONS.keySet();
    public static final Set<String> ROUTED_BLOCKS = routedBlocks();
    public static final ResourceManifest RESOURCES = ResourceManifest.load(
            RESOURCE_ROOT + "required-resources.tsv",
            RESOURCE_COUNT,
            RESOURCES_SHA256,
            Set.of("rechiseledcreate", "rechiseled")
    );
    public static final TextureCatalog TEXTURES = TextureCatalog.load(
            RESOURCE_ROOT + "textures.tsv",
            TEXTURE_COUNT,
            TEXTURES_SHA256
    );
    public static final ResourceManifest HOST_MODELS = ResourceManifest.load(
            RESOURCE_ROOT + "host-models.tsv",
            HOST_MODEL_COUNT,
            HOST_MODELS_SHA256,
            Set.of("minecraft")
    );
    public static final ResourceManifest CHISEL_RESOURCES = ResourceManifest.load(
            RESOURCE_ROOT + "mechanical-chisel-resources.tsv",
            CHISEL_RESOURCE_COUNT,
            CHISEL_RESOURCES_SHA256,
            Set.of("rechiseledcreate", "rechiseled", "create")
    );
    public static final TextureCatalog CHISEL_TEXTURES = TextureCatalog.load(
            RESOURCE_ROOT + "mechanical-chisel-textures.tsv",
            CHISEL_TEXTURE_COUNT,
            CHISEL_TEXTURES_SHA256
    );

    private RechiseledCreate111Fusion1312Profile() {
    }

    private static Set<String> routedBlocks() {
        LinkedHashSet<String> blocks = new LinkedHashSet<>(FUSION_ROUTED_BLOCKS);
        blocks.addAll(FORCED_DISCONNECTED_BLOCKS);
        blocks.add(MECHANICAL_CHISEL_ID);
        return Set.copyOf(blocks);
    }
}
