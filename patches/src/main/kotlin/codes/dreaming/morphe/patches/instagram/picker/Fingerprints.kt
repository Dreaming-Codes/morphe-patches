package codes.dreaming.morphe.patches.instagram.picker

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.literal
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
 * Any method that checks the MobileConfig boolean [flag].
 */
class MobileConfigFlagFingerprint(flag: Long) : Fingerprint(
    filters = listOf(
        literal(flag),
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

/**
 * Builds the album list of the album picker. In 447.0.0.55.81 this is
 * `X.GZ3.A01(X.GZ3)`. The external gallery entry has the id -11, and its
 * label and icon are Instagram resources, not the fields of the factory entry.
 */
object AlbumPickerEntryListFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL),
    returnType = "V",
    strings = listOf("Meta View"),
    filters = listOf(
        literal(ALBUM_PICKER_EXTERNAL_ENTRY_ID),
    ),
)

/**
 * Opens the album menu and passes a Google Photos callback only if a
 * MobileConfig flag is on. In 447.0.0.55.81 the stories and reels gallery does
 * this in `X.PXT.G87`, and the full-screen creation gallery does it in `X.E0b.A05`.
 */
object AlbumMenuGooglePhotosFlagFingerprint : Fingerprint(
    filters = listOf(
        literal(GOOGLE_PHOTOS_ENTRY_FLAG),
        methodCall(
            returnType = "V",
            parameters = listOf(
                "Landroid/view/View;",
                "L",
                "Ljava/lang/Integer;",
                "Lkotlin/jvm/functions/Function1;",
                "Z",
            ),
        ),
    ),
)

internal const val ALBUM_PICKER_EXTERNAL_ENTRY_ID = -11L

/**
 * Gates the external gallery entry in the DM media picker. In 447.0.0.55.81 the
 * album menu checks it in `X.OY2` and the album picker config in `X.ZdW`.
 */
internal const val DM_GOOGLE_PHOTOS_ENTRY_FLAG = 36327258591351633L

internal const val GOOGLE_PHOTOS_ENTRY_FLAG = 36318084541522952L
