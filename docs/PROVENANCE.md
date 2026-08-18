# Provenance

## Implementation lineage

The immediate foundation is the owner-authored MIT BlueMap Rechiseled Add-on
tree at commit `a5530b3178022c2ba755c3d275debc6adcd47e42`. Its released
Fusion interpreter, resource validation, geometry emission, activation, and
test scaffold were retargeted to the independent `rechiseledcreate` namespace.

Mechanical shaft and frozen-part transform patterns reuse owner-authored MIT
work from the BlueMap Create Add-on. The bridge-specific route tables,
forced-disconnected behavior, mechanical-chisel projection, and item-sprite
extrusion are independently authored for this project.

Geometry, UV-lock, lighting, AO, culling, cave, top-only, map-color, random
offset, and model-selection mechanics in `FusionModelEmitter` adapt BlueMap
5.22's MIT `ResourceModelRenderer` at backport commit
`9be321df995a1103808621d529eb72773e719d4d`. The affected file retains the
BlueMap copyright and permission notice; the complete notice is in
`LICENSE-BlueMap` and both published JAR boundaries.

## Runtime inputs

Rechiseled: Create, Rechiseled, and Fusion declare All rights reserved. Create
code is MIT while its installed assets retain their upstream terms. All four
JARs are operator-installed runtime and verification inputs only. This project
bundles no upstream code, classes, JSON, models, textures, metadata, source, or
binaries.

The official Rechiseled: Create branch commit
`5d00bf7d45c5e502d7fd4222029451ebd5ba3fce` is version-correlated reference
evidence for 1.1.1 and matches the full 1,038-path bridge closure semantically;
it is not a reproducible-source or code-reuse claim. Rechiseled commit
`e9e806c3ef3d0277a006e7fc9de4fff74d34dcd7`, Fusion commit
`bace466e1c4f116ff2df535aadab690c81160a0e`, and Create tag
`mc1.21.1-6.0.10` commit `ac0c444d9828da3453ae8cc65338e8de063286fb`
are reference-only semantic corroboration.

## Generated evidence

`tools/generate_profile.py` verifies SHA-1, SHA-256, SHA-512, filename, and
size for all four artifacts. It independently regenerates only factual route
and resource metadata. Exact runtime bytes remain outside the repository and
the production/source JARs. The machine-readable record is
`provenance/upstreams.json`.

## Release freeze

The owner-accepted implementation commit, production JAR, release sidecars,
and frozen gallery identities are recorded separately in
`provenance/release.json`. The implementation-time `upstreams.json` is already
packaged in the accepted JAR and intentionally remains byte-frozen; changing
it would change the accepted production artifact. Neither manifest contains
or redistributes third-party resources.
