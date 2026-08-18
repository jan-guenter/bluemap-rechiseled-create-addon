# Releasing

`0.1.0-alpha.1` is an immutable exact-profile prerelease. The accepted source
and artifact identities are recorded in `provenance/release.json`.

## Required gate

Use Java 21, Gradle 9.6.1, and the exact BlueMap backport checkout at commit
`9be321df995a1103808621d529eb72773e719d4d`:

```bash
python3 -m json.tool provenance/upstreams.json >/dev/null
python3 -m json.tool provenance/release.json >/dev/null
python3 -m unittest discover -s tools/tests -p 'test_*.py'
(cd gallery && sha256sum --check SHA256SUMS)
gradle --no-daemon clean check build \
  generatePomFileForAddonPublication \
  generateMetadataFileForAddonPublication
```

The full local artifact and gallery verifier additionally uses the four exact
operator-supplied mod JARs documented in `README.md`. CI rejects any production
JAR or frozen gallery archive that differs from the owner-accepted size and
SHA-256.

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
