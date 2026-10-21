# Dreaming-Codes patches for Morphe

Patches by Dreaming-Codes for apps I use. The patches work with [Morphe](https://morphe.software). The Morphe project did not write them and does not endorse them.

## Add the patches to Morphe Manager

Open this link on your phone to add the bundle as a patch source: https://morphe.software/add-source?github=Dreaming-Codes/morphe-patches

## Patches

<!-- PATCHES_START EXPANDED -->

The patch list appears here after the first release.

<!-- PATCHES_END -->

## Build the bundle

The Morphe Gradle plugin is on GitHub Packages. To download it, Gradle needs a GitHub token with the `read:packages` scope:

```sh
GITHUB_ACTOR=<user> GITHUB_TOKEN=<token> ./gradlew :patches:buildAndroid
```

Gradle writes the bundle to `patches/build/libs/patches-<version>.mpp`.

## Release

Commit to `dev` with [Conventional Commits](https://www.conventionalcommits.org). A `feat` or `fix` commit on `dev` publishes a pre-release. Merge `dev` into `main` without a squash to publish a stable release. A `chore` or `docs` commit publishes nothing.

## License

GPLv3. See [LICENSE](LICENSE) and [NOTICE](NOTICE).
