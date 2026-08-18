#!/usr/bin/env python3
# SPDX-License-Identifier: MIT
"""Generate the bounded Rechiseled: Create 1.1.1 staging gallery."""

from __future__ import annotations

import argparse
from collections import Counter
from dataclasses import dataclass, field
import hashlib
import io
import json
from pathlib import Path
import sys
from typing import Any, Iterable
from zipfile import ZipFile


ROOT = Path(__file__).resolve().parent
NAMESPACE = "rechiseledcreate_gallery"
OBJECTIVE = "rc_gallery"
ENVELOPE = {
    "min_x": 160,
    "max_x": 255,
    "min_y": 99,
    "max_y": 132,
    "min_z": 160,
    "max_z": 255,
}
FROZEN_ARCHIVE = {
    "filename": "bluemap-rechiseledcreate-gallery-scope-audit-frozen.zip",
    "size_bytes": 13_516,
    "sha256": "f5084c9b24d9645565b9e6708ee3ac10ea004ecacf7e2a45d6e85b3e5490ac4c",
}

ARTIFACTS = {
    "rechiseledcreate": {
        "filename": "rechiseledcreate-1.1.1-neoforge-mc1.21.jar",
        "size_bytes": 983_177,
        "sha256": "ba89cd5d1221621ed226cc7f1c26dc84a660cc4f6d122753052429f96d71248d",
    },
    "rechiseled": {
        "filename": "rechiseled-1.2.5-neoforge-mc1.21.jar",
        "size_bytes": 11_498_611,
        "sha256": "7bf14cf8a4bfdc4b6c990126a75da29fd2bb7559d1c05b71e29c8fd5ae044435",
    },
    "fusion": {
        "filename": "fusion-1.3.12-neoforge-mc1.21.1.jar",
        "size_bytes": 923_270,
        "sha256": "17f5215648a98bcde4134577b013200dbf363273ae282449c51408ae8346f2fa",
    },
    "create": {
        "filename": "create-1.21.1-6.0.10.jar",
        "size_bytes": 19_123_767,
        "sha256": "ef87fe5709f1ba1f5b8bb20a2925b5afb4669e178fd6d8bf10c167759eefe37a",
    },
}

# Read-only pins observed in the reusable same-pod lab. The generator can
# independently validate the four mod artifacts above from --artifact-dir.
LAB_ARTIFACT_PINS = {
    "all_the_mons_server_files_1.2.0": {
        "size_bytes": 1_055_896_389,
        "sha256": "de112ed8d79b3ff027e399a5108b706f6a2db3be74b15d0db6f6b9d6ac268e6c",
    },
    "neoforge_21.1.248_installer": {
        "size_bytes": 6_972_104,
        "sha256": "68eeab77059ba53df1812f1afa5bf530ab2566a3cdcd5f924aa6e71be42e410c",
    },
    "bluemap_5.22_java21_backport": {
        "size_bytes": 6_467_235,
        "sha256": "749f7647fa29764cea113114a7ab3259271bab3da22720989f2bd9fd1f3ba150",
    },
    "bluemap_create_addon_0.1.0-alpha.1": {
        "size_bytes": 312_744,
        "sha256": "e9e860ff0a3cc3398090d03f36441a9df863ec96c0c5e6da408815a1f9c1cd05",
    },
    **{
        key: {
            "size_bytes": value["size_bytes"],
            "sha256": value["sha256"],
        }
        for key, value in ARTIFACTS.items()
    },
}

STONE_MATERIALS = (
    "andesite",
    "asurine",
    "calcite",
    "crimsite",
    "deepslate",
    "diorite",
    "dripstone",
    "granite",
    "limestone",
    "ochrum",
    "scorchia",
    "scoria",
    "tuff",
    "veridium",
)
STONE_LAYOUTS = ("polished", "small_brick")
WINDOW_LAYOUTS = {
    "acacia": (
        "covered", "diagonal", "large", "panes", "rounded", "slim",
        "swirling", "tiles",
    ),
    "birch": (
        "bars", "diagonal", "large", "panes", "rounded", "slim",
        "swirling", "tiles",
    ),
    "crimson": (
        "bars", "covered", "large", "panes", "rounded", "slim",
        "swirling", "tiles",
    ),
    "dark_oak": (
        "bars", "covered", "diagonal", "panes", "rounded", "slim",
        "swirling", "tiles",
    ),
    "jungle": (
        "bars", "covered", "diagonal", "large", "panes", "rounded",
        "swirling", "tiles",
    ),
    "mangrove": (
        "bars", "covered", "diagonal", "large", "panes", "slim",
        "swirling", "tiles",
    ),
    "oak": (
        "bars", "covered", "diagonal", "large", "rounded", "slim",
        "swirling", "tiles",
    ),
    "spruce": (
        "bars", "covered", "diagonal", "large", "panes", "rounded",
        "slim", "swirling", "tiles",
    ),
    "warped": (
        "bars", "covered", "diagonal", "large", "panes", "rounded",
        "slim", "tiles",
    ),
}
ROSE_ORDINARY = (
    "rose_quartz_bricks",
    "rose_quartz_chiseled",
    "rose_quartz_crushed",
    "rose_quartz_polished_block",
    "rose_quartz_squares",
)
ROSE_CONNECTED = (*ROSE_ORDINARY, "rose_quartz_tiles")
MECHANICAL_CHISEL = "rechiseledcreate:mechanical_chisel"

MASKS = (
    ("none", 0x00),
    ("top", 0x01),
    ("right", 0x04),
    ("bottom", 0x10),
    ("left", 0x40),
    ("top-right", 0x05),
    ("top-bottom", 0x11),
    ("left-right", 0x44),
    ("cardinals", 0x55),
    ("one-diagonal", 0x57),
    ("mask-dd", 0xDD),
    ("full-edge47", 0x7F),
    ("full-edge46", 0xFD),
    ("all", 0xFF),
)
MASK_OFFSETS = (
    (0, 0, -1),
    (1, 0, -1),
    (1, 0, 0),
    (1, 0, 1),
    (0, 0, 1),
    (-1, 0, 1),
    (-1, 0, 0),
    (-1, 0, -1),
)
HORIZONTAL_OFFSETS = {
    "north": (0, 0, -1),
    "east": (1, 0, 0),
    "south": (0, 0, 1),
    "west": (-1, 0, 0),
}
COUNTER_CLOCKWISE = {
    "north": "west",
    "east": "north",
    "south": "east",
    "west": "south",
}
CLOCKWISE = {value: key for key, value in COUNTER_CLOCKWISE.items()}

SECTION_SCORE_NAMES = {
    "connected-census": "#connected_census",
    "forced-disconnected": "#forced_routes",
    "forced-disconnected-adjacency": "#forced_pairs",
    "layout-mask": "#mask_cases",
    "stairs-state": "#stair_cases",
    "slab-state": "#slab_cases",
    "axis-pair": "#axis_pairs",
    "axis-topology": "#axis_topology",
    "shape-topology": "#shape_topology",
    "mechanical-chisel": "#chisel_cases",
    "ownership-control": "#ownership_controls",
}


@dataclass(frozen=True, order=True)
class Position:
    x: int
    y: int
    z: int

    def offset(self, dx: int = 0, dy: int = 0, dz: int = 0) -> "Position":
        return Position(self.x + dx, self.y + dy, self.z + dz)

    def command(self) -> str:
        return f"{self.x} {self.y} {self.z}"

    def as_dict(self) -> dict[str, int]:
        return {"x": self.x, "y": self.y, "z": self.z}


@dataclass(frozen=True)
class Placement:
    position: Position
    block: str


@dataclass(frozen=True)
class Case:
    case_id: str
    section: str
    anchor: Position
    placements: tuple[Placement, ...]
    notes: str
    metadata: dict[str, Any] = field(default_factory=dict)


@dataclass(frozen=True)
class RosterEntry:
    block_id: str
    route_kind: str
    shape_family: str
    legal_state_count: int
    texture_layout: str
    texture_sheet: str
    counterpart: str


@dataclass(frozen=True)
class ArtifactEvidence:
    paths: dict[str, Path]
    bytes_by_key: dict[str, bytes]

    def zip(self, key: str) -> ZipFile:
        return ZipFile(io.BytesIO(self.bytes_by_key[key]))


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def load_artifacts(artifact_dir: Path) -> ArtifactEvidence:
    paths: dict[str, Path] = {}
    payloads: dict[str, bytes] = {}
    for key, expected in ARTIFACTS.items():
        path = artifact_dir / str(expected["filename"])
        if not path.is_file():
            raise ValueError(f"missing exact {key} artifact: {path}")
        payload = path.read_bytes()
        if len(payload) != expected["size_bytes"]:
            raise ValueError(
                f"{key} artifact size changed: {len(payload)} != "
                f"{expected['size_bytes']}"
            )
        actual_sha = sha256(payload)
        if actual_sha != expected["sha256"]:
            raise ValueError(
                f"{key} artifact SHA-256 changed: {actual_sha} != "
                f"{expected['sha256']}"
            )
        paths[key] = path
        payloads[key] = payload
    return ArtifactEvidence(paths, payloads)


def stone_connected_names() -> set[str]:
    return {
        f"{material}_cut_{layout}{suffix}_connecting"
        for material in STONE_MATERIALS
        for layout in STONE_LAYOUTS
        for suffix in ("", "_slab", "_stairs")
    }


def window_ordinary_names() -> set[str]:
    return {
        f"{material}_window_{layout}"
        for material, layouts in WINDOW_LAYOUTS.items()
        for layout in layouts
    }


def window_connected_names() -> set[str]:
    return {f"{name}_connecting" for name in window_ordinary_names()}


def rose_ordinary_names() -> set[str]:
    return set(ROSE_ORDINARY)


def rose_connected_names() -> set[str]:
    return {f"{name}_connecting" for name in ROSE_CONNECTED}


def connected_names() -> set[str]:
    return stone_connected_names() | window_connected_names() | rose_connected_names()


def ordinary_names() -> set[str]:
    return window_ordinary_names() | rose_ordinary_names()


def expected_blockstate_names() -> set[str]:
    return connected_names() | ordinary_names() | {"mechanical_chisel"}


def namespace(name: str) -> str:
    return f"rechiseledcreate:{name}"


def shape_family(name: str) -> str:
    if name == "mechanical_chisel":
        return "mechanical"
    if "_window_" in name:
        return "axis"
    if name.endswith("_slab_connecting"):
        return "slab"
    if name.endswith("_stairs_connecting"):
        return "stairs"
    return "full"


def legal_state_count(name: str) -> int:
    family = shape_family(name)
    return {
        "axis": 3,
        "slab": 6,
        "stairs": 80,
        "full": 1,
        "mechanical": 24,
    }[family]


def texture_sheet(name: str) -> str:
    base = name.removesuffix("_connecting")
    if base.endswith("_slab"):
        base = base.removesuffix("_slab")
    elif base.endswith("_stairs"):
        base = base.removesuffix("_stairs")
    if "_window_" in base:
        base = f"{base}_side"
    return f"rechiseledcreate:block/{base}"


def sheet_layout(name: str) -> str:
    sheet = texture_sheet(name)
    if "_window_tiles_side" in sheet:
        return "full"
    if "_window_" in sheet:
        return "simple"
    if sheet.endswith("/rose_quartz_bricks"):
        return "simple"
    return "full"


def counterpart(name: str) -> str:
    if name in ordinary_names():
        return namespace(f"{name}_connecting")
    if name in connected_names():
        ordinary = name.removesuffix("_connecting")
        if ordinary in ordinary_names():
            return namespace(ordinary)
    return "-"


def roster() -> list[RosterEntry]:
    rows: list[RosterEntry] = []
    for name in sorted(expected_blockstate_names()):
        if name == "mechanical_chisel":
            route_kind = "mechanical-chisel"
            layout = "create-static-plus-dynamic"
            sheet = "create:block/mechanical_saw"
        elif name in ordinary_names():
            route_kind = "forced-disconnected-tile0"
            layout = sheet_layout(name)
            sheet = texture_sheet(name)
        else:
            route_kind = "predicate-connected"
            layout = sheet_layout(name)
            sheet = texture_sheet(name)
        rows.append(RosterEntry(
            namespace(name),
            route_kind,
            shape_family(name),
            legal_state_count(name),
            layout,
            sheet,
            counterpart(name),
        ))
    if len(rows) != 242:
        raise AssertionError(f"route roster changed: {len(rows)} != 242")
    if sum(row.legal_state_count for row in rows) != 2_909:
        raise AssertionError("legal state census changed")
    kinds = Counter(row.route_kind for row in rows)
    if kinds != Counter({
        "predicate-connected": 163,
        "forced-disconnected-tile0": 78,
        "mechanical-chisel": 1,
    }):
        raise AssertionError(f"route-kind census changed: {kinds}")
    return rows


def model_entries(value: Any) -> Iterable[dict[str, Any]]:
    if isinstance(value, list):
        yield from value
    elif isinstance(value, dict):
        yield value
    else:
        raise ValueError(f"invalid blockstate model entry: {value!r}")


def expected_variant_keys(name: str) -> set[str]:
    family = shape_family(name)
    if family == "axis":
        return {f"axis={axis}" for axis in ("x", "y", "z")}
    if family == "slab":
        return {f"type={kind}" for kind in ("bottom", "double", "top")}
    if family == "stairs":
        return {
            f"facing={facing},half={half},shape={shape}"
            for facing in ("east", "north", "south", "west")
            for half in ("bottom", "top")
            for shape in (
                "inner_left", "inner_right", "outer_left", "outer_right", "straight"
            )
        }
    if family == "mechanical":
        return {
            f"axis_along_first={axis},facing={facing},flipped={flipped}"
            for axis in ("false", "true")
            for facing in ("down", "east", "north", "south", "up", "west")
            for flipped in ("false", "true")
        }
    return {""}


def parse_json(data: bytes, label: str) -> dict[str, Any]:
    try:
        value = json.loads(data)
    except json.JSONDecodeError as error:
        raise ValueError(f"malformed JSON in {label}: {error}") from error
    if not isinstance(value, dict):
        raise ValueError(f"expected object in {label}")
    return value


def validate_artifact_roster(evidence: ArtifactEvidence) -> None:
    with evidence.zip("rechiseledcreate") as archive:
        names = set(archive.namelist())
        prefix = "assets/rechiseledcreate/blockstates/"
        actual = {
            Path(path).stem
            for path in names
            if path.startswith(prefix) and path.endswith(".json")
        }
        expected = expected_blockstate_names()
        if actual != expected:
            missing = sorted(expected - actual)
            extra = sorted(actual - expected)
            raise ValueError(f"exact blockstate roster changed; missing={missing}, extra={extra}")

        for name in sorted(expected):
            resource = f"{prefix}{name}.json"
            blockstate = parse_json(archive.read(resource), resource)
            variants = blockstate.get("variants")
            if not isinstance(variants, dict):
                raise ValueError(f"{resource} does not contain variants")
            actual_keys = set(variants)
            wanted_keys = expected_variant_keys(name)
            if actual_keys != wanted_keys:
                raise ValueError(
                    f"{resource} variants changed; "
                    f"missing={sorted(wanted_keys - actual_keys)}, "
                    f"extra={sorted(actual_keys - wanted_keys)}"
                )
            for variant in variants.values():
                for entry in model_entries(variant):
                    reference = entry.get("model")
                    if not isinstance(reference, str) or ":" not in reference:
                        raise ValueError(f"invalid model reference in {resource}: {reference!r}")
                    model_namespace, model_path = reference.split(":", 1)
                    if model_namespace == "rechiseledcreate":
                        target = f"assets/rechiseledcreate/models/{model_path}.json"
                        if target not in names:
                            raise ValueError(f"missing direct model reference: {target}")
                    elif name == "mechanical_chisel" and model_namespace == "create":
                        continue
                    else:
                        raise ValueError(
                            f"unexpected external direct model reference in {resource}: {reference}"
                        )

        metadata_paths = {
            path for path in names
            if path.startswith("assets/rechiseledcreate/textures/block/")
            and path.endswith(".png.mcmeta")
        }
        layout_by_path: dict[str, str] = {}
        for path in metadata_paths:
            metadata = parse_json(archive.read(path), path)
            fusion = metadata.get("fusion")
            if not isinstance(fusion, dict) or fusion.get("type") != "connecting":
                raise ValueError(f"unexpected non-connecting metadata: {path}")
            connections = fusion.get("connections")
            if connections != {"type": "false"}:
                raise ValueError(f"unexpected connection predicate in {path}: {connections}")
            layout = fusion.get("layout", "full")
            if layout not in {"full", "simple"}:
                raise ValueError(f"unexpected texture layout in {path}: {layout}")
            layout_by_path[path] = layout

        if Counter(layout_by_path.values()) != Counter({"full": 42, "simple": 73}):
            raise ValueError(
                "source Fusion sheet census changed: "
                f"{Counter(layout_by_path.values())}"
            )
        referenced = {
            f"assets/rechiseledcreate/textures/{row.texture_sheet.split(':', 1)[1]}.png.mcmeta"
            for row in roster()
            if row.route_kind != "mechanical-chisel"
        }
        if len(referenced) != 107 or not referenced <= metadata_paths:
            raise ValueError(
                f"referenced sheet census changed: count={len(referenced)}, "
                f"missing={sorted(referenced - metadata_paths)}"
            )
        if Counter(layout_by_path[path] for path in referenced) != Counter({
            "full": 42,
            "simple": 65,
        }):
            raise ValueError("referenced Fusion sheet layout census changed")
        unused = metadata_paths - referenced
        if len(unused) != 8 or {layout_by_path[path] for path in unused} != {"simple"}:
            raise ValueError("unused Fusion sheet census changed")

    with evidence.zip("rechiseled") as archive:
        control = "assets/rechiseled/blockstates/andesite_brick_pattern_connecting.json"
        if control not in archive.namelist():
            raise ValueError(f"missing parent ownership control: {control}")
    with evidence.zip("create") as archive:
        control = "assets/create/blockstates/mechanical_saw.json"
        if control not in archive.namelist():
            raise ValueError(f"missing Create ownership control: {control}")
        blockstate = parse_json(archive.read(control), control)
        variant = "axis_along_first=false,facing=up,flipped=false"
        if variant not in blockstate.get("variants", {}):
            raise ValueError(f"Create ownership-control state changed: {variant}")


def default_state(name: str) -> str:
    block = namespace(name)
    family = shape_family(name)
    if family == "axis":
        return f"{block}[axis=y]"
    if family == "slab":
        return f"{block}[type=bottom,waterlogged=false]"
    if family == "stairs":
        return (
            f"{block}[facing=north,half=bottom,shape=straight,waterlogged=false]"
        )
    return block


def stair_topology(
    block: str,
    facing: str,
    half: str,
    shape: str,
    waterlogged: str,
    target: tuple[int, int, int] = (0, 0, 0),
) -> tuple[tuple[int, int, int, str] | None, tuple[int, int, int, str]]:
    """Return a support (when needed) and target for a stable stair state."""
    if facing not in HORIZONTAL_OFFSETS:
        raise ValueError(f"invalid stair facing: {facing}")
    if half not in {"bottom", "top"}:
        raise ValueError(f"invalid stair half: {half}")
    if shape not in {
        "inner_left", "inner_right", "outer_left", "outer_right", "straight"
    }:
        raise ValueError(f"invalid stair shape: {shape}")
    if waterlogged not in {"false", "true"}:
        raise ValueError(f"invalid waterlogged value: {waterlogged}")
    tx, ty, tz = target
    target_state = (
        f"{block}[facing={facing},half={half},shape={shape},"
        f"waterlogged={waterlogged}]"
    )
    target_placement = (tx, ty, tz, target_state)
    if shape == "straight":
        return None, target_placement
    fx, fy, fz = HORIZONTAL_OFFSETS[facing]
    if shape.startswith("inner_"):
        fx, fy, fz = -fx, -fy, -fz
    support_facing = (
        COUNTER_CLOCKWISE[facing]
        if shape.endswith("_left")
        else CLOCKWISE[facing]
    )
    support_state = (
        f"{block}[facing={support_facing},half={half},shape=straight,"
        f"waterlogged={waterlogged}]"
    )
    return (tx + fx, ty + fy, tz + fz, support_state), target_placement


def add_case(
    cases: list[Case],
    case_id: str,
    section: str,
    anchor: Position,
    relative: Iterable[tuple[int, int, int, str]],
    notes: str,
    metadata: dict[str, Any] | None = None,
) -> None:
    placements = tuple(
        Placement(anchor.offset(dx, dy, dz), block)
        for dx, dy, dz, block in relative
    )
    if not placements:
        raise AssertionError(f"case has no placements: {case_id}")
    cases.append(Case(
        case_id,
        section,
        anchor,
        placements,
        notes,
        metadata or {},
    ))


def gallery_cases() -> list[Case]:
    cases: list[Case] = []

    # Exact 163 predicate-connected IDs, with all 78 corresponding
    # forced-disconnected routes immediately alongside their matching route.
    ordinary_seen: set[str] = set()
    for index, name in enumerate(sorted(connected_names())):
        center = Position(162 + 3 * (index % 18), 100, 162 + 3 * (index // 18))
        add_case(
            cases,
            f"connected-{name}",
            "connected-census",
            center,
            [(0, 0, 0, default_state(name))],
            "isolated default-state census of an exact predicate-connected route",
            {"block_id": namespace(name), "route_kind": "predicate-connected"},
        )
        ordinary = name.removesuffix("_connecting")
        if ordinary in ordinary_names():
            ordinary_seen.add(ordinary)
            add_case(
                cases,
                f"forced-disconnected-{ordinary}",
                "forced-disconnected",
                center.offset(dx=1),
                [(0, 0, 0, default_state(ordinary))],
                "matching bridge route whose Fusion sheet must be forced to tile 0",
                {
                    "block_id": namespace(ordinary),
                    "route_kind": "forced-disconnected-tile0",
                    "paired_with": namespace(name),
                },
            )
    if ordinary_seen != ordinary_names():
        raise AssertionError(
            f"ordinary counterpart pairing changed: missing={sorted(ordinary_names()-ordinary_seen)}"
        )

    # Canonical UP-face mask witnesses for both connected-sheet layouts.
    for layout_index, (layout, block) in enumerate((
        ("full", "rechiseledcreate:andesite_cut_polished_connecting"),
        ("simple", "rechiseledcreate:rose_quartz_bricks_connecting"),
    )):
        for mask_index, (label, mask) in enumerate(MASKS):
            global_index = layout_index * len(MASKS) + mask_index
            center = Position(
                162 + 6 * (global_index % 7),
                100,
                198 + 6 * (global_index // 7),
            )
            relative = [(0, 0, 0, block)]
            relative.extend(
                (*MASK_OFFSETS[bit], block)
                for bit in range(8)
                if mask & (1 << bit)
            )
            add_case(
                cases,
                f"mask-{layout}-{label}",
                "layout-mask",
                center,
                relative,
                f"canonical {layout.upper()} UP-face mask 0x{mask:02x}",
                {"layout": layout, "face": "up", "mask": f"0x{mask:02x}"},
            )

    # Every legal state of one representative connecting slab family.
    slab = "rechiseledcreate:andesite_cut_polished_slab_connecting"
    for index, (slab_type, waterlogged) in enumerate(
        (kind, water)
        for kind in ("bottom", "double", "top")
        for water in ("false", "true")
    ):
        center = Position(238 + 4 * (index % 3), 100, 162 + 4 * (index // 3))
        state = f"{slab}[type={slab_type},waterlogged={waterlogged}]"
        add_case(
            cases,
            f"slab-{slab_type}-waterlogged-{waterlogged}",
            "slab-state",
            center,
            [(0, 0, 0, state)],
            "all six legal representative slab states",
            {"type": slab_type, "waterlogged": waterlogged},
        )

    # Every legal state of one representative connecting stair family. Stable
    # same-half perpendicular supports are placed before non-straight targets.
    stairs = "rechiseledcreate:andesite_cut_polished_stairs_connecting"
    stair_states = [
        (facing, half, shape, waterlogged)
        for facing in ("east", "north", "south", "west")
        for half in ("bottom", "top")
        for shape in (
            "inner_left", "inner_right", "outer_left", "outer_right", "straight"
        )
        for waterlogged in ("false", "true")
    ]
    for index, (facing, half, shape, waterlogged) in enumerate(stair_states):
        center = Position(162 + 4 * (index % 10), 100, 224 + 4 * (index // 10))
        support, target = stair_topology(stairs, facing, half, shape, waterlogged)
        relative = [item for item in (support, target) if item is not None]
        add_case(
            cases,
            f"stairs-{facing}-{half}-{shape}-waterlogged-{waterlogged}",
            "stairs-state",
            center,
            relative,
            "all 80 legal targets; non-straight topology has support first",
            {
                "facing": facing,
                "half": half,
                "shape": shape,
                "waterlogged": waterlogged,
                "support": support is not None,
            },
        )

    # Latin-square-like witness set: all nine wood materials and all nine
    # window layouts exactly once, each in x/y/z and paired with its ordinary
    # forced-disconnected counterpart.
    axis_witnesses = (
        ("acacia", "covered"),
        ("birch", "diagonal"),
        ("crimson", "large"),
        ("dark_oak", "panes"),
        ("jungle", "rounded"),
        ("mangrove", "slim"),
        ("oak", "swirling"),
        ("spruce", "tiles"),
        ("warped", "bars"),
    )
    for row, (material, layout) in enumerate(axis_witnesses):
        ordinary = f"rechiseledcreate:{material}_window_{layout}"
        connected = f"{ordinary}_connecting"
        for column, axis in enumerate(("x", "y", "z")):
            center = Position(220 + 4 * column, 100, 162 + 3 * row)
            add_case(
                cases,
                f"axis-pair-{material}-{layout}-{axis}",
                "axis-pair",
                center,
                [
                    (0, 0, 0, f"{connected}[axis={axis}]"),
                    (1, 0, 0, f"{ordinary}[axis={axis}]"),
                ],
                "connected route beside the same-sheet forced-tile0 route",
                {"material": material, "layout": layout, "axis": axis},
            )

    pillar = "rechiseledcreate:acacia_window_tiles_connecting"
    add_case(
        cases,
        "axis-x-same-state-continuity",
        "axis-topology",
        Position(238, 100, 174),
        [(0, 0, 0, f"{pillar}[axis=x]"), (1, 0, 0, f"{pillar}[axis=x]")],
        "same-state x-axis continuity",
    )
    add_case(
        cases,
        "axis-y-same-state-continuity",
        "axis-topology",
        Position(244, 100, 174),
        [(0, 0, 0, f"{pillar}[axis=y]"), (0, 1, 0, f"{pillar}[axis=y]")],
        "same-state y-axis continuity",
    )
    add_case(
        cases,
        "axis-z-same-state-continuity",
        "axis-topology",
        Position(250, 100, 174),
        [(0, 0, 0, f"{pillar}[axis=z]"), (0, 0, 1, f"{pillar}[axis=z]")],
        "same-state z-axis continuity",
    )
    add_case(
        cases,
        "axis-state-mismatch",
        "axis-topology",
        Position(238, 100, 180),
        [
            (0, 0, 0, f"{pillar}[axis=x]"),
            (1, 0, 0, f"{pillar}[axis=y]"),
            (2, 0, 0, f"{pillar}[axis=z]"),
        ],
        "adjacent x/y/z states must not satisfy is_same_state",
    )
    add_case(
        cases,
        "axis-cross-material-layout",
        "axis-topology",
        Position(244, 100, 180),
        [
            (0, 0, 0, "rechiseledcreate:acacia_window_covered_connecting[axis=y]"),
            (1, 0, 0, "rechiseledcreate:birch_window_diagonal_connecting[axis=y]"),
        ],
        "different material/layout IDs are deliberate non-connections",
    )
    add_case(
        cases,
        "axis-connected-versus-forced-tile0",
        "axis-topology",
        Position(250, 100, 180),
        [
            (0, 0, 0, f"{pillar}[axis=y]"),
            (1, 0, 0, "rechiseledcreate:acacia_window_tiles[axis=y]"),
        ],
        "same sheet but distinct route ownership; ordinary block stays tile 0",
    )

    cube = "rechiseledcreate:andesite_cut_polished_connecting"
    small = "rechiseledcreate:andesite_cut_small_brick_connecting"
    slab_bottom = f"{slab}[type=bottom,waterlogged=false]"
    slab_double = f"{slab}[type=double,waterlogged=false]"
    slab_top = f"{slab}[type=top,waterlogged=false]"
    stair_straight = (
        f"{stairs}[facing=north,half=bottom,shape=straight,waterlogged=false]"
    )
    shape_cases = (
        ("cube-top-slab", Position(238, 100, 190), cube, slab_top,
         "top slab participates in the cube top-face predicate"),
        ("cube-bottom-slab-control", Position(242, 100, 190), cube, slab_bottom,
         "bottom slab is the complementary top-face non-connection control"),
        ("cube-double-slab", Position(246, 100, 190), cube, slab_double,
         "double slab participates as a full block"),
        ("cube-stair", Position(250, 100, 190), cube, stair_straight,
         "same-family stair participates in cross-shape connectivity"),
        ("top-slab-pair", Position(238, 100, 194), slab_top, slab_top,
         "same-state top slab continuity"),
        ("bottom-slab-pair", Position(242, 100, 194), slab_bottom, slab_bottom,
         "same-state bottom slab continuity"),
        ("straight-stair-pair", Position(246, 100, 194), stair_straight, stair_straight,
         "same-family straight stair continuity"),
        ("cross-sheet-control", Position(250, 100, 194), cube, small,
         "different texture sheets are a deliberate non-connection"),
    )
    for case_id, center, left, right, notes in shape_cases:
        add_case(
            cases,
            case_id,
            "shape-topology",
            center,
            [(0, 0, 0, left), (1, 0, 0, right)],
            notes,
        )

    # Ordinary window/quartz routes are not stock controls: their Fusion
    # sheets explicitly force disconnected tile 0. Exercise both layouts with
    # adjacent blocks of the exact same ID so an incorrect same-ID predicate
    # cannot accidentally make either block select a connected tile.
    forced_adjacency = (
        (
            "simple",
            Position(238, 100, 200),
            "rechiseledcreate:acacia_window_covered[axis=y]",
        ),
        (
            "full",
            Position(244, 100, 200),
            "rechiseledcreate:acacia_window_tiles[axis=y]",
        ),
    )
    for layout, center, block in forced_adjacency:
        add_case(
            cases,
            f"forced-disconnected-adjacent-same-id-{layout}",
            "forced-disconnected-adjacency",
            center,
            [(0, 0, 0, block), (1, 0, 0, block)],
            f"adjacent same-ID ordinary {layout.upper()} routes both remain tile 0",
            {
                "layout": layout,
                "route_kind": "forced-disconnected-tile0",
                "same_id_adjacency": True,
                "expected_tile": 0,
            },
        )

    # All 24 blockstates. Empty setblock-created block entities and isolation
    # from a kinetic source keep the shaft stable at zero speed. UP/DOWN rows
    # exercise the visible chisel tool; no item/filter/progress NBT is written.
    chisel_states = [
        (facing, axis, flipped)
        for facing in ("down", "up", "north", "south", "west", "east")
        for axis in ("false", "true")
        for flipped in ("false", "true")
    ]
    for index, (facing, axis, flipped) in enumerate(chisel_states):
        center = Position(220 + 4 * (index % 4), 100, 198 + 4 * (index // 4))
        state = (
            f"{MECHANICAL_CHISEL}[axis_along_first={axis},facing={facing},"
            f"flipped={flipped}]"
        )
        add_case(
            cases,
            f"mechanical-chisel-{facing}-axis-{axis}-flipped-{flipped}",
            "mechanical-chisel",
            center,
            [(0, 0, 0, state)],
            "neutral empty shaft; vertical facings include the visible chisel tool",
            {
                "facing": facing,
                "axis_along_first": axis,
                "flipped": flipped,
                "expected_dynamic_tool": facing in {"up", "down"},
                "excluded": ["transported_item", "input_item", "filter_overlay"],
            },
        )

    ownership = (
        (
            "parent-rechiseled",
            Position(252, 100, 198),
            "rechiseled:andesite_brick_pattern_connecting",
            "parent Rechiseled add-on ownership/fallback control",
        ),
        (
            "parent-create",
            Position(252, 100, 202),
            "create:mechanical_saw[axis_along_first=false,facing=up,flipped=false]",
            "parent Create add-on ownership/fallback control",
        ),
        (
            "vanilla",
            Position(252, 100, 206),
            "minecraft:stone",
            "untouched vanilla stock renderer control",
        ),
    )
    for label, center, block, notes in ownership:
        add_case(
            cases,
            f"ownership-{label}",
            "ownership-control",
            center,
            [(0, 0, 0, block)],
            notes,
        )

    case_ids = [case.case_id for case in cases]
    if len(case_ids) != len(set(case_ids)):
        raise AssertionError("duplicate gallery case ID")
    positions = [placement.position for case in cases for placement in case.placements]
    if len(positions) != len(set(positions)):
        duplicates = sorted(position for position, count in Counter(positions).items() if count > 1)
        raise AssertionError(f"gallery placement overlap: {duplicates}")
    for position in positions:
        assert_in_envelope(position)

    section_counts = Counter(case.section for case in cases)
    expected_sections = Counter({
        "connected-census": 163,
        "forced-disconnected": 78,
        "forced-disconnected-adjacency": 2,
        "layout-mask": 28,
        "stairs-state": 80,
        "slab-state": 6,
        "axis-pair": 27,
        "axis-topology": 6,
        "shape-topology": 8,
        "mechanical-chisel": 24,
        "ownership-control": 3,
    })
    if section_counts != expected_sections:
        raise AssertionError(f"gallery case census changed: {section_counts}")
    if len(cases) != 425 or len(positions) != 627:
        raise AssertionError(
            f"frozen gallery census changed: {len(cases)} cases / "
            f"{len(positions)} placements"
        )
    return cases


def assert_in_envelope(position: Position) -> None:
    if not (
        ENVELOPE["min_x"] <= position.x <= ENVELOPE["max_x"]
        and ENVELOPE["min_y"] <= position.y <= ENVELOPE["max_y"]
        and ENVELOPE["min_z"] <= position.z <= ENVELOPE["max_z"]
    ):
        raise AssertionError(f"position outside safe envelope: {position}")


def fill_commands(y1: int, y2: int, block: str) -> list[str]:
    lines: list[str] = []
    for x in range(ENVELOPE["min_x"], ENVELOPE["max_x"] + 1, 32):
        for z in range(ENVELOPE["min_z"], ENVELOPE["max_z"] + 1, 32):
            x2 = min(x + 31, ENVELOPE["max_x"])
            z2 = min(z + 31, ENVELOPE["max_z"])
            volume = (x2 - x + 1) * (y2 - y1 + 1) * (z2 - z + 1)
            if volume > 32_768:
                raise AssertionError(f"fill exceeds command limit: {volume}")
            lines.append(f"fill {x} {y1} {z} {x2} {y2} {z2} {block}")
    return lines


def clear_commands() -> list[str]:
    lines: list[str] = []
    for y in range(ENVELOPE["min_y"], ENVELOPE["max_y"] + 1, 8):
        y2 = min(y + 7, ENVELOPE["max_y"])
        lines.extend(fill_commands(y, y2, "minecraft:air"))
    return lines


def section_counts(cases: list[Case]) -> Counter[str]:
    return Counter(case.section for case in cases)


def placements(cases: list[Case]) -> list[Placement]:
    return [placement for case in cases for placement in case.placements]


FLOOR_SENTINELS = (
    Position(160, 99, 160),
    Position(255, 99, 160),
    Position(160, 99, 255),
    Position(255, 99, 255),
)


def build_function(cases: list[Case]) -> str:
    lines = [
        "# Generated by gallery/generate.py; do not edit.",
        f"function {NAMESPACE}:clear",
        f"forceload add {ENVELOPE['min_x']} {ENVELOPE['min_z']} "
        f"{ENVELOPE['max_x']} {ENVELOPE['max_z']}",
        *fill_commands(99, 99, "minecraft:stone"),
        f"scoreboard players set #placements_built {OBJECTIVE} 0",
        f"scoreboard players set #logical_cases_built {OBJECTIVE} 0",
    ]
    for score in SECTION_SCORE_NAMES.values():
        lines.append(f"scoreboard players set {score} {OBJECTIVE} 0")
    for case in cases:
        for placement in case.placements:
            lines.append(f"setblock {placement.position.command()} {placement.block}")
            lines.append(f"scoreboard players add #placements_built {OBJECTIVE} 1")
        lines.append(
            f"scoreboard players add {SECTION_SCORE_NAMES[case.section]} {OBJECTIVE} 1"
        )
        lines.append(f"scoreboard players add #logical_cases_built {OBJECTIVE} 1")
    lines.extend((
        f"function {NAMESPACE}:verify_immediate",
        f"schedule function {NAMESPACE}:verify_20t 20t replace",
        f"schedule function {NAMESPACE}:verify_100t 100t replace",
    ))
    return "\n".join(lines) + "\n"


def expected_counter_values(cases: list[Case]) -> list[tuple[str, int]]:
    all_placements = placements(cases)
    checked = len(all_placements) + len(FLOOR_SENTINELS)
    values = [
        ("#checked", checked),
        ("#placements_built", len(all_placements)),
        ("#logical_cases_built", len(cases)),
    ]
    counts = section_counts(cases)
    values.extend(
        (score, counts[section])
        for section, score in SECTION_SCORE_NAMES.items()
    )
    return values


def verify_function(cases: list[Case]) -> str:
    lines = [
        "# Generated by gallery/generate.py; do not edit.",
        f"scoreboard players set #failures {OBJECTIVE} 0",
        f"scoreboard players set #checked {OBJECTIVE} 0",
    ]
    for placement in placements(cases):
        lines.append(
            f"execute unless block {placement.position.command()} {placement.block} run "
            f"scoreboard players add #failures {OBJECTIVE} 1"
        )
        lines.append(f"scoreboard players add #checked {OBJECTIVE} 1")
    for position in FLOOR_SENTINELS:
        lines.append(
            f"execute unless block {position.command()} minecraft:stone run "
            f"scoreboard players add #failures {OBJECTIVE} 1"
        )
        lines.append(f"scoreboard players add #checked {OBJECTIVE} 1")
    for score, value in expected_counter_values(cases):
        lines.append(
            f"execute unless score {score} {OBJECTIVE} matches {value} run "
            f"scoreboard players add #failures {OBJECTIVE} 1"
        )
    return "\n".join(lines) + "\n"


def phase_wrapper(phase: str, announce: bool) -> str:
    lines = [
        "# Generated by gallery/generate.py; do not edit.",
        f"function {NAMESPACE}:verify",
        f"scoreboard players operation #{phase}_failures {OBJECTIVE} = #failures {OBJECTIVE}",
        f"scoreboard players operation #{phase}_checked {OBJECTIVE} = #checked {OBJECTIVE}",
    ]
    if announce:
        lines.append(
            "tellraw @a [{\"text\":\"Rechiseled: Create gallery "
            f"{phase}: \"}},{{\"score\":{{\"name\":\"#{phase}_checked\","
            f"\"objective\":\"{OBJECTIVE}\"}}}},{{\"text\":\" block checks, \"}},"
            f"{{\"score\":{{\"name\":\"#{phase}_failures\",\"objective\":"
            f"\"{OBJECTIVE}\"}}}},{{\"text\":\" failures\"}}]"
        )
    return "\n".join(lines) + "\n"


def cases_tsv(cases: list[Case]) -> str:
    lines = ["index\tcase_id\tsection\tx\ty\tz\tplacements\tnotes"]
    for index, case in enumerate(cases):
        lines.append(
            f"{index}\t{case.case_id}\t{case.section}\t{case.anchor.x}\t"
            f"{case.anchor.y}\t{case.anchor.z}\t{len(case.placements)}\t{case.notes}"
        )
    return "\n".join(lines) + "\n"


def roster_tsv(rows: list[RosterEntry]) -> str:
    lines = [
        "index\tblock_id\troute_kind\tshape_family\tlegal_states\t"
        "texture_layout\ttexture_sheet\tcounterpart"
    ]
    for index, row in enumerate(rows):
        lines.append(
            f"{index}\t{row.block_id}\t{row.route_kind}\t{row.shape_family}\t"
            f"{row.legal_state_count}\t{row.texture_layout}\t{row.texture_sheet}\t"
            f"{row.counterpart}"
        )
    return "\n".join(lines) + "\n"


def manifest(cases: list[Case], rows: list[RosterEntry]) -> dict[str, Any]:
    all_placements = placements(cases)
    counts = section_counts(cases)
    block_assertions = len(all_placements) + len(FLOOR_SENTINELS)
    counter_assertions = len(expected_counter_values(cases))
    per_phase = block_assertions + counter_assertions
    route_layout_counts = Counter(
        (row.route_kind, row.texture_layout)
        for row in rows
        if row.route_kind != "mechanical-chisel"
    )
    return {
        "schema_version": 1,
        "status": "scope-audit-frozen-gallery-candidate",
        "baseline": {
            "pack": "All the Mons 1.2.0",
            "minecraft": "1.21.1",
            "neoforge": "21.1.248",
            "java": 21,
            "bluemap": "5.22 Java-21 backport",
            "rechiseledcreate": "1.1.1",
            "rechiseled": "1.2.5",
            "fusion": "1.3.12",
            "create": "6.0.10",
        },
        "artifact_pins": LAB_ARTIFACT_PINS,
        "artifact_validation": {
            "locally_revalidated_by_generator": sorted(ARTIFACTS),
            "lab_only_pins": sorted(set(LAB_ARTIFACT_PINS) - set(ARTIFACTS)),
        },
        "frozen_datapack_archive": FROZEN_ARCHIVE,
        "route_scope": {
            "routed_block_ids": len(rows),
            "legal_block_states": sum(row.legal_state_count for row in rows),
            "predicate_connected_ids": sum(
                row.route_kind == "predicate-connected" for row in rows
            ),
            "forced_disconnected_tile0_ids": sum(
                row.route_kind == "forced-disconnected-tile0" for row in rows
            ),
            "mechanical_chisel_ids": sum(
                row.route_kind == "mechanical-chisel" for row in rows
            ),
            "shape_families": Counter(row.shape_family for row in rows),
            "route_layout_counts": {
                f"{kind}:{layout}": count
                for (kind, layout), count in sorted(route_layout_counts.items())
            },
            "source_fusion_sheets": {
                "total": 115,
                "full": 42,
                "simple": 73,
                "referenced_unique": 107,
                "referenced_full": 42,
                "referenced_simple": 65,
                "unused_simple": 8,
            },
        },
        "gallery": {
            "safe_envelope": ENVELOPE,
            "clear_fill_max_volume": 8_192,
            "logical_cases": len(cases),
            "placement_commands": len(all_placements),
            "section_case_counts": dict(sorted(counts.items())),
            "floor_sentinel_checks": len(FLOOR_SENTINELS),
            "review_poses": ["overview", "census", "masks", "states", "bridge"],
        },
        "assertions": {
            "phases": ["immediate", "20t", "100t"],
            "block_assertions_per_phase": block_assertions,
            "counter_assertions_per_phase": counter_assertions,
            "assertions_per_phase": per_phase,
            "total_scheduled_assertions": per_phase * 3,
            "scoreboard_objective": OBJECTIVE,
        },
        "coverage": {
            "canonical_masks": [f"0x{mask:02x}" for _label, mask in MASKS],
            "mask_layouts": ["full", "simple"],
            "selector_unit_contract": {
                "all_input_masks": 256,
                "full_reachable_tiles": 47,
                "full_unreachable_tile": 41,
                "simple_reachable_tiles": 16,
            },
            "representative_slab_legal_states": 6,
            "representative_stair_legal_states": 80,
            "stair_supports": sum(
                len(case.placements) == 2
                for case in cases
                if case.section == "stairs-state"
            ),
            "axis_materials": sorted(WINDOW_LAYOUTS),
            "axis_layouts": sorted({layout for layouts in WINDOW_LAYOUTS.values() for layout in layouts}),
            "mechanical_chisel_states": 24,
            "mechanical_chisel_vertical_tool_states": 8,
            "mechanical_chisel_setup": {
                "kinetic_speed": "neutral-zero-by-isolation",
                "block_entity": "fresh-empty-setblock-created",
                "excluded": ["transported_item", "input_item", "filter_overlay"],
            },
            "forced_disconnected_adjacency": [
                {
                    "layout": "simple",
                    "block": "rechiseledcreate:acacia_window_covered",
                    "same_id_pair": True,
                    "expected_tile": 0,
                },
                {
                    "layout": "full",
                    "block": "rechiseledcreate:acacia_window_tiles",
                    "same_id_pair": True,
                    "expected_tile": 0,
                },
            ],
            "ownership_controls": [
                "rechiseled:andesite_brick_pattern_connecting",
                "create:mechanical_saw",
                "minecraft:stone",
            ],
        },
        "cases": [
            {
                "case_id": case.case_id,
                "section": case.section,
                "anchor": case.anchor.as_dict(),
                "notes": case.notes,
                "metadata": case.metadata,
                "placements": [
                    {
                        **placement.position.as_dict(),
                        "block": placement.block,
                    }
                    for placement in case.placements
                ],
            }
            for case in cases
        ],
    }


def json_bytes(value: Any) -> bytes:
    return (json.dumps(value, indent=2, sort_keys=True) + "\n").encode("utf-8")


def rendered_files(evidence: ArtifactEvidence) -> tuple[dict[Path, bytes], dict[str, Any]]:
    validate_artifact_roster(evidence)
    rows = roster()
    cases = gallery_cases()
    gallery_manifest = manifest(cases, rows)
    files: dict[Path, bytes] = {
        Path("manifest.json"): json_bytes(gallery_manifest),
        Path("cases.tsv"): cases_tsv(cases).encode("utf-8"),
        Path("roster.tsv"): roster_tsv(rows).encode("utf-8"),
        Path("staging/create_staging.conf"): (
            "## Bounded, disposable Rechiseled: Create review map.\n\n"
            "world: \"world\"\n"
            "dimension: \"minecraft:overworld\"\n"
            "name: \"Rechiseled: Create review\"\n\n"
            "start-pos: { x: 208, z: 208 }\n\n"
            "render-mask: [\n"
            "  {\n"
            "    min-x: 160\n"
            "    max-x: 255\n"
            "    min-z: 160\n"
            "    max-z: 255\n"
            "    min-y: 99\n"
            "    max-y: 132\n"
            "  }\n"
            "]\n\n"
            "storage: \"file\"\n"
        ).encode("utf-8"),
        Path("datapack/pack.mcmeta"): json_bytes({
            "pack": {
                "description": "ATM 1.2.0 Rechiseled: Create 1.1.1 BlueMap gallery",
                "pack_format": 48,
            }
        }),
        Path("datapack/data/minecraft/tags/function/load.json"): json_bytes({
            "values": [f"{NAMESPACE}:load"]
        }),
        Path(f"datapack/data/{NAMESPACE}/function/load.mcfunction"): (
            "# Generated by gallery/generate.py; do not edit.\n"
            f"scoreboard objectives add {OBJECTIVE} dummy\n"
            f"forceload add {ENVELOPE['min_x']} {ENVELOPE['min_z']} "
            f"{ENVELOPE['max_x']} {ENVELOPE['max_z']}\n"
        ).encode("utf-8"),
        Path(f"datapack/data/{NAMESPACE}/function/build.mcfunction"):
            build_function(cases).encode("utf-8"),
        Path(f"datapack/data/{NAMESPACE}/function/verify.mcfunction"):
            verify_function(cases).encode("utf-8"),
        Path(f"datapack/data/{NAMESPACE}/function/verify_immediate.mcfunction"):
            phase_wrapper("immediate", True).encode("utf-8"),
        Path(f"datapack/data/{NAMESPACE}/function/verify_20t.mcfunction"):
            phase_wrapper("20t", False).encode("utf-8"),
        Path(f"datapack/data/{NAMESPACE}/function/verify_100t.mcfunction"):
            phase_wrapper("100t", True).encode("utf-8"),
        Path(f"datapack/data/{NAMESPACE}/function/clear.mcfunction"): (
            "# Generated by gallery/generate.py; do not edit.\n"
            f"schedule clear {NAMESPACE}:verify_20t\n"
            f"schedule clear {NAMESPACE}:verify_100t\n"
            + "\n".join(clear_commands())
            + "\n"
        ).encode("utf-8"),
        Path(f"datapack/data/{NAMESPACE}/function/release.mcfunction"): (
            "# Generated by gallery/generate.py; do not edit.\n"
            f"schedule clear {NAMESPACE}:verify_20t\n"
            f"schedule clear {NAMESPACE}:verify_100t\n"
            f"forceload remove {ENVELOPE['min_x']} {ENVELOPE['min_z']} "
            f"{ENVELOPE['max_x']} {ENVELOPE['max_z']}\n"
        ).encode("utf-8"),
        Path(f"datapack/data/{NAMESPACE}/function/pose.mcfunction"): (
            "# Stable alias for the overview review pose.\n"
            f"function {NAMESPACE}:pose_overview\n"
        ).encode("utf-8"),
        Path(f"datapack/data/{NAMESPACE}/function/pose_overview.mcfunction"): (
            "# Generated by gallery/generate.py; do not edit.\n"
            "tp @s 207.5 132 207.5 180 72\n"
        ).encode("utf-8"),
        Path(f"datapack/data/{NAMESPACE}/function/pose_census.mcfunction"): (
            "# Generated by gallery/generate.py; do not edit.\n"
            "tp @s 187.5 116 196.5 180 38\n"
        ).encode("utf-8"),
        Path(f"datapack/data/{NAMESPACE}/function/pose_masks.mcfunction"): (
            "# Generated by gallery/generate.py; do not edit.\n"
            "tp @s 180.5 120 220.5 180 52\n"
        ).encode("utf-8"),
        Path(f"datapack/data/{NAMESPACE}/function/pose_states.mcfunction"): (
            "# Generated by gallery/generate.py; do not edit.\n"
            "tp @s 180.5 124 255 180 48\n"
        ).encode("utf-8"),
        Path(f"datapack/data/{NAMESPACE}/function/pose_bridge.mcfunction"): (
            "# Generated by gallery/generate.py; do not edit.\n"
            "tp @s 254.5 114 210.5 90 40\n"
        ).encode("utf-8"),
    }
    checksum_lines = [
        f"{sha256(content)}  {path.as_posix()}"
        for path, content in sorted(files.items(), key=lambda item: item[0].as_posix())
    ]
    files[Path("SHA256SUMS")] = ("\n".join(checksum_lines) + "\n").encode("ascii")
    return files, gallery_manifest


def write_or_check(files: dict[Path, bytes], check: bool) -> int:
    differences: list[str] = []
    for relative, expected in files.items():
        path = ROOT / relative
        if check:
            if not path.is_file() or path.read_bytes() != expected:
                differences.append(relative.as_posix())
        else:
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_bytes(expected)
    if differences:
        print("generated gallery differs: " + ", ".join(differences), file=sys.stderr)
        return 1
    return 0


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--artifact-dir",
        required=True,
        type=Path,
        help="directory containing the four exact pinned mod artifacts",
    )
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    evidence = load_artifacts(args.artifact_dir.resolve())
    files, gallery_manifest = rendered_files(evidence)
    result = write_or_check(files, args.check)
    if result == 0:
        action = "checked" if args.check else "generated"
        gallery = gallery_manifest["gallery"]
        assertions = gallery_manifest["assertions"]
        print(
            f"{action} {gallery['logical_cases']}-case / "
            f"{gallery['placement_commands']}-placement gallery; "
            f"{assertions['assertions_per_phase']} assertions per phase"
        )
    return result


if __name__ == "__main__":
    raise SystemExit(main())
