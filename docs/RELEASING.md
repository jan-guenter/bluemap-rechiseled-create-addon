# Releasing

`0.1.0-alpha.1` is an immutable exact-profile prerelease. The current
`0.1.0-alpha.2` source is the owner-accepted BlueMap 5.23 release candidate.
Its artifact identities are sealed and publication is authorized. The alpha.1
source and artifact identities remain historical in `provenance/release.json`.

The owner accepted the aggregate integration render on 2026-08-31. Publication
is limited to these exact assets:

| Asset | Bytes | SHA-256 |
| --- | ---: | --- |
| `bluemap-rechiseled-create-addon-0.1.0-alpha.2.jar` | 213,503 | `8e1c5709698e1a2a8313935a4ac138eefc5465c2ec885526b714c706705c99f8` |
| `bluemap-rechiseled-create-addon-0.1.0-alpha.2-sources.jar` | 129,854 | `d632c5a949b9e2f6ecb44aee39303180ffbe3e6690fc8d154041a3fd52ab75d6` |
| `bluemap-rechiseled-create-addon-0.1.0-alpha.2.pom` | 1,378 | `66070ef375d7a566fba39c4930bd92b3db8cb3cfad7e20b515efc5db391d97b3` |
| `bluemap-rechiseled-create-addon-0.1.0-alpha.2.module.json` | 2,890 | `07b572675081739626b63246d0eb0da973aadfea4ae8da32971bdb934b59c216` |
| `SHA256SUMS` | 488 | `333ff1c288feab7c218e3d11907f00810a4b9410a0868b6dd8fa680ad6a77a6b` |

## Required gate

Use Java 21, Gradle 9.6.1, and BlueMap feature-backport commit
`7e07f4e74ec1e92a6ead9aa1e66054af3e133aac`, API commit
`285c9a60eff3ac2b0cab308ce1058d1565be0971`. Initialize all exact source
checkouts first:

```bash
git submodule update --init --recursive -- \
  tooling/bluemap-addon-toolkit \
  modules/bluemap-addon-adapter-api \
  modules/bluemap-fusion-resource-models
```

```bash
python3 -m json.tool provenance/upstreams.json >/dev/null
python3 -m json.tool provenance/release.json >/dev/null
python3 -m unittest discover -s tools/tests -p 'test_*.py'
(cd gallery && sha256sum --check SHA256SUMS)
gradle --no-daemon clean prototypeCheck build \
  generatePomFileForAddonPublication \
  generateMetadataFileForAddonPublication
```

`prototypeCheck` and the gallery verifier additionally use the four exact
operator-supplied mod JARs documented in `README.md`. Release promotion must
seal and then reject any production JAR or sidecar that differs from the new
owner-accepted size and SHA-256.

## Publication contract

- `addon_version` must not contain `-SNAPSHOT`.
- A version change enters `main` through a pull request.
- The release tag is annotated and exactly `v<addon_version>`.
- The tag targets the reviewed merge commit on `main`.
- The tag workflow creates a prerelease, attests both JARs, publishes the
  Maven coordinate, verifies uploaded bytes, then opens the release.
- The tracked gallery sources reproduce the accepted ZIP but the gallery is
  not a GitHub Release or Maven asset.

Publication does not deploy the add-on to a Minecraft server. Installation and
production rollout remain separate operator actions.
