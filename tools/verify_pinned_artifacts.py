#!/usr/bin/env python3
# SPDX-License-Identifier: MIT
"""Fail-closed review gate for the exact ATM 1.2.0 bridge tuple."""

from __future__ import annotations

import argparse
from pathlib import Path
import sys
import zipfile

import generate_profile


def _metadata(path: Path) -> tuple[bytes, list[str]]:
    with zipfile.ZipFile(path) as archive:
        try:
            metadata = archive.read("META-INF/neoforge.mods.toml")
        except KeyError as error:
            raise ValueError(f"missing NeoForge metadata in {path.name}") from error
        return metadata, archive.namelist()


def _verify_mod_metadata(
    bridge: Path, rechiseled: Path, fusion: Path, create: Path
) -> None:
    metadata, names = _metadata(bridge)
    if (b'modId = "rechiseledcreate"' not in metadata
            or b'version = "1.1.1"' not in metadata
            or b'license = "All rights reserved"' not in metadata
            or not any(name.startswith("assets/rechiseledcreate/") for name in names)):
        raise ValueError("Rechiseled: Create NeoForge identity changed")

    metadata, names = _metadata(rechiseled)
    if (
        b'modId = "rechiseled"' not in metadata
        or b'version = "1.2.5"' not in metadata
        or b'license = "All rights reserved"' not in metadata
    ):
        raise ValueError("Rechiseled NeoForge metadata identity changed")
    if not any(name.startswith("assets/rechiseled/") for name in names):
        raise ValueError("Rechiseled archive has no installed resource root")
    if any(name.startswith("earth/terrarium/fusion/") for name in names):
        raise ValueError("Rechiseled archive unexpectedly embeds Fusion classes")

    metadata, names = _metadata(fusion)
    if (b'modId = "fusion"' not in metadata
            or b'version = "1.3.12"' not in metadata
            or b'license = "All rights reserved"' not in metadata
            or not any(name.startswith("com/supermartijn642/fusion/") for name in names)):
        raise ValueError("Fusion NeoForge identity changed")

    metadata, names = _metadata(create)
    if (b'modId = "create"' not in metadata
            or b'version = "6.0.10"' not in metadata
            or b'license = "Read attached LICENSE.md"' not in metadata
            or not any(name.startswith("assets/create/") for name in names)):
        raise ValueError("Create NeoForge identity changed")


def verify(bridge: Path, rechiseled: Path, fusion: Path, create: Path) -> None:
    outputs = generate_profile.build_outputs(bridge, rechiseled, fusion, create)
    generate_profile.write_or_check(outputs, check=True)
    _verify_mod_metadata(bridge, rechiseled, fusion, create)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--bridge", required=True, type=Path)
    parser.add_argument("--rechiseled", required=True, type=Path)
    parser.add_argument("--fusion", required=True, type=Path)
    parser.add_argument("--create", required=True, type=Path)
    args = parser.parse_args()
    try:
        verify(args.bridge, args.rechiseled, args.fusion, args.create)
    except (OSError, ValueError, zipfile.BadZipFile) as error:
        print(f"artifact verification failed: {error}", file=sys.stderr)
        return 1
    print(
        "Verified exact Rechiseled: Create 1.1.1, Rechiseled 1.2.5, "
        "Fusion 1.3.12, and Create 6.0.10 artifacts; 242 routed blocks."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
