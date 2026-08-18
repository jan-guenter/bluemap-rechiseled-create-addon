# SPDX-License-Identifier: MIT
"""Unit coverage for deterministic fail-closed profile generation."""

from __future__ import annotations

import importlib.util
from pathlib import Path
import tempfile
import unittest


ROOT = Path(__file__).resolve().parents[2]
SPEC = importlib.util.spec_from_file_location(
    "generate_profile", ROOT / "tools/generate_profile.py"
)
assert SPEC is not None and SPEC.loader is not None
generate_profile = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(generate_profile)


class ProfileHelpersTest(unittest.TestCase):
    def test_roster_digest_is_sorted_and_newline_terminated(self) -> None:
        self.assertEqual(
            generate_profile.roster_digest(["z", "a"]),
            generate_profile.digest_bytes(b"a\nz\n"),
        )

    def test_resource_path_rejects_parent_escape(self) -> None:
        with self.assertRaisesRegex(ValueError, "unsafe resource key"):
            generate_profile.resource_path(
                "rechiseledcreate:../escape", "models", ".json"
            )

    def test_identity_gate_rejects_wrong_filename_before_hashing(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "wrong.jar"
            path.write_bytes(b"")
            with self.assertRaisesRegex(ValueError, "unexpected artifact path"):
                generate_profile._verify_identity(  # noqa: SLF001 - exact helper test
                    path,
                    filename="expected.jar",
                    size=0,
                    sha1="",
                    sha256="",
                    sha512="",
                )

    def test_canonical_json_is_order_independent(self) -> None:
        self.assertEqual(
            generate_profile.canonical_json({"b": 2, "a": 1}),
            b'{\n  "a": 1,\n  "b": 2\n}\n',
        )

    def test_full_profile_constants_lock_exact_closure(self) -> None:
        self.assertEqual(242, generate_profile.ROUTED_COUNT)
        self.assertEqual(2_909, generate_profile.ROUTED_STATE_COUNT)
        self.assertEqual(163, generate_profile.FUSION_ROUTED_COUNT)
        self.assertEqual(78, generate_profile.DISCONNECTED_ROUTED_COUNT)
        self.assertEqual(
            "5a5a6a3eadce26b480c6c0ce38077c8f850eb5e2830bc586e250a38b4f14bdcb",
            generate_profile.DISCONNECTED_SHEET_DIGEST,
        )
        self.assertEqual(432, generate_profile.CUSTOM_MODEL_COUNT)
        self.assertEqual(510, generate_profile.DIRECT_MODEL_COUNT)
        self.assertEqual(513, generate_profile.MODEL_COUNT)
        self.assertEqual(1_038, generate_profile.BRIDGE_RESOURCE_COUNT)
        self.assertEqual(1_041, generate_profile.RESOURCE_COUNT)
        self.assertEqual(9, generate_profile.HOST_MODEL_COUNT)
        self.assertEqual(16, generate_profile.CHISEL_RESOURCE_COUNT)
        self.assertEqual(9, generate_profile.CHISEL_TEXTURE_COUNT)
        host_rows = "".join(
            f"model\t{path}\t{size}\t{sha256}\n"
            for path, size, sha256 in generate_profile.HOST_MODELS
        ).encode("ascii")
        self.assertEqual(
            "edde9209002e63eb5a989daa489cb36edb579806473f66baac4dfe56c07c8b80",
            generate_profile.digest_bytes(host_rows),
        )
        self.assertIn(
            "assets/minecraft/models/item/generated.json",
            {path for path, _, _ in generate_profile.HOST_MODELS},
        )
        self.assertIn(
            "assets/rechiseled/models/item/chisel.json",
            generate_profile.CHISEL_MODELS,
        )
        self.assertEqual(
            "b3e8603aabcb54f4fc51f36b3396880bd03ca0cd51fc60beec0e15e4fd701fb5",
            generate_profile.PATH_CLOSURE_DIGEST,
        )


if __name__ == "__main__":
    unittest.main()
