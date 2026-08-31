# Architecture

## Shared source boundaries

The production JAR compiles two exact-pinned MIT source sets. Adapter API
`0.1.0-alpha.2` supplies only the BlueMap 5.23 runtime identity, registry,
resource-extension, and synthetic-dispatch helpers. Fusion Resource Models
`0.1.0-alpha.1` supplies only the five pure geometry/selector model sources.
The bridge keeps all Rechiseled: Create predicates, resource loading,
registration, activation, mechanical-chisel logic, and rendering. Neither
support-module JAR is a runtime dependency, and no 5.22 render-core module is
used.

## Lifecycle

```text
preflight renderer and resource-extension registry IDs
  -> detect the exact four-artifact tuple
  -> validate the active first-wins resource closures
  -> compile 432 bounded Fusion programs
  -> route exactly 242 rechiseledcreate IDs through one synthetic dispatch
  -> crop 3,056 collision-safe logical tile textures
  -> emit installed model geometry or stable mechanical multipart geometry
```

The process route begins inactive. Tuple, schema, closure, host ABI, synthetic
key, or texture-key collisions leave it inactive. Operator disablement uses
`bluemap.rechiseledcreate.disabledProfiles` or
`BLUEMAP_RECHISELEDCREATE_DISABLED_PROFILES` with profile ID
`rechiseledcreate-1.1.1-atm-1.2.0`.

## Generated profile

The generator consumes four exact JARs and emits identities, hashes,
allowlists, counts, dimensions, and layout names only. It locks 163 connected
and 78 forced-disconnected block definitions, all 2,885 non-chisel legal
states, 241 blockstates, 510 direct bridge models, three transitive Rechiseled
parents, 180 PNGs, and 107 metadata files. The 24 mechanical states and a
separate 16-path installed-resource closure cover the remaining block. Nine
hash-locked vanilla host models include both the block parents and the
`item/handheld` to `item/generated` chain used by the frozen tool extrusion.

The 432 custom Fusion models use only `or`, `and`, `is_direction`,
`match_block`, `match_state`, and `is_same_state`. Empty OR lists are exact
false placeholders in the bridge resources. Alias and parent resolution are
bounded and cycle checked.

## Fusion routes

Connected blocks evaluate the eight-neighbor mask in final transformed texture
space. Forced-disconnected blocks use the same original model geometry and
sheet metadata but hard-select mask 0. Both routes preserve face UVs, element
and variant transforms, UV lock, cullface, tint, light, AO, cave removal,
top-only filtering, random offsets, and map color.

The exact texture inventory is 73 plain 16×16 textures, 65 SIMPLE 4×4 sheets,
and 42 FULL logical 8×6 sheets stored in 128×128 images. FULL tile 41 is
unreachable. Cropped output keys live only below
`bluemap_rechiseled_create:tiles/`.

## Mechanical chisel

The stock blockstate selects Create's horizontal or vertical mechanical-saw
housing. The bridge invokes that original variant, then appends installed
Create shaft geometry. Horizontal facings use a half-shaft toward the opposite
face; vertical facings use a full shaft on the axis selected by
`axis_along_first`.

Vertical states also extrude the installed 16×16 `rechiseled:item/chisel`
sprite with vanilla generated-item depth and alpha-boundary sides. The neutral
pose preserves the exact facing, `axis_along_first`, and `flipped` transforms.
The active schema verifies the exact item model, handheld/generated parents,
layer-0 binding, and fixed transform before this hardcoded stable lane is
enabled. No inventory or moving state is read.

## Failure semantics

Per-block rendering records its geometry start and map color. Malformed input
or missing installed resources reset partial output and invoke the original
pre-extension stock blockstate. BlueMap capacity failures propagate. The add-on
registers no mod blocks, items, packets, mixins, client hooks, or upstream
namespace resources.
