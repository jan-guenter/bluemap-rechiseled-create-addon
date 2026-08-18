# BlueMap Rechiseled: Create Add-on

[![CI](https://github.com/jan-guenter/bluemap-rechiseled-create-addon/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/jan-guenter/bluemap-rechiseled-create-addon/actions/workflows/ci.yml)

This independent MIT BlueMap add-on interprets the models installed by
Rechiseled: Create 1.1.1, Rechiseled 1.2.5, Fusion 1.3.12, and Create 6.0.10
on the exact All the Mons 1.2.0 baseline.

Version `0.1.0-alpha.1` is the owner-accepted prerelease. Its production JAR
is exactly 209,444 bytes with SHA-256
`651b8be8a41d459f04f3ec1cbc64ba7441cceb3c2bb3c07d518556a11c83596b`.
Compatibility outside the exact inputs below is not asserted.

## Exact contract

Activation requires all four byte-exact operator-installed artifacts:

- `rechiseledcreate-1.1.1-neoforge-mc1.21.jar`, 983,177 bytes, SHA-256
  `ba89cd5d1221621ed226cc7f1c26dc84a660cc4f6d122753052429f96d71248d`;
- `rechiseled-1.2.5-neoforge-mc1.21.jar`, 11,498,611 bytes, SHA-256
  `7bf14cf8a4bfdc4b6c990126a75da29fd2bb7559d1c05b71e29c8fd5ae044435`;
- `fusion-1.3.12-neoforge-mc1.21.1.jar`, 923,270 bytes, SHA-256
  `17f5215648a98bcde4134577b013200dbf363273ae282449c51408ae8346f2fa`;
- `create-1.21.1-6.0.10.jar`, 19,123,767 bytes, SHA-256
  `ef87fe5709f1ba1f5b8bb20a2925b5afb4669e178fd6d8bf10c167759eefe37a`.

The bridge owns all 242 `rechiseledcreate:*` block IDs and 2,909 legal states:

| Route | IDs | Legal states |
| --- | ---: | ---: |
| Predicate-connected Fusion models | 163 | 2,661 |
| Forced-disconnected Fusion sheets | 78 | 224 |
| Mechanical chisel stable multipart | 1 | 24 |
| **Total** | **242** | **2,909** |

The 78 ordinary-looking window and rose-quartz blocks are deliberately routed.
Their exact models reference Fusion sheets whose `connections:{type:false}`
metadata selects disconnected tile 0; stock BlueMap would stretch the full
64×64 or 128×128 sheet.

## Rendering behavior

The connected lane reuses the independently authored MIT interpreter from the
released BlueMap Rechiseled Add-on. It preserves selected blockstate geometry,
variant/model transforms, UV lock, partial UVs, culling, AO, lighting, alpha,
map color, and bounded Fusion predicates. It supports only the exact predicate
types and layouts present in the installed bridge resources. The
forced-disconnected lane renders the same model geometry while selecting mask
0 regardless of neighbors.

The mechanical chisel lane keeps the stock Create saw housing and appends the
stable physical pieces omitted by stock BlueMap: a neutral shaft on every
facing, plus the fixed empty chisel tool on vertical UP/DOWN states. Transported
items, filters, progress, speed, and animation are intentionally excluded.

The active Fusion closure is 241 blockstates, 513 models, 180 PNGs, and 107
metadata files: 1,038 bridge-owned paths plus three exact Rechiseled stair
parents. The separate chisel closure contains 16 installed resources, and the
nine-model host ABI includes the exact vanilla generated-item parent chain.
No third-party bytes are bundled.

An unknown tuple, structural resource, predicate, selector, model, texture
dimension, state, or registry collision leaves the route inactive or falls
back atomically to the original BlueMap renderer. Capacity exhaustion is not
contained.

## Focused build and review

Java 21, Gradle 9.6.1, and the exact local BlueMap backport checkout are
required.

```bash
python3 tools/verify_pinned_artifacts.py \
  --bridge /absolute/path/rechiseledcreate-1.1.1-neoforge-mc1.21.jar \
  --rechiseled /absolute/path/rechiseled-1.2.5-neoforge-mc1.21.jar \
  --fusion /absolute/path/fusion-1.3.12-neoforge-mc1.21.1.jar \
  --create /absolute/path/create-1.21.1-6.0.10.jar
python3 -m unittest discover -s tools/tests -p 'test_*.py'
gradle --no-daemon \
  -PbridgeJar=/absolute/path/rechiseledcreate-1.1.1-neoforge-mc1.21.jar \
  -PrechiseledJar=/absolute/path/rechiseled-1.2.5-neoforge-mc1.21.jar \
  -PfusionJar=/absolute/path/fusion-1.3.12-neoforge-mc1.21.1.jar \
  -PcreateJar=/absolute/path/create-1.21.1-6.0.10.jar \
  clean check build generatePomFileForAddonPublication \
  generateMetadataFileForAddonPublication verifyPinnedArtifacts
```

`check` rejects a production JAR that differs from the accepted size or
SHA-256. The accepted implementation passed 36 Java tests and five Python
tests. Tagged releases publish the production and source JARs, POM, Gradle
module metadata, and checksums at Maven coordinate
`io.github.jan-guenter:bluemap-rechiseled-create-addon:<version>`. The tag must
equal `v<addon_version>`.

## Gallery

The tracked `gallery/` tree freezes 425 logical cases, 627 unique placements,
all 242 routed IDs, representative connected masks and shape/state controls,
all 24 mechanical-chisel states, and parent ownership controls. Its
reproducible datapack ZIP is exactly 13,516 bytes with SHA-256
`f5084c9b24d9645565b9e6708ee3ac10ea004ecacf7e2a45d6e85b3e5490ac4c`.
It contains commands and first-party metadata only, not mod assets. See
`gallery/README.md` for generation and bounded staging instructions.

## Installation

Place the reviewed add-on JAR in `config/bluemap/packs` and restart the JVM.
It is a BlueMap add-on, not a NeoForge mod, and does not belong in the server's
`mods` directory. The four exact compatible mod JARs remain operator-installed
runtime inputs. The add-on writes no world or player data.

## Licensing

Project code and generated factual metadata are MIT. The four mod artifacts are
operator-installed inputs only; none of their code, classes, JSON, models,
textures, metadata, source, or binaries are redistributed. BlueMap-derived MIT
renderer mechanics retain attribution. See `LICENSE-BlueMap`,
`THIRD_PARTY.md`, `docs/PROVENANCE.md`, and `provenance/release.json`.
