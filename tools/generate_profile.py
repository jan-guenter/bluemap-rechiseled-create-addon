#!/usr/bin/env python3
# SPDX-License-Identifier: MIT
"""Generate the metadata-only exact Rechiseled: Create/Fusion profile.

The operator supplies the four hash-pinned ATM 1.2.0 runtime artifacts. The
generator emits only identities, hashes, allowlists, counts, dimensions and
layout names. It never copies third-party JSON, models, textures, classes or
binaries into the add-on.
"""

from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path, PurePosixPath
import struct
from typing import Any, Iterable
import zipfile


PROFILE_ROOT = Path("src/main/resources/bluemap-rechiseled-create/profiles")
PROFILE_DIRECTORY = (
    PROFILE_ROOT
    / "rechiseledcreate/1.1.1-rechiseled-1.2.5-fusion-1.3.12-create-6.0.10"
)
CATALOG_PATH = PROFILE_ROOT / "exact-artifacts.json"
PROFILE_PATH = PROFILE_DIRECTORY / "profile.json"
DEFINITIONS_PATH = PROFILE_DIRECTORY / "definitions.tsv"
DISCONNECTED_DEFINITIONS_PATH = PROFILE_DIRECTORY / "disconnected-definitions.tsv"
RESOURCES_PATH = PROFILE_DIRECTORY / "required-resources.tsv"
TEXTURES_PATH = PROFILE_DIRECTORY / "textures.tsv"
HOST_MODELS_PATH = PROFILE_DIRECTORY / "host-models.tsv"
CHISEL_RESOURCES_PATH = PROFILE_DIRECTORY / "mechanical-chisel-resources.tsv"
CHISEL_TEXTURES_PATH = PROFILE_DIRECTORY / "mechanical-chisel-textures.tsv"

BRIDGE_FILENAME = "rechiseledcreate-1.1.1-neoforge-mc1.21.jar"
BRIDGE_SIZE = 983_177
BRIDGE_SHA1 = "6f7e53f39eb477d7cee8e21af215cd3db698deda"
BRIDGE_SHA256 = "ba89cd5d1221621ed226cc7f1c26dc84a660cc4f6d122753052429f96d71248d"
BRIDGE_SHA512 = (
    "ca77dea4dd3276105176578855b0ffd9f48256bed58701a86171b05db1116a295"
    "92871ca7d6d5acc9a023270a45654feb4ab7153026eb751d567e7bf892ca560"
)
RECHISELED_FILENAME = "rechiseled-1.2.5-neoforge-mc1.21.jar"
RECHISELED_SIZE = 11_498_611
RECHISELED_SHA1 = "ed2973c6952caa3173259314276b5b9d72880494"
RECHISELED_SHA256 = "7bf14cf8a4bfdc4b6c990126a75da29fd2bb7559d1c05b71e29c8fd5ae044435"
RECHISELED_SHA512 = (
    "d849bc3e775577978bbf96dccee11b0904fa928c556a12e05adf759edab9479bd"
    "757f490380475e974eaa137c566ffe8bfb62d5df19f966dfd99b67a2fe0ee9b"
)
FUSION_FILENAME = "fusion-1.3.12-neoforge-mc1.21.1.jar"
FUSION_SIZE = 923_270
FUSION_SHA1 = "79c0c6b6a2d9c9a04298df9a88bb71a93e885235"
FUSION_SHA256 = "17f5215648a98bcde4134577b013200dbf363273ae282449c51408ae8346f2fa"
FUSION_SHA512 = (
    "a13d2a654988f021106f8a455134da1b515872e9122cf70bda064e663749f1c11"
    "aeddc3def23a621e236f2ffeefb6f56b15c2c63eeb2bca5f9833c5a2dc23a93"
)
CREATE_FILENAME = "create-1.21.1-6.0.10.jar"
CREATE_SIZE = 19_123_767
CREATE_SHA1 = "0e97e49837bed766e6f28a4c95b04885d6acc353"
CREATE_SHA256 = "ef87fe5709f1ba1f5b8bb20a2925b5afb4669e178fd6d8bf10c167759eefe37a"
CREATE_SHA512 = (
    "11cc8fc049d2f67f6548c7abfada6b82a3adb5c7ca410a742de04bbca76e03862"
    "c518721b88d806f6e6d768a4d68531fdb903a85859b25d1484d550cc7bafd4b"
)

ALL_BLOCKSTATES_COUNT = 242
FUSION_ROUTED_COUNT = 163
DISCONNECTED_ROUTED_COUNT = 78
FUSION_ROUTED_STATE_COUNT = 2_661
DISCONNECTED_ROUTED_STATE_COUNT = 224
MECHANICAL_CHISEL_STATE_COUNT = 24
NON_CHISEL_ROUTED_COUNT = FUSION_ROUTED_COUNT + DISCONNECTED_ROUTED_COUNT
NON_CHISEL_ROUTED_STATE_COUNT = (
    FUSION_ROUTED_STATE_COUNT + DISCONNECTED_ROUTED_STATE_COUNT
)
ROUTED_COUNT = NON_CHISEL_ROUTED_COUNT + 1
ROUTED_STATE_COUNT = NON_CHISEL_ROUTED_STATE_COUNT + MECHANICAL_CHISEL_STATE_COUNT
CUSTOM_MODEL_COUNT = 432
DIRECT_MODEL_COUNT = 510
MODEL_COUNT = 513
PNG_COUNT = 180
MCMETA_COUNT = 107
BRIDGE_RESOURCE_COUNT = 1_038
RESOURCE_COUNT = 1_041
HOST_MODEL_COUNT = 9
CHISEL_RESOURCE_COUNT = 16
CHISEL_TEXTURE_COUNT = 9
CONNECTED_ROSTER_DIGEST = "6f9310b3166c4946d25a71461abf538eee7d65909f73326e4a434f0259ac2427"
DISCONNECTED_ROSTER_DIGEST = (
    "22cf503d5e0cff2fe560262a7d0c9fb92f47501a3ebcaec46f0e83ae37b52ef7"
)
ROUTED_DIGEST = "107fe5e07bf81062245408be8e3cb9e8b84e479a2d6eff8f4bb4341e18457747"
CONNECTED_LEGAL_STATE_DIGEST = (
    "a9b43dd48984f82d691d68678d583852d5c34ca09c0a07db2ba512ca85d98daf"
)
DISCONNECTED_LEGAL_STATE_DIGEST = (
    "937cb1781ba4a2b0a2613f6611cf3fdf448b15d88dd57c5a3cd9cb415661f70c"
)
LEGAL_STATE_DIGEST = "7b76090d1635400a1a4669bd7c7deb007e342334ac0223db42b48eccf0f6d6ff"
CONNECTED_DIRECT_MODEL_DIGEST = (
    "ab56bf6019396c4ebc85bcdbf8e3119748e2d091731d4991cd0de50cb7771e6e"
)
DISCONNECTED_DIRECT_MODEL_DIGEST = (
    "30638ac91f5c3103c7b46340c48845d46c8505f560175174bbc26bb156684a28"
)
DISCONNECTED_SHEET_DIGEST = (
    "5a5a6a3eadce26b480c6c0ce38077c8f850eb5e2830bc586e250a38b4f14bdcb"
)
DIRECT_MODEL_DIGEST = "b13339c3585e73a56684fad1c8b8a6864cd6ff4622d1ac0c16a26acdde27caec"
MODEL_DIGEST = "502f78b5b42d94c9dd549293ed89b161346411e8345c4355259c46aa17a03eb9"
PNG_DIGEST = "507c4fdaeac67cc6edbcd181767a999fe818a384ad5fe7bba1c3c90c95de35f5"
MCMETA_DIGEST = "3a5d591130fb28a7bd5cecb95368b3982d37a3c2070f727214c40cfa9e4a8139"
PATH_CLOSURE_DIGEST = "b3e8603aabcb54f4fc51f36b3396880bd03ca0cd51fc60beec0e15e4fd701fb5"
EXTERNAL_MODELS_DIGEST = "bbde9719ff5532cc76289c77f5eb96d1057cf1b518d78e69a7c1a204cfd69286"
MECHANICAL_CHISEL_STATE_DIGEST = (
    "3559dec895fd98172d43fd337a8063976ec5de51712d9e69c5db2be7f535dbba"
)
MINECRAFT_CLIENT_SHA256 = (
    "499f6897d1837516680f3114072d8106e11c9adcd933fe5cf051b551089b0c99"
)

HOST_MODELS = (
    ("assets/minecraft/models/block/block.json", 997,
     "3ef6c442f1ab55d2a57fa58e28bb831268159052659f12b453b637b31ded1da8"),
    ("assets/minecraft/models/block/cube.json", 584,
     "3e4aacd02e816aeba38f83076596e18ded4cf49c01e17c62d1fce79850ffb84e"),
    ("assets/minecraft/models/block/inner_stairs.json", 1_755,
     "fcb56ce59da95e5c1a77e49149caa3c72c18195141554a2c063d24b7962648d8"),
    ("assets/minecraft/models/block/outer_stairs.json", 1_271,
     "39142eb37d9e9d9ff2404404af460d85b3109bf0c59898c21c676719c3e16ef8"),
    ("assets/minecraft/models/block/slab.json", 761,
     "bd869ebe3ba380d46349e5c6e988b9b1ccf1ab25212ab1de66e2fdcc067edc1d"),
    ("assets/minecraft/models/block/slab_top.json", 733,
     "c02e81cd0b59698040db7a682d32d08ddeb0de64756e309d62ecfbda4af804f9"),
    ("assets/minecraft/models/block/stairs.json", 1_806,
     "962dc154fd3337d6b7e165e2b734e171c4b3595c2b838a5da1e01f5bdcdcae3b"),
    ("assets/minecraft/models/item/generated.json", 813,
     "63190611202e75ad7b812bc9a4b31bd53f3e186895d7b53e5e280b0d1e30a67a"),
    ("assets/minecraft/models/item/handheld.json", 752,
     "8fe990d1c011429e27df2d49d35cf8571946370eac8d0fb7497aacede4ff20f5"),
)

EXPECTED_LAYOUTS = {
    "plain": (73, 16, 16),
    "full": (42, 128, 128),
    "simple": (65, 64, 64),
}
PREDICATE_TYPES = {
    "fusion:or",
    "fusion:and",
    "fusion:is_direction",
    "fusion:match_block",
    "fusion:match_state",
    "fusion:is_same_state",
}
MODEL_NAMESPACES = {"rechiseledcreate", "rechiseled"}
CHISEL_MODELS = (
    "assets/create/models/block/block.json",
    "assets/create/models/block/mechanical_saw/horizontal.json",
    "assets/create/models/block/mechanical_saw/vertical.json",
    "assets/create/models/block/shaft.json",
    "assets/create/models/block/shaft_half.json",
    "assets/rechiseled/models/item/chisel.json",
)
CHISEL_TEXTURES = (
    "assets/create/textures/block/andesite_casing_short.png",
    "assets/create/textures/block/axis.png",
    "assets/create/textures/block/axis_top.png",
    "assets/create/textures/block/encased_chain_drive.png",
    "assets/create/textures/block/gearbox.png",
    "assets/create/textures/block/gearbox_top.png",
    "assets/create/textures/block/mechanical_saw_top.png",
    "assets/create/textures/block/mechanical_saw_top_no_slot.png",
    "assets/rechiseled/textures/item/chisel.png",
)


def digest_bytes(raw: bytes, algorithm: str = "sha256") -> str:
    return hashlib.new(algorithm, raw).hexdigest()


def digest_path(path: Path, algorithm: str) -> str:
    value = hashlib.new(algorithm)
    with path.open("rb") as source:
        for chunk in iter(lambda: source.read(64 * 1024), b""):
            value.update(chunk)
    return value.hexdigest()


def roster_digest(values: Iterable[str]) -> str:
    payload = "".join(f"{value}\n" for value in sorted(values)).encode("utf-8")
    return digest_bytes(payload)


def canonical_json(value: Any) -> bytes:
    return (json.dumps(value, ensure_ascii=True, sort_keys=True, indent=2) + "\n").encode(
        "ascii"
    )


def resource_path(key: str, kind: str, suffix: str) -> str:
    if ":" in key:
        namespace, value = key.split(":", 1)
    else:
        namespace, value = "minecraft", key
    path = PurePosixPath(value)
    if path.is_absolute() or any(part in {"", ".", ".."} for part in path.parts):
        raise ValueError(f"unsafe resource key: {key}")
    return f"assets/{namespace}/{kind}/{value}{suffix}"


def _verify_identity(
    path: Path, *, filename: str, size: int, sha1: str, sha256: str, sha512: str
) -> None:
    if not path.is_file() or path.name != filename:
        raise ValueError(f"unexpected artifact path: {path}")
    if path.stat().st_size != size:
        raise ValueError(f"unexpected artifact size for {path}")
    for algorithm, expected in (
        ("sha1", sha1),
        ("sha256", sha256),
        ("sha512", sha512),
    ):
        actual = digest_path(path, algorithm)
        if actual != expected:
            raise ValueError(
                f"{path.name} {algorithm} changed: got {actual}, expected {expected}"
            )


def _model_key(value: str, default_namespace: str) -> str:
    return value if ":" in value else f"{default_namespace}:{value}"


def _path_for_model(model: str) -> str:
    namespace, value = model.split(":", 1)
    return f"assets/{namespace}/models/{value}.json"


def _path_for_texture(texture: str, suffix: str = ".png") -> str:
    namespace, value = texture.split(":", 1)
    return f"assets/{namespace}/textures/{value}{suffix}"


def _walk_predicate(value: Any, found: set[str]) -> None:
    if isinstance(value, dict):
        predicate_type = value.get("type")
        if isinstance(predicate_type, str) and predicate_type.startswith("fusion:"):
            found.add(predicate_type)
        for child in value.values():
            _walk_predicate(child, found)
    elif isinstance(value, list):
        for child in value:
            _walk_predicate(child, found)


def _shape_and_states(block: str, variants: dict[str, Any]) -> tuple[str, int, list[str]]:
    selector_keys = sorted(variants)
    if block.endswith("_slab_connecting"):
        if {key.split("=", 1)[0] for key in selector_keys} != {"type"}:
            raise ValueError(f"{block} slab selectors changed")
        shape, count = "slab", 6
    elif block.endswith("_stairs_connecting"):
        properties = {
            assignment.split("=", 1)[0]
            for key in selector_keys
            for assignment in key.split(",")
        }
        if properties != {"facing", "half", "shape"}:
            raise ValueError(f"{block} stair selectors changed")
        shape, count = "stairs", 80
    elif selector_keys == [""]:
        shape, count = "full", 1
    elif selector_keys == ["axis=x", "axis=y", "axis=z"]:
        shape, count = "axis", 3
    else:
        raise ValueError(f"{block} has an unsupported selector schema")
    return shape, count, selector_keys


def _legal_states(block_id: str, shape: str, selectors: list[str]) -> list[str]:
    rows: list[str] = []
    if shape in {"slab", "stairs"}:
        for selector in selectors:
            for waterlogged in ("false", "true"):
                properties = selector.split(",") + [f"waterlogged={waterlogged}"]
                rows.append(f"{block_id}\t{','.join(sorted(properties))}")
    else:
        for selector in selectors:
            properties = [] if selector == "" else selector.split(",")
            rows.append(f"{block_id}\t{','.join(sorted(properties))}")
    return rows


def _artifact_record(
    mod_id: str,
    version: str,
    filename: str,
    size: int,
    sha1: str,
    sha256: str,
    sha512: str,
) -> dict[str, Any]:
    return {
        "modId": mod_id,
        "version": version,
        "filename": filename,
        "size": size,
        "sha1": sha1,
        "sha256": sha256,
        "sha512": sha512,
    }


def build_outputs(
    bridge: Path, rechiseled: Path, fusion: Path, create: Path
) -> dict[Path, bytes]:
    for path, values in (
        (bridge, (BRIDGE_FILENAME, BRIDGE_SIZE, BRIDGE_SHA1, BRIDGE_SHA256, BRIDGE_SHA512)),
        (rechiseled, (RECHISELED_FILENAME, RECHISELED_SIZE, RECHISELED_SHA1,
                      RECHISELED_SHA256, RECHISELED_SHA512)),
        (fusion, (FUSION_FILENAME, FUSION_SIZE, FUSION_SHA1, FUSION_SHA256,
                  FUSION_SHA512)),
        (create, (CREATE_FILENAME, CREATE_SIZE, CREATE_SHA1, CREATE_SHA256, CREATE_SHA512)),
    ):
        _verify_identity(
            path,
            filename=values[0],
            size=values[1],
            sha1=values[2],
            sha256=values[3],
            sha512=values[4],
        )

    definitions: list[str] = []
    disconnected_definitions: list[str] = []
    legal_states: list[str] = []
    disconnected_legal_states: list[str] = []
    routed_paths: list[str] = []
    disconnected_paths: list[str] = []
    direct_models: set[str] = set()
    connected_direct_models: set[str] = set()
    disconnected_direct_models: set[str] = set()
    disconnected_model_textures: dict[str, set[str]] = {}
    model_paths: set[str] = set()
    texture_keys: set[str] = set()
    png_paths: set[str] = set()
    metadata_paths: set[str] = set()
    predicate_types: set[str] = set()
    layouts: dict[str, tuple[str, int, int, str]] = {}
    shape_counts: dict[str, int] = {"full": 0, "axis": 0, "slab": 0, "stairs": 0}
    disconnected_shape_counts: dict[str, int] = {
        "full": 0, "axis": 0, "slab": 0, "stairs": 0
    }

    with zipfile.ZipFile(bridge) as bridge_archive, zipfile.ZipFile(
        rechiseled
    ) as rechiseled_archive, zipfile.ZipFile(create) as create_archive:
        archives = {
            "rechiseledcreate": bridge_archive,
            "rechiseled": rechiseled_archive,
            "create": create_archive,
        }
        available: dict[str, set[str]] = {}
        for namespace, archive in archives.items():
            names = archive.namelist()
            if len(names) != len(set(names)):
                raise ValueError(f"{namespace} JAR contains duplicate ZIP entries")
            available[namespace] = set(names)

        blockstate_paths = sorted(
            path
            for path in available["rechiseledcreate"]
            if path.startswith("assets/rechiseledcreate/blockstates/")
            and path.endswith(".json")
        )
        if len(blockstate_paths) != ALL_BLOCKSTATES_COUNT:
            raise ValueError("Rechiseled: Create blockstate count changed")

        chisel_path = "assets/rechiseledcreate/blockstates/mechanical_chisel.json"
        chisel_state = json.loads(bridge_archive.read(chisel_path))
        if set(chisel_state) != {"variants"} or not isinstance(
            chisel_state["variants"], dict
        ):
            raise ValueError("mechanical chisel blockstate schema changed")
        chisel_rows: list[str] = []
        chisel_models: set[str] = set()
        expected_faces = {"down", "east", "north", "south", "up", "west"}
        observed_faces: set[str] = set()
        observed_axes: set[str] = set()
        observed_flips: set[str] = set()
        for selector, variant in chisel_state["variants"].items():
            properties = dict(part.split("=", 1) for part in selector.split(","))
            if set(properties) != {"axis_along_first", "facing", "flipped"}:
                raise ValueError("mechanical chisel selector schema changed")
            observed_faces.add(properties["facing"])
            observed_axes.add(properties["axis_along_first"])
            observed_flips.add(properties["flipped"])
            if not isinstance(variant, dict) or not set(variant).issubset(
                {"model", "x", "y"}
            ):
                raise ValueError("mechanical chisel variant schema changed")
            model = variant.get("model")
            if not isinstance(model, str):
                raise ValueError("mechanical chisel model changed")
            chisel_models.add(model)
            for angle in (variant.get("x", 0), variant.get("y", 0)):
                if not isinstance(angle, int) or angle % 90 != 0:
                    raise ValueError("mechanical chisel variant angle changed")
            chisel_rows.append(
                "rechiseledcreate:mechanical_chisel\t"
                + ",".join(sorted(selector.split(",")))
            )
        if (
            len(chisel_rows) != MECHANICAL_CHISEL_STATE_COUNT
            or observed_faces != expected_faces
            or observed_axes != {"false", "true"}
            or observed_flips != {"false", "true"}
            or chisel_models
            != {
                "create:block/mechanical_saw/horizontal",
                "create:block/mechanical_saw/vertical",
            }
            or roster_digest(chisel_rows) != MECHANICAL_CHISEL_STATE_DIGEST
        ):
            raise ValueError("mechanical chisel legal-state roster changed")

        for path in blockstate_paths:
            block = path.removeprefix(
                "assets/rechiseledcreate/blockstates/"
            ).removesuffix(".json")
            if block == "mechanical_chisel":
                continue
            connected = block.endswith("_connecting")
            if connected:
                routed_paths.append(path)
            else:
                disconnected_paths.append(path)
            value = json.loads(bridge_archive.read(path))
            if set(value) != {"variants"} or not isinstance(value["variants"], dict):
                raise ValueError(f"{path} blockstate schema changed")
            shape, state_count, selectors = _shape_and_states(block, value["variants"])
            (shape_counts if connected else disconnected_shape_counts)[shape] += 1
            variant_models: set[str] = set()
            for variant in value["variants"].values():
                variants = variant if isinstance(variant, list) else [variant]
                if len(variants) != 1 or not isinstance(variants[0], dict):
                    raise ValueError(f"{path} weighted variant changed")
                model = variants[0].get("model")
                if not isinstance(model, str):
                    raise ValueError(f"{path} model selector changed")
                variant_models.add(_model_key(model, "rechiseledcreate"))
            direct_models.update(variant_models)
            (connected_direct_models if connected else disconnected_direct_models).update(
                variant_models
            )
            block_id = f"rechiseledcreate:{block}"
            (legal_states if connected else disconnected_legal_states).extend(
                _legal_states(block_id, shape, selectors)
            )
            (definitions if connected else disconnected_definitions).append(
                "\t".join(
                    (
                        block_id,
                        shape,
                        str(state_count),
                        digest_bytes(bridge_archive.read(path)),
                        digest_bytes(
                            "".join(
                                f"{model}\n" for model in sorted(variant_models)
                            ).encode("ascii")
                        ),
                    )
                )
            )

        if len(routed_paths) != FUSION_ROUTED_COUNT or roster_digest(
            routed_paths
        ) != CONNECTED_ROSTER_DIGEST:
            raise ValueError("Rechiseled: Create routed roster changed")
        if len(disconnected_paths) != DISCONNECTED_ROUTED_COUNT or roster_digest(
            disconnected_paths
        ) != DISCONNECTED_ROSTER_DIGEST:
            raise ValueError("Rechiseled: Create forced-disconnected roster changed")
        if roster_digest(routed_paths + disconnected_paths) != ROUTED_DIGEST:
            raise ValueError("Rechiseled: Create full routed roster changed")
        if len(connected_direct_models) != CUSTOM_MODEL_COUNT or roster_digest(
            connected_direct_models
        ) != CONNECTED_DIRECT_MODEL_DIGEST:
            raise ValueError("connected Fusion direct-model roster changed")
        if len(disconnected_direct_models) != DISCONNECTED_ROUTED_COUNT or roster_digest(
            disconnected_direct_models
        ) != DISCONNECTED_DIRECT_MODEL_DIGEST:
            raise ValueError("forced-disconnected direct-model roster changed")
        if len(direct_models) != DIRECT_MODEL_COUNT or roster_digest(
            direct_models
        ) != DIRECT_MODEL_DIGEST:
            raise ValueError("Rechiseled: Create direct Fusion model roster changed")

        pending = list(direct_models)
        visited_models: set[str] = set()
        external_models: set[str] = set()
        while pending:
            model = pending.pop()
            if model in visited_models:
                continue
            namespace = model.split(":", 1)[0]
            if namespace == "minecraft":
                external_models.add(model)
                continue
            if namespace not in MODEL_NAMESPACES:
                raise ValueError(f"model closure leaves exact namespaces: {model}")
            visited_models.add(model)
            path = _path_for_model(model)
            if path not in available[namespace]:
                raise ValueError(f"missing exact model {path}")
            model_paths.add(path)
            value = json.loads(archives[namespace].read(path))
            parent = value.get("parent")
            if isinstance(parent, str):
                pending.append(_model_key(parent, namespace))
            for texture in value.get("textures", {}).values():
                if isinstance(texture, str) and not texture.startswith("#"):
                    texture_keys.add(_model_key(texture, namespace))
            if model in connected_direct_models:
                if value.get("type") != "fusion:connecting" or value.get(
                    "loader"
                ) != "fusion:model":
                    raise ValueError(f"{path} Fusion model schema changed")
                if not isinstance(value.get("connections"), dict):
                    raise ValueError(f"{path} Fusion connections changed")
                _walk_predicate(value["connections"], predicate_types)
            elif model in disconnected_direct_models and (
                "type" in value or "loader" in value or "connections" in value
            ):
                raise ValueError(f"{path} forced-disconnected model schema changed")
            elif model in disconnected_direct_models:
                disconnected_model_textures[model] = {
                    _model_key(texture, namespace)
                    for texture in value.get("textures", {}).values()
                    if isinstance(texture, str) and not texture.startswith("#")
                }

        if len(model_paths) != MODEL_COUNT or roster_digest(model_paths) != MODEL_DIGEST:
            raise ValueError("exact model closure changed")
        if len(external_models) != 6 or roster_digest(
            external_models
        ) != EXTERNAL_MODELS_DIGEST:
            raise ValueError("Minecraft model parent roster changed")
        if predicate_types != PREDICATE_TYPES:
            raise ValueError("Fusion predicate type roster changed")

        for texture in sorted(texture_keys):
            namespace = texture.split(":", 1)[0]
            if namespace != "rechiseledcreate":
                raise ValueError(f"texture closure leaves bridge namespace: {texture}")
            png = _path_for_texture(texture)
            if png not in available[namespace]:
                raise ValueError(f"missing exact texture {png}")
            png_paths.add(png)
            raw = archives[namespace].read(png)
            if raw[:8] != b"\x89PNG\r\n\x1a\n" or len(raw) < 24:
                raise ValueError(f"invalid PNG {png}")
            width, height = struct.unpack(">II", raw[16:24])
            metadata = f"{png}.mcmeta"
            if metadata in available[namespace]:
                metadata_paths.add(metadata)
                meta_raw = archives[namespace].read(metadata)
                meta = json.loads(meta_raw)
                fusion_meta = meta.get("fusion") if isinstance(meta, dict) else None
                if not isinstance(fusion_meta, dict) or set(fusion_meta) not in (
                    {"connections", "type"},
                    {"connections", "layout", "type"},
                ):
                    raise ValueError(f"{metadata} Fusion metadata schema changed")
                layout = fusion_meta.get("layout", "full")
                if fusion_meta.get("type") != "connecting" or fusion_meta.get(
                    "connections"
                ) != {"type": "false"}:
                    raise ValueError(f"{metadata} Fusion metadata value changed")
                layouts[texture] = (layout, width, height, digest_bytes(meta_raw))
            else:
                layouts[texture] = ("plain", width, height, "-")

        if len(png_paths) != PNG_COUNT or roster_digest(png_paths) != PNG_DIGEST:
            raise ValueError("Rechiseled: Create PNG roster changed")
        if len(metadata_paths) != MCMETA_COUNT or roster_digest(
            metadata_paths
        ) != MCMETA_DIGEST:
            raise ValueError("Rechiseled: Create Fusion metadata roster changed")
        observed_layouts = {
            layout: sum(1 for row in layouts.values() if row[0] == layout)
            for layout in EXPECTED_LAYOUTS
        }
        if observed_layouts != {
            key: value[0] for key, value in EXPECTED_LAYOUTS.items()
        }:
            raise ValueError("Rechiseled: Create Fusion layout counts changed")
        for texture, (layout, width, height, _digest) in layouts.items():
            expected = EXPECTED_LAYOUTS.get(layout)
            if expected is None or (width, height) != expected[1:]:
                raise ValueError(f"{texture} dimensions changed for {layout}")
        metadata_textures = {
            texture for texture, row in layouts.items() if row[0] != "plain"
        }
        disconnected_sheets: set[str] = set()
        for model in disconnected_direct_models:
            sheets = disconnected_model_textures.get(model, set()) & metadata_textures
            if len(sheets) != 1:
                raise ValueError(
                    f"{model} forced-disconnected Fusion sheet contract changed"
                )
            disconnected_sheets.update(sheets)
        if len(disconnected_sheets) != DISCONNECTED_ROUTED_COUNT or roster_digest(
            disconnected_sheets
        ) != DISCONNECTED_SHEET_DIGEST:
            raise ValueError("forced-disconnected Fusion sheet roster changed")

        closure = sorted(
            routed_paths + disconnected_paths + list(model_paths)
            + list(png_paths) + list(metadata_paths)
        )
        if len(closure) != RESOURCE_COUNT or roster_digest(
            closure
        ) != PATH_CLOSURE_DIGEST:
            raise ValueError("exact resource closure changed")
        bridge_closure = [
            path for path in closure if path.startswith("assets/rechiseledcreate/")
        ]
        if len(bridge_closure) != BRIDGE_RESOURCE_COUNT:
            raise ValueError("bridge-owned resource closure changed")
        resource_rows: list[str] = []
        for path in closure:
            namespace = path.split("/", 2)[1]
            archive = archives[namespace]
            if "/blockstates/" in path:
                kind = "blockstate"
            elif "/models/" in path:
                kind = "model"
            elif path.endswith(".png.mcmeta"):
                kind = "metadata"
            else:
                kind = "texture"
            raw = archive.read(path)
            resource_rows.append(f"{kind}\t{path}\t{len(raw)}\t{digest_bytes(raw)}")

    if len(legal_states) != FUSION_ROUTED_STATE_COUNT or roster_digest(
        legal_states
    ) != CONNECTED_LEGAL_STATE_DIGEST:
        raise ValueError("Rechiseled: Create routed legal-state roster changed")
    if len(disconnected_legal_states) != DISCONNECTED_ROUTED_STATE_COUNT or roster_digest(
        disconnected_legal_states
    ) != DISCONNECTED_LEGAL_STATE_DIGEST:
        raise ValueError("forced-disconnected legal-state roster changed")
    if roster_digest(legal_states + disconnected_legal_states) != LEGAL_STATE_DIGEST:
        raise ValueError("full non-chisel legal-state roster changed")
    if shape_counts != {"full": 34, "axis": 73, "slab": 28, "stairs": 28}:
        raise ValueError("Rechiseled: Create shape roster changed")
    if disconnected_shape_counts != {
        "full": 5, "axis": 73, "slab": 0, "stairs": 0
    }:
        raise ValueError("forced-disconnected shape roster changed")

    definitions_raw = ("\n".join(sorted(definitions)) + "\n").encode("ascii")
    disconnected_definitions_raw = (
        "\n".join(sorted(disconnected_definitions)) + "\n"
    ).encode("ascii")
    resources_raw = ("\n".join(resource_rows) + "\n").encode("ascii")
    textures_raw = (
        "\n".join(
            "\t".join((texture, layout, str(width), str(height), meta_digest))
            for texture, (layout, width, height, meta_digest) in sorted(layouts.items())
        )
        + "\n"
    ).encode("ascii")
    host_models_raw = (
        "\n".join(
            f"model\t{path}\t{size}\t{sha256}"
            for path, size, sha256 in HOST_MODELS
        )
        + "\n"
    ).encode("ascii")
    if len(HOST_MODELS) != HOST_MODEL_COUNT:
        raise ValueError("host geometry ABI roster changed")

    chisel_paths = [
        "assets/rechiseledcreate/blockstates/mechanical_chisel.json",
        *CHISEL_MODELS,
        *CHISEL_TEXTURES,
    ]
    if len(chisel_paths) != CHISEL_RESOURCE_COUNT:
        raise ValueError("mechanical chisel closure count changed")
    chisel_resource_rows: list[str] = []
    chisel_texture_rows: list[str] = []
    with zipfile.ZipFile(bridge) as bridge_archive, zipfile.ZipFile(
        rechiseled
    ) as rechiseled_archive, zipfile.ZipFile(create) as create_archive:
        chisel_archives = {
            "rechiseledcreate": bridge_archive,
            "rechiseled": rechiseled_archive,
            "create": create_archive,
        }
        for path in sorted(chisel_paths):
            namespace = path.split("/", 2)[1]
            archive = chisel_archives[namespace]
            try:
                raw = archive.read(path)
            except KeyError as error:
                raise ValueError(f"missing mechanical chisel resource {path}") from error
            kind = (
                "blockstate" if "/blockstates/" in path
                else "model" if "/models/" in path
                else "texture"
            )
            if path == "assets/rechiseled/models/item/chisel.json":
                model = json.loads(raw)
                if model != {
                    "parent": "minecraft:item/handheld",
                    "textures": {"layer0": "rechiseled:item/chisel"},
                }:
                    raise ValueError("mechanical chisel item-model chain changed")
            chisel_resource_rows.append(
                f"{kind}\t{path}\t{len(raw)}\t{digest_bytes(raw)}"
            )
            if kind == "texture":
                if raw[:8] != b"\x89PNG\r\n\x1a\n" or len(raw) < 24:
                    raise ValueError(f"invalid mechanical chisel texture {path}")
                width, height = struct.unpack(">II", raw[16:24])
                if (width, height) != (16, 16):
                    raise ValueError(f"mechanical chisel texture dimensions changed: {path}")
                key = path.removeprefix("assets/").removesuffix(".png")
                namespace, value = key.split("/textures/", 1)
                chisel_texture_rows.append(
                    f"{namespace}:{value}\tplain\t{width}\t{height}\t-"
                )
    if len(chisel_texture_rows) != CHISEL_TEXTURE_COUNT:
        raise ValueError("mechanical chisel texture count changed")
    chisel_resources_raw = (
        "\n".join(chisel_resource_rows) + "\n"
    ).encode("ascii")
    chisel_textures_raw = (
        "\n".join(sorted(chisel_texture_rows)) + "\n"
    ).encode("ascii")

    catalog = {
        "schema": 1,
        "artifacts": [
            _artifact_record("rechiseledcreate", "1.1.1", BRIDGE_FILENAME,
                             BRIDGE_SIZE, BRIDGE_SHA1, BRIDGE_SHA256, BRIDGE_SHA512),
            _artifact_record("rechiseled", "1.2.5", RECHISELED_FILENAME,
                             RECHISELED_SIZE, RECHISELED_SHA1, RECHISELED_SHA256,
                             RECHISELED_SHA512),
            _artifact_record("fusion", "1.3.12", FUSION_FILENAME, FUSION_SIZE,
                             FUSION_SHA1, FUSION_SHA256, FUSION_SHA512),
            _artifact_record("create", "6.0.10", CREATE_FILENAME, CREATE_SIZE,
                             CREATE_SHA1, CREATE_SHA256, CREATE_SHA512),
        ],
        "requiredForStaticRendering": [
            "rechiseledcreate", "rechiseled", "fusion", "create"
        ],
    }
    profile = {
        "schema": 1,
        "profileId": "rechiseledcreate-1.1.1-atm-1.2.0",
        "namespaceOwner": "rechiseledcreate",
        "formatOwner": "fusion",
        "geometryDependencies": ["create", "rechiseled", "minecraft"],
        "counts": {
            "allBlockstates": ALL_BLOCKSTATES_COUNT,
            "routedBlocks": ROUTED_COUNT,
            "stockBlocks": 0,
            "routedLegalStates": ROUTED_STATE_COUNT,
            "fusionRoutedBlocks": FUSION_ROUTED_COUNT,
            "fusionRoutedLegalStates": FUSION_ROUTED_STATE_COUNT,
            "forcedDisconnectedBlocks": DISCONNECTED_ROUTED_COUNT,
            "forcedDisconnectedLegalStates": DISCONNECTED_ROUTED_STATE_COUNT,
            "mechanicalChiselStates": MECHANICAL_CHISEL_STATE_COUNT,
            "customFusionModels": CUSTOM_MODEL_COUNT,
            "directModels": DIRECT_MODEL_COUNT,
            "modelClosure": MODEL_COUNT,
            "textures": PNG_COUNT,
            "fusionMetadata": MCMETA_COUNT,
            "resourceClosure": RESOURCE_COUNT,
            "bridgeResourceClosure": BRIDGE_RESOURCE_COUNT,
            "hostGeometryModels": HOST_MODEL_COUNT,
            "mechanicalChiselResources": CHISEL_RESOURCE_COUNT,
            "mechanicalChiselTextures": CHISEL_TEXTURE_COUNT,
        },
        "shapes": {
            "predicateConnected": shape_counts,
            "forcedDisconnected": disconnected_shape_counts,
        },
        "layouts": {key: value[0] for key, value in EXPECTED_LAYOUTS.items()},
        "predicateTypes": sorted(PREDICATE_TYPES),
        "digests": {
            "routedRoster": ROUTED_DIGEST,
            "predicateConnectedRoster": CONNECTED_ROSTER_DIGEST,
            "forcedDisconnectedRoster": DISCONNECTED_ROSTER_DIGEST,
            "forcedDisconnectedSheetRoster": DISCONNECTED_SHEET_DIGEST,
            "legalStateRoster": LEGAL_STATE_DIGEST,
            "predicateConnectedLegalStates": CONNECTED_LEGAL_STATE_DIGEST,
            "forcedDisconnectedLegalStates": DISCONNECTED_LEGAL_STATE_DIGEST,
            "directModelRoster": DIRECT_MODEL_DIGEST,
            "modelRoster": MODEL_DIGEST,
            "pngRoster": PNG_DIGEST,
            "metadataRoster": MCMETA_DIGEST,
            "resourcePathClosure": PATH_CLOSURE_DIGEST,
            "definitions": digest_bytes(definitions_raw),
            "disconnectedDefinitions": digest_bytes(disconnected_definitions_raw),
            "requiredResources": digest_bytes(resources_raw),
            "textures": digest_bytes(textures_raw),
            "hostGeometryModels": digest_bytes(host_models_raw),
            "mechanicalChiselLegalStates": MECHANICAL_CHISEL_STATE_DIGEST,
            "mechanicalChiselResources": digest_bytes(chisel_resources_raw),
            "mechanicalChiselTextures": digest_bytes(chisel_textures_raw),
        },
        "hostGeometryAbi": {
            "owner": "minecraft",
            "clientJarSha256": MINECRAFT_CLIENT_SHA256,
            "pathsAreOutsideBridgeClosure": True,
        },
        "failurePolicy": "route-wide-inactive-or-atomic-stock-fallback",
        "assetPolicy": "operator-installed-only",
    }
    return {
        CATALOG_PATH: canonical_json(catalog),
        PROFILE_PATH: canonical_json(profile),
        DEFINITIONS_PATH: definitions_raw,
        DISCONNECTED_DEFINITIONS_PATH: disconnected_definitions_raw,
        RESOURCES_PATH: resources_raw,
        TEXTURES_PATH: textures_raw,
        HOST_MODELS_PATH: host_models_raw,
        CHISEL_RESOURCES_PATH: chisel_resources_raw,
        CHISEL_TEXTURES_PATH: chisel_textures_raw,
    }


def write_or_check(outputs: dict[Path, bytes], check: bool) -> str:
    changed: list[str] = []
    for path, raw in outputs.items():
        if not path.is_file() or path.read_bytes() != raw:
            changed.append(str(path))
            if not check:
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_bytes(raw)
    if check and changed:
        raise ValueError("generated profile is stale: " + ", ".join(changed))
    return "verified" if check else "generated"


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--bridge", required=True, type=Path)
    parser.add_argument("--rechiseled", required=True, type=Path)
    parser.add_argument("--fusion", required=True, type=Path)
    parser.add_argument("--create", required=True, type=Path)
    parser.add_argument("--check", action="store_true")
    arguments = parser.parse_args()
    outputs = build_outputs(
        arguments.bridge, arguments.rechiseled, arguments.fusion, arguments.create
    )
    action = write_or_check(outputs, arguments.check)
    print(
        f"{action} exact Rechiseled: Create profile ({ROUTED_COUNT} routed blocks)"
    )


if __name__ == "__main__":
    main()
