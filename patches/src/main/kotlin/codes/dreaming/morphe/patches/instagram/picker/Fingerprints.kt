package codes.dreaming.morphe.patches.instagram.picker

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.AccessFlags

/**
 * The static factory for the external gallery entry ("Google Photos").
 * In 447.0.0.55.81 this is `X.SCo.A00(PackageManager): X.K15`.
 * It queries ACTION_PICK activities and returns an entry with an Intent,
 * a Drawable and a label, or null when Google Photos is not installed.
 */
object ExternalGalleryEntryFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "L",
    parameters = listOf("Landroid/content/pm/PackageManager;"),
    filters = listOf(
        string("com.google.android.apps.photos"),
        string("Google Photos"),
    ),
)

/**
 * The album picker fragment rebuilds its config from fragment arguments.
 * In 447.0.0.55.81 this is `X.Q4c.onViewCreated`, which calls
 * `new X.Umv(String, int, boolean, boolean, boolean, boolean)`.
 * The last boolean is "args_can_show_google_photos_entry_point".
 */
object AlbumPickerConfigFingerprint : Fingerprint(
    name = "onViewCreated",
    parameters = listOf("Landroid/view/View;", "Landroid/os/Bundle;"),
    filters = listOf(
        string("args_can_show_google_photos_entry_point"),
        methodCall(
            name = "<init>",
            parameters = listOf("Ljava/lang/String;", "I", "Z", "Z", "Z", "Z"),
        ),
    ),
)
