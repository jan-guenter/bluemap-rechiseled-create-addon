# Rechiseled: Create staging gallery

This deterministic gallery is the **owner-accepted release freeze** for
Rechiseled: Create 1.1.1. The tracked sources make the exact reviewed gallery
and its coverage contract durable without placing the gallery in the add-on
JAR or redistributing any third-party resource.

The frozen datapack ZIP is 13,516 bytes with SHA-256
`f5084c9b24d9645565b9e6708ee3ac10ea004ecacf7e2a45d6e85b3e5490ac4c`.

The exact-artifact gate currently proves a 242-block / 2,909-legal-state
bridge scope:

- 163 predicate-connected blocks;
- 78 ordinary window/quartz blocks whose Fusion sheets require the bridge to
  force disconnected tile 0;
- `rechiseledcreate:mechanical_chisel`, with all 24 legal blockstates.

The gallery has 425 logical cases and 627 unique placement commands. It covers
every routed ID at least once, both canonical FULL/SIMPLE 14-mask sets, all six
representative slab states, all 80 representative stair states, material and
layout axis witnesses, cross-shape and axis connectivity controls, all 24
mechanical-chisel states, and separate parent Rechiseled/Create plus vanilla
ownership controls. Non-straight stair targets use 64 stable supports placed
first. Two dedicated fixtures place same-ID ordinary blocks face-adjacent—one
SIMPLE pair and one FULL pair—and require both blocks in each pair to remain
forced to disconnected tile 0.

The mechanical chisel section uses fresh `setblock`-created block entities,
isolates them from kinetic sources, and writes no item, filter, recipe-progress,
or transported-item NBT. The eight UP/DOWN combinations are the vertical
dynamic-tool witnesses; every state retains a neutral zero-speed shaft.

## Generate, lint, and package

The generator requires the directory containing the four exact pinned bridge,
Rechiseled, Fusion, and Create JARs. It verifies their byte lengths, SHA-256
digests, complete 242-block blockstate roster, direct model references, state
families, and Fusion sheet census before writing anything.

```text
python3 gallery/generate.py \
  --artifact-dir /tmp/rechiseledcreate-audit/jars
python3 gallery/generate.py \
  --artifact-dir /tmp/rechiseledcreate-audit/jars --check
python3 gallery/lint.py \
  --artifact-dir /tmp/rechiseledcreate-audit/jars
gallery/package.sh \
  /tmp/rechiseledcreate-audit/jars \
  /tmp/bluemap-rechiseledcreate-gallery-scope-audit-frozen.zip
```

`SHA256SUMS` covers every generated datapack, manifest, roster, case table, and
map-config file. Packaging uses sorted paths, stripped ZIP metadata, and the
fixed DOS epoch so repeated archives are byte-identical. No third-party asset
is bundled.

## Safe staging envelope and map

All world mutations are confined to inclusive x/z `160..255`, y `99..132`.
`clear` divides that envelope into 45 fills of at most 8,192 blocks, and the
floor uses nine 1,024-block fills. The bundled
`staging/create_staging.conf` preserves the reusable `create_staging` map ID
while restricting BlueMap to the same envelope.

Primary functions are:

```text
/function rechiseledcreate_gallery:build
/function rechiseledcreate_gallery:verify
/function rechiseledcreate_gallery:pose
/function rechiseledcreate_gallery:release
```

`pose` is the stable alias for `pose_overview`. Focused review poses are
`pose_census`, `pose_masks`, `pose_states`, and `pose_bridge` in the same
namespace.

`build` verifies immediately and schedules repeat verification at 20 and 100
ticks. Each phase executes 631 block assertions and 14 counter assertions:
645 assertions per phase, 1,935 across all three phases. Require these scores:

```text
#immediate_checked = 631   #immediate_failures = 0
#20t_checked       = 631   #20t_failures       = 0
#100t_checked      = 631   #100t_failures      = 0
```

All scores use objective `rc_gallery`. `release` cancels pending delayed checks
and removes only this envelope's 36 forceloaded chunks; it does not erase the
gallery.

## Same-pod transition runbook

The reusable lab uses pod-local disposable storage. Before deployment, record
the current pod identity and exact installed hashes, then keep the same pod:
do not reapply or roll out the stale deployment template. Copy only the
candidate add-on, generated datapack ZIP, and bounded map config; restart the
Minecraft process/container in place. Abort if the pod identity changes.

After reload, run `build`, wait beyond 100 ticks, require the exact scores
above, and render only `create_staging`. Check the five review poses, including
the vertical mechanical-chisel tool states and ordinary forced-tile0 controls.
Use `release` after rendering. This runbook authorizes no production-world
operation and this repository generation step performs no cluster mutation.
