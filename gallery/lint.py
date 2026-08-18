#!/usr/bin/env python3
# SPDX-License-Identifier: MIT
"""Lint generated Rechiseled: Create gallery files without a Minecraft run."""

from __future__ import annotations

import argparse
import csv
import json
from pathlib import Path
import re
import sys
from typing import Any

sys.dont_write_bytecode = True
import generate


ROOT = Path(__file__).resolve().parent
FUNCTION_ROOT = ROOT / "datapack/data"
FUNCTION_REFERENCE = re.compile(r"^[a-z0-9_.-]+:[a-z0-9_./-]+$")
EXECUTE_BLOCK = re.compile(
    r"^execute unless block (-?\d+) (-?\d+) (-?\d+) (\S+) run "
    r"scoreboard players add #failures rc_gallery 1$"
)
EXECUTE_SCORE = re.compile(
    r"^execute unless score (#[a-z0-9_]+) rc_gallery matches (\d+) run "
    r"scoreboard players add #failures rc_gallery 1$"
)


def fail(message: str) -> None:
    raise ValueError(message)


def load_json(path: Path) -> Any:
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as error:
        fail(f"cannot parse JSON {path.relative_to(ROOT)}: {error}")


def in_envelope(x: float, y: float, z: float) -> bool:
    envelope = generate.ENVELOPE
    return (
        envelope["min_x"] <= x <= envelope["max_x"]
        and envelope["min_y"] <= y <= envelope["max_y"]
        and envelope["min_z"] <= z <= envelope["max_z"]
    )


def block_id(block: str) -> str:
    return block.split("[", 1)[0]


def validate_json_files(files: dict[Path, bytes]) -> None:
    for relative in sorted(files):
        if relative.suffix == ".json" or relative.name.endswith(".mcmeta"):
            load_json(ROOT / relative)


def validate_checksums(files: dict[Path, bytes]) -> None:
    checksum_path = ROOT / "SHA256SUMS"
    lines = checksum_path.read_text(encoding="ascii").splitlines()
    expected_paths = set(files) - {Path("SHA256SUMS")}
    seen: set[Path] = set()
    for line in lines:
        match = re.fullmatch(r"([0-9a-f]{64})  ([^\n]+)", line)
        if match is None:
            fail(f"malformed SHA256SUMS line: {line!r}")
        relative = Path(match.group(2))
        if relative.is_absolute() or ".." in relative.parts:
            fail(f"unsafe checksum path: {relative}")
        if relative in seen:
            fail(f"duplicate checksum path: {relative}")
        seen.add(relative)
        path = ROOT / relative
        if not path.is_file():
            fail(f"checksummed file is missing: {relative}")
        actual = generate.sha256(path.read_bytes())
        if actual != match.group(1):
            fail(f"checksum mismatch for {relative}: {actual}")
    if seen != expected_paths:
        fail(
            "checksum path census changed; "
            f"missing={sorted(map(str, expected_paths - seen))}, "
            f"extra={sorted(map(str, seen - expected_paths))}"
        )


def function_path(reference: str) -> Path:
    if FUNCTION_REFERENCE.fullmatch(reference) is None:
        fail(f"invalid function reference: {reference}")
    namespace, name = reference.split(":", 1)
    return ROOT / f"datapack/data/{namespace}/function/{name}.mcfunction"


def validate_function_reference(reference: str) -> None:
    target = function_path(reference)
    if not target.is_file():
        fail(f"missing function target for {reference}: {target.relative_to(ROOT)}")


def parse_coords(tokens: list[str], label: str) -> tuple[int, int, int]:
    try:
        x, y, z = (int(token) for token in tokens)
    except ValueError as error:
        fail(f"non-integer {label} coordinate: {tokens}")
        raise AssertionError from error
    if not in_envelope(x, y, z):
        fail(f"{label} coordinate outside safe envelope: {(x, y, z)}")
    return x, y, z


def validate_functions(manifest: dict[str, Any]) -> None:
    allowed_blocks = {row.block_id for row in generate.roster()} | {
        "rechiseled:andesite_brick_pattern_connecting",
        "create:mechanical_saw",
        "minecraft:stone",
        "minecraft:air",
    }
    setblocks: list[str] = []
    execute_blocks: list[str] = []
    execute_scores: list[tuple[str, int]] = []
    fill_volumes: list[int] = []
    references: set[str] = set()
    function_files = sorted(FUNCTION_ROOT.glob("*/function/**/*.mcfunction"))
    expected_function_count = 14
    if len(function_files) != expected_function_count:
        fail(f"function file census changed: {len(function_files)} != {expected_function_count}")

    for path in function_files:
        relative = path.relative_to(ROOT)
        for number, raw_line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
            line = raw_line.strip()
            if not line or line.startswith("#"):
                continue
            words = line.split()
            command = words[0]
            where = f"{relative}:{number}"
            if command == "function":
                if len(words) != 2:
                    fail(f"malformed function command at {where}: {line}")
                references.add(words[1])
            elif command == "schedule":
                if len(words) not in {3, 5}:
                    fail(f"malformed schedule command at {where}: {line}")
                if words[1] not in {"function", "clear"}:
                    fail(f"unsupported schedule form at {where}: {line}")
                references.add(words[2])
                if words[1] == "function" and words[3:] not in [["20t", "replace"], ["100t", "replace"]]:
                    fail(f"unexpected delayed schedule at {where}: {line}")
                if words[1] == "clear" and len(words) != 3:
                    fail(f"malformed schedule clear at {where}: {line}")
            elif command == "setblock":
                if len(words) != 5:
                    fail(f"malformed setblock at {where}: {line}")
                parse_coords(words[1:4], "setblock")
                if block_id(words[4]) not in allowed_blocks:
                    fail(f"unapproved setblock ID at {where}: {words[4]}")
                setblocks.append(words[4])
            elif command == "fill":
                if len(words) != 8:
                    fail(f"malformed fill at {where}: {line}")
                x1, y1, z1 = parse_coords(words[1:4], "fill start")
                x2, y2, z2 = parse_coords(words[4:7], "fill end")
                if x2 < x1 or y2 < y1 or z2 < z1:
                    fail(f"reversed fill bounds at {where}: {line}")
                if block_id(words[7]) not in {"minecraft:air", "minecraft:stone"}:
                    fail(f"unsupported fill material at {where}: {words[7]}")
                volume = (x2 - x1 + 1) * (y2 - y1 + 1) * (z2 - z1 + 1)
                if volume > 32_768:
                    fail(f"fill volume exceeds command limit at {where}: {volume}")
                fill_volumes.append(volume)
            elif command == "execute":
                block_match = EXECUTE_BLOCK.fullmatch(line)
                if block_match is not None:
                    parse_coords(list(block_match.groups()[:3]), "block assertion")
                    block = block_match.group(4)
                    if block_id(block) not in allowed_blocks:
                        fail(f"unapproved assertion block at {where}: {block}")
                    execute_blocks.append(block)
                    continue
                score_match = EXECUTE_SCORE.fullmatch(line)
                if score_match is not None:
                    execute_scores.append((score_match.group(1), int(score_match.group(2))))
                    continue
                fail(f"unsupported execute command at {where}: {line}")
            elif command == "scoreboard":
                if words[1:3] == ["objectives", "add"]:
                    if words[3:] != [generate.OBJECTIVE, "dummy"]:
                        fail(f"unexpected objective declaration at {where}: {line}")
                elif words[1:3] == ["players", "set"]:
                    if len(words) != 6 or words[4] != generate.OBJECTIVE:
                        fail(f"malformed scoreboard set at {where}: {line}")
                    int(words[5])
                elif words[1:3] == ["players", "add"]:
                    if len(words) != 6 or words[4] != generate.OBJECTIVE:
                        fail(f"malformed scoreboard add at {where}: {line}")
                    int(words[5])
                elif words[1:3] == ["players", "operation"]:
                    if (
                        len(words) != 8
                        or words[4] != generate.OBJECTIVE
                        or words[5] != "="
                        or words[7] != generate.OBJECTIVE
                    ):
                        fail(f"malformed scoreboard operation at {where}: {line}")
                else:
                    fail(f"unsupported scoreboard command at {where}: {line}")
            elif command == "forceload":
                if len(words) != 6 or words[1] not in {"add", "remove"}:
                    fail(f"malformed forceload command at {where}: {line}")
                try:
                    x1, z1, x2, z2 = map(int, words[2:])
                except ValueError:
                    fail(f"non-integer forceload coordinate at {where}: {line}")
                envelope = generate.ENVELOPE
                if (
                    x1 != envelope["min_x"]
                    or z1 != envelope["min_z"]
                    or x2 != envelope["max_x"]
                    or z2 != envelope["max_z"]
                ):
                    fail(f"forceload does not match safe envelope at {where}: {line}")
            elif command == "tp":
                if len(words) != 7 or words[1] != "@s":
                    fail(f"malformed review pose at {where}: {line}")
                try:
                    x, y, z = map(float, words[2:5])
                    float(words[5])
                    float(words[6])
                except ValueError:
                    fail(f"non-numeric review pose at {where}: {line}")
                if not in_envelope(x, y, z):
                    fail(f"review pose outside safe envelope at {where}: {(x, y, z)}")
            elif command == "tellraw":
                if len(words) < 3 or words[1] != "@a":
                    fail(f"malformed tellraw at {where}: {line}")
                payload = line.split(" ", 2)[2]
                try:
                    json.loads(payload)
                except json.JSONDecodeError as error:
                    fail(f"malformed tellraw JSON at {where}: {error}")
            else:
                fail(f"unsupported command at {where}: {line}")

    for reference in references:
        validate_function_reference(reference)
    load_tag = load_json(ROOT / "datapack/data/minecraft/tags/function/load.json")
    if load_tag != {"values": [f"{generate.NAMESPACE}:load"]}:
        fail(f"unexpected load tag: {load_tag}")
    for reference in load_tag["values"]:
        validate_function_reference(reference)

    routed = {row.block_id for row in generate.roster()}
    routed_seen = {block_id(block) for block in setblocks} & routed
    if routed_seen != routed:
        fail(
            "setblock route census incomplete; "
            f"missing={sorted(routed - routed_seen)}, extra={sorted(routed_seen - routed)}"
        )
    gallery = manifest["gallery"]
    assertions = manifest["assertions"]
    if len(setblocks) != gallery["placement_commands"]:
        fail(f"setblock count changed: {len(setblocks)} != {gallery['placement_commands']}")
    if len(execute_blocks) != assertions["block_assertions_per_phase"]:
        fail(
            f"block assertion count changed: {len(execute_blocks)} != "
            f"{assertions['block_assertions_per_phase']}"
        )
    if len(execute_scores) != assertions["counter_assertions_per_phase"]:
        fail(
            f"counter assertion count changed: {len(execute_scores)} != "
            f"{assertions['counter_assertions_per_phase']}"
        )
    if max(fill_volumes, default=0) != gallery["clear_fill_max_volume"]:
        fail(
            f"maximum fill volume changed: {max(fill_volumes, default=0)} != "
            f"{gallery['clear_fill_max_volume']}"
        )

    chisel_states = {
        block for block in setblocks if block_id(block) == generate.MECHANICAL_CHISEL
    }
    if len(chisel_states) != 24:
        fail(f"mechanical chisel state census changed: {len(chisel_states)} != 24")
    if any("data " in line for path in function_files for line in path.read_text().splitlines()):
        fail("gallery must not mutate block-entity inventory/filter/progress NBT")


def validate_manifest(manifest: dict[str, Any]) -> None:
    if manifest.get("status") != "scope-audit-frozen-gallery-candidate":
        fail("gallery must retain the scope-audit-frozen candidate status")
    if manifest.get("frozen_datapack_archive") != generate.FROZEN_ARCHIVE:
        fail("manifest frozen datapack identity changed")
    route_scope = manifest.get("route_scope", {})
    expected = {
        "routed_block_ids": 242,
        "legal_block_states": 2_909,
        "predicate_connected_ids": 163,
        "forced_disconnected_tile0_ids": 78,
        "mechanical_chisel_ids": 1,
    }
    for key, value in expected.items():
        if route_scope.get(key) != value:
            fail(f"manifest {key} changed: {route_scope.get(key)} != {value}")
    gallery = manifest.get("gallery", {})
    if gallery.get("safe_envelope") != generate.ENVELOPE:
        fail("manifest safe envelope differs from generator")
    if gallery.get("logical_cases") != 425 or gallery.get("placement_commands") != 627:
        fail(
            "frozen gallery case/placement census changed: "
            f"{gallery.get('logical_cases')} / {gallery.get('placement_commands')}"
        )
    if gallery.get("section_case_counts", {}).get("forced-disconnected-adjacency") != 2:
        fail("frozen same-ID forced-disconnected fixture count changed")
    assertions = manifest.get("assertions", {})
    if assertions.get("phases") != ["immediate", "20t", "100t"]:
        fail("delayed assertion phases changed")
    if assertions.get("total_scheduled_assertions") != (
        assertions.get("assertions_per_phase", -1) * 3
    ):
        fail("total scheduled assertion count is inconsistent")
    expected_assertions = {
        "block_assertions_per_phase": 631,
        "counter_assertions_per_phase": 14,
        "assertions_per_phase": 645,
        "total_scheduled_assertions": 1_935,
    }
    for key, value in expected_assertions.items():
        if assertions.get(key) != value:
            fail(f"frozen {key} changed: {assertions.get(key)} != {value}")

    cases = manifest.get("cases")
    if not isinstance(cases, list) or len(cases) != gallery.get("logical_cases"):
        fail("manifest case census is inconsistent")
    positions: set[tuple[int, int, int]] = set()
    placement_count = 0
    for case in cases:
        for placement in case.get("placements", []):
            position = (placement.get("x"), placement.get("y"), placement.get("z"))
            if not all(isinstance(value, int) for value in position):
                fail(f"non-integer manifest position: {position}")
            if not in_envelope(*position):
                fail(f"manifest position outside envelope: {position}")
            if position in positions:
                fail(f"duplicate manifest position: {position}")
            positions.add(position)
            placement_count += 1
    if placement_count != gallery.get("placement_commands"):
        fail("manifest placement census is inconsistent")

    adjacency_cases = [
        case
        for case in cases
        if case.get("section") == "forced-disconnected-adjacency"
    ]
    if len(adjacency_cases) != 2:
        fail(f"forced-disconnected adjacency census changed: {len(adjacency_cases)} != 2")
    layouts: set[str] = set()
    for case in adjacency_cases:
        metadata = case.get("metadata", {})
        layout = metadata.get("layout")
        layouts.add(layout)
        if (
            metadata.get("route_kind") != "forced-disconnected-tile0"
            or metadata.get("same_id_adjacency") is not True
            or metadata.get("expected_tile") != 0
        ):
            fail(f"invalid forced-disconnected adjacency metadata: {case.get('case_id')}")
        pair = case.get("placements", [])
        if len(pair) != 2 or pair[0].get("block") != pair[1].get("block"):
            fail(f"forced-disconnected fixture is not a same-ID pair: {case.get('case_id')}")
        first = (pair[0]["x"], pair[0]["y"], pair[0]["z"])
        second = (pair[1]["x"], pair[1]["y"], pair[1]["z"])
        if sum(abs(left - right) for left, right in zip(first, second, strict=True)) != 1:
            fail(f"forced-disconnected fixture is not face-adjacent: {case.get('case_id')}")
        name = block_id(pair[0]["block"]).split(":", 1)[1]
        if name not in generate.ordinary_names() or generate.sheet_layout(name) != layout:
            fail(f"forced-disconnected fixture layout/route mismatch: {case.get('case_id')}")
    if layouts != {"full", "simple"}:
        fail(f"forced-disconnected adjacency layouts changed: {layouts}")


def validate_tabular_files(manifest: dict[str, Any]) -> None:
    with (ROOT / "roster.tsv").open(encoding="utf-8", newline="") as handle:
        roster_rows = list(csv.DictReader(handle, delimiter="\t"))
    if len(roster_rows) != 242:
        fail(f"roster.tsv row count changed: {len(roster_rows)} != 242")
    if sum(int(row["legal_states"]) for row in roster_rows) != 2_909:
        fail("roster.tsv legal state total changed")
    if {row["block_id"] for row in roster_rows} != {
        row.block_id for row in generate.roster()
    }:
        fail("roster.tsv block IDs differ from exact roster")

    with (ROOT / "cases.tsv").open(encoding="utf-8", newline="") as handle:
        case_rows = list(csv.DictReader(handle, delimiter="\t"))
    if len(case_rows) != manifest["gallery"]["logical_cases"]:
        fail("cases.tsv row count differs from manifest")
    if len({row["case_id"] for row in case_rows}) != len(case_rows):
        fail("cases.tsv contains duplicate case IDs")


def validate_map_config() -> None:
    text = (ROOT / "staging/create_staging.conf").read_text(encoding="utf-8")
    expected_literals = (
        'world: "world"',
        'dimension: "minecraft:overworld"',
        "min-x: 160",
        "max-x: 255",
        "min-z: 160",
        "max-z: 255",
        "min-y: 99",
        "max-y: 132",
        'storage: "file"',
    )
    for literal in expected_literals:
        if text.count(literal) != 1:
            fail(f"map config does not contain exactly one {literal!r}")


def validate_generated_file_set(files: dict[Path, bytes]) -> None:
    expected_datapack = {
        path for path in files if path.parts and path.parts[0] == "datapack"
    }
    actual_datapack = {
        path.relative_to(ROOT)
        for path in (ROOT / "datapack").rglob("*")
        if path.is_file()
    }
    if actual_datapack != expected_datapack:
        fail(
            "datapack file set changed; "
            f"missing={sorted(map(str, expected_datapack - actual_datapack))}, "
            f"extra={sorted(map(str, actual_datapack - expected_datapack))}"
        )
    expected_staging = {
        path for path in files if path.parts and path.parts[0] == "staging"
    }
    actual_staging = {
        path.relative_to(ROOT)
        for path in (ROOT / "staging").rglob("*")
        if path.is_file()
    }
    if actual_staging != expected_staging:
        fail("staging generated file set changed")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--artifact-dir", required=True, type=Path)
    args = parser.parse_args()
    evidence = generate.load_artifacts(args.artifact_dir.resolve())
    files, expected_manifest = generate.rendered_files(evidence)
    differences = [
        relative.as_posix()
        for relative, expected in files.items()
        if not (ROOT / relative).is_file() or (ROOT / relative).read_bytes() != expected
    ]
    if differences:
        fail("generated content differs: " + ", ".join(differences))
    manifest = load_json(ROOT / "manifest.json")
    if manifest != expected_manifest:
        fail("manifest differs from the exact generated contract")
    validate_generated_file_set(files)
    validate_json_files(files)
    validate_checksums(files)
    validate_manifest(manifest)
    validate_tabular_files(manifest)
    validate_map_config()
    validate_functions(manifest)
    print(
        "gallery lint passed: "
        f"{manifest['route_scope']['routed_block_ids']} routes / "
        f"{manifest['route_scope']['legal_block_states']} legal states / "
        f"{manifest['gallery']['logical_cases']} cases / "
        f"{manifest['gallery']['placement_commands']} placements / "
        f"{manifest['assertions']['assertions_per_phase']} assertions per phase"
    )
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except ValueError as error:
        print(f"gallery lint failed: {error}", file=sys.stderr)
        raise SystemExit(1)
