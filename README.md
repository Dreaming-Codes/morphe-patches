# Dreaming-Codes patches for Morphe

Patches by Dreaming-Codes for apps I use. The patches work with [Morphe](https://morphe.software). The Morphe project did not write them and does not endorse them.

## Add the patches to Morphe Manager

Open this link on your phone to add the bundle as a patch source: https://morphe.software/add-source?github=Dreaming-Codes/morphe-patches

## Patches

<!-- PATCHES_START EXPANDED -->
> **[v1.0.0-dev.2](https://github.com/Dreaming-Codes/morphe-patches/releases/tag/v1.0.0-dev.2)**&nbsp;&nbsp;•&nbsp;&nbsp;`dev`&nbsp;&nbsp;•&nbsp;&nbsp;1 patches total
<details open>
<summary>📦 Instagram&nbsp;&nbsp;•&nbsp;&nbsp;1 patch</summary>
<br>

**🎯 Supported versions:**

| 447.0.0.55.81 |
| :---: |

| 💊&nbsp;Patch | 📜&nbsp;Description | ⚙️&nbsp;Options |
|----------|----------------|-----------|
| [Use system photo picker](#use-system-photo-picker) | Adds an entry to the DM, stories, and reels gallery album lists that opens the Android system photo picker. The picker also shows media from cloud media providers such as Immich. |  |

</details>

<!-- PATCHES_END -->

## Use the Instagram photo picker patch

The **Use system photo picker** patch adds a **Photo picker** entry to the album list of the Instagram DM gallery, the stories and reels creation gallery, and the full-screen creation gallery. The entry opens the Android system photo picker, which also lists media from cloud media providers such as [immich-cloud-media](https://github.com/Dreaming-Codes/immich-cloud-media). Instagram copies the picked file into its own storage before it sends it.

The patch has these limits:

- It covers only the galleries that already have launch and import code for an external picker. Those are the DM gallery, the stories and reels creation gallery, and the full-screen creation gallery. Other galleries, such as the profile picture gallery and the carousel editor, do not show the entry.
- It targets Instagram 447.0.0.55.81.
- The stories and reels entry works on a Pixel. The other galleries have only been checked in the patched bytecode.
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
