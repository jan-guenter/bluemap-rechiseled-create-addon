# Agent guide

This is the standalone Rechiseled: Create bridge for BlueMap. Read the
workspace and portfolio guides, this README, `docs/ARCHITECTURE.md`, and
`docs/PROVENANCE.md` before changing it.

## Boundaries

- Java 21, Minecraft 1.21.1, and only BlueMap feature-backport commit
  `7e07f4e74ec1e92a6ead9aa1e66054af3e133aac`, API commit
  `285c9a60eff3ac2b0cab308ce1058d1565be0971`.
- Compile the four Adapter API sources from gitlink
  `e81f08bc4bfbf02d810ec8949a019130e2e61634`, source tree
  `2f974c9bb2ba13888d69682f86f30f58922d30eb`, and the five Fusion model
  sources from released gitlink `3ddd5d39bb7cc8664c242aedd849a636316075c2`,
  source tree `6e85031ff2f0e7417a7a2fb0babbf7ed5a4f218a`. Never bundle either
  standalone module JAR.
- Own all and only the 242 exact `rechiseledcreate:*` block IDs: 163
  predicate-connected blocks, 78 forced-disconnected Fusion-sheet blocks, and
  `mechanical_chisel`.
- Bundle no Rechiseled: Create, Rechiseled, Fusion runtime, or Create classes,
  JSON, models, textures, metadata, source, or binaries. The exact pinned MIT
  Fusion resource-model sources are deliberately compiled into this add-on;
  read the operator-installed exact resources at runtime.
- Structural resources are hash-locked. Pixel-only sheet overrides are allowed
  only at the exact layout dimensions.
- Preserve stock behavior outside the exact route. Unknown artifacts, schema,
  models, states, and resources fail closed; per-block rendering falls back
  atomically. BlueMap capacity failures propagate.
- Do not change another repository, gallery-owned files, staging, cluster,
  remotes, tags, releases, or production systems from an implementation task.

The owner accepted the aggregate BlueMap 5.23 integration view for release
candidate `0.1.0-alpha.2` on 2026-08-31. Its exact production JAR is 213,503
bytes with SHA-256
`8e1c5709698e1a2a8313935a4ac138eefc5465c2ec885526b714c706705c99f8`.
Publication is authorized; production deployment remains excluded.

## Generated inputs

`tools/generate_profile.py` consumes the four exact artifacts documented in
README and generates only first-party factual metadata. Never hand-edit files
below `src/main/resources/bluemap-rechiseled-create/profiles/`.

## Validation

Run the exact profile verifier, Python tests, and the focused Java/checkstyle
gate from README before requesting an independent audit. Do not commit, freeze,
deploy, or publish without the task owner's explicit authorization.
