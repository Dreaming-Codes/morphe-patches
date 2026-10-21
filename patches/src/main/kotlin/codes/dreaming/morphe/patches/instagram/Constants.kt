package codes.dreaming.morphe.patches.instagram

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility

object Constants {
    val COMPATIBILITY_INSTAGRAM = Compatibility(
        name = "Instagram",
        packageName = "com.instagram.android",
        // APKMirror ships Instagram as an APKM bundle.
        apkFileType = ApkFileType.APKM,
        appIconColor = 0xFC483C,
        targets = listOf(
            // The version the fingerprints were written against.
            AppTarget(version = "447.0.0.55.81"),
        ),
    )
}
