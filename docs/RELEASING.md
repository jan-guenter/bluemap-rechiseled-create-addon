# Releasing

`0.1.0-alpha.1` is an immutable exact-profile prerelease. The current
`0.1.0-alpha.2` source is an unpublished BlueMap 5.23 migration candidate.
Its accepted artifact identities remain `PENDING`; do not tag or publish it
until the integration gallery is accepted and those identities are sealed.
The alpha.1 source and artifact identities remain historical in
`provenance/release.json`.

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
