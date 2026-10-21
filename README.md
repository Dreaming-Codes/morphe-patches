# Dreaming-Codes patches for Morphe

Patches by Dreaming-Codes for apps I use. The patches work with [Morphe](https://morphe.software). The Morphe project did not write them and does not endorse them.

## Add the patches to Morphe Manager

Open this link on your phone to add the bundle as a patch source: https://morphe.software/add-source?github=Dreaming-Codes/morphe-patches

## Patches

<!-- PATCHES_START EXPANDED -->

The patch list appears here after the first release.

<!-- PATCHES_END -->

## Use the Instagram photo picker patch

The **Use system photo picker** patch adds a **Photo picker** entry to the album list of the Instagram DM gallery and of the stories and reels creation gallery. The entry opens the Android system photo picker, which also lists media from cloud media providers such as [immich-cloud-media](https://github.com/Dreaming-Codes/immich-cloud-media). Instagram copies the picked file into its own storage before it sends it.

The patch has these limits:

- It covers the DM gallery and the stories and reels creation gallery. Other galleries, such as the feed post gallery, do not show the entry.
- It targets Instagram 447.0.0.55.81.
- Nobody has tested it on a device yet. The Morphe CLI applies it to 447.0.0.55.81 without errors.
- Instagram imports a picked item only if the provider reports a known MIME type, such as `image/jpeg` or `video/mp4`.

This bundle does not handle the signature check that Instagram runs after a re-sign. Use it together with [Piko](https://github.com/crimera/piko), which does. To patch Instagram:

1. In Morphe Manager, add this bundle and the Piko bundle as patch sources.
2. Select Instagram 447.0.0.55.81.
3. Keep Piko's default Instagram patches, which include its signature handling.
4. Enable **Use system photo picker** from this bundle.
5. Patch and install.

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
