# Agent guide

This is the standalone Rechiseled: Create bridge for BlueMap. Read the
workspace and portfolio guides, this README, `docs/ARCHITECTURE.md`, and
`docs/PROVENANCE.md` before changing it.

## Boundaries

- Java 21, Minecraft 1.21.1, BlueMap 5.22 backport commit
  `9be321df995a1103808621d529eb72773e719d4d`.
- Own all and only the 242 exact `rechiseledcreate:*` block IDs: 163
  predicate-connected blocks, 78 forced-disconnected Fusion-sheet blocks, and
  `mechanical_chisel`.
- Bundle no Rechiseled: Create, Rechiseled, Fusion, or Create classes, JSON,
  models, textures, metadata, source, or binaries. Read the operator-installed
  exact resources at runtime.
- Structural resources are hash-locked. Pixel-only sheet overrides are allowed
  only at the exact layout dimensions.
- Preserve stock behavior outside the exact route. Unknown artifacts, schema,
  models, states, and resources fail closed; per-block rendering falls back
  atomically. BlueMap capacity failures propagate.
- Do not change another repository, gallery-owned files, staging, cluster,
  remotes, tags, releases, or production systems from an implementation task.

## Generated inputs

`tools/generate_profile.py` consumes the four exact artifacts documented in
README and generates only first-party factual metadata. Never hand-edit files
below `src/main/resources/bluemap-rechiseled-create/profiles/`.

## Validation

Run the exact profile verifier, Python tests, and the focused Java/checkstyle
gate from README before requesting an independent audit. Do not commit, freeze,
deploy, or publish without the task owner's explicit authorization.
