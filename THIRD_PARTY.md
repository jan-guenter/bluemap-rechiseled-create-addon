# Third-party and provenance inventory

| Component | Role | Exact identity | License | Bundled |
| --- | --- | --- | --- | --- |
| BlueMap | Compile-time ABI and adapted renderer mechanics | Feature backport `5.22-feature.backport-5.23-stateless-java-web-server-46`, commit `7e07f4e74ec1e92a6ead9aa1e66054af3e133aac` | MIT | License notice only |
| BlueMap Add-on Adapter API | Four narrow 5.23 adapter helpers | `0.1.0-alpha.2`, commit `e81f08bc4bfbf02d810ec8949a019130e2e61634`, source tree `2f974c9bb2ba13888d69682f86f30f58922d30eb` | MIT | Exact source compiled; no standalone JAR |
| BlueMap Fusion Resource Models | Pure Fusion geometry and selector model | `0.1.0-alpha.1`, commit `3ddd5d39bb7cc8664c242aedd849a636316075c2`, source tree `6e85031ff2f0e7417a7a2fb0babbf7ed5a4f218a` | MIT | Exact source compiled; no standalone JAR |
| BlueMap Rechiseled Add-on | Owner-authored interpreter and scaffold | commit `a5530b3178022c2ba755c3d275debc6adcd47e42` | MIT | No |
| Rechiseled: Create | Operator-installed bridge resources | `1.1.1`, 983,177 bytes, SHA-256 `ba89cd5d1221621ed226cc7f1c26dc84a660cc4f6d122753052429f96d71248d` | All rights reserved | No |
| Rechiseled | Operator-installed parent models and chisel sprite | `1.2.5`, 11,498,611 bytes, SHA-256 `7bf14cf8a4bfdc4b6c990126a75da29fd2bb7559d1c05b71e29c8fd5ae044435` | All rights reserved | No |
| Fusion | Installed format implementation and metadata contract | `1.3.12`, 923,270 bytes, SHA-256 `17f5215648a98bcde4134577b013200dbf363273ae282449c51408ae8346f2fa` | All rights reserved | No |
| Create | Installed mechanical housing/shaft resources | `6.0.10`, 19,123,767 bytes, SHA-256 `ef87fe5709f1ba1f5b8bb20a2925b5afb4669e178fd6d8bf10c167759eefe37a` | MIT code; upstream asset terms | No |
| JUnit Jupiter | Tests | 5.11.4 BOM | EPL-2.0 | No |
| Checkstyle | Source style | 10.18.2 | LGPL-2.1-or-later | No |

Production and sources JAR audits reject nested JARs, upstream namespace
assets, foreign classes, research files, and runtime binaries. The complete
machine-readable record is `provenance/upstreams.json`.
