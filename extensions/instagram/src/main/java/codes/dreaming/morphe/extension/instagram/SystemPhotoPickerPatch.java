package codes.dreaming.morphe.extension.instagram;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;

/**
 * Instagram has a hidden "Google Photos" entry in its album list. The entry starts an
 * external picker and imports the returned content Uri through its own import code.
 * The patch points that entry at the Android system photo picker instead.
 */
@SuppressWarnings("unused")
public final class SystemPhotoPickerPatch {
    private static final String TAG = "SystemPhotoPicker";

    // MediaStore.ACTION_PICK_IMAGES, written out so the extension builds against any SDK.
    private static final String ACTION_PICK_IMAGES = "android.provider.action.PICK_IMAGES";

    private SystemPhotoPickerPatch() {
    }

    /**
     * Returns an intent for the system photo picker, or null when the device has no
     * photo picker. Null makes the patched method fall through to Instagram's own code.
     */
    public static Intent getPickerIntent(PackageManager packageManager) {
        try {
            // No type: the picker then shows both photos and videos.
            Intent intent = new Intent(ACTION_PICK_IMAGES);
            if (packageManager == null || intent.resolveActivity(packageManager) == null) {
                Log.w(TAG, "System photo picker not available");
                return null;
            }
            return intent;
        } catch (Throwable t) {
            Log.e(TAG, "getPickerIntent failed", t);
            return null;
        }
    }

    /** Icon of the picker activity, used by the album list entry. Never null. */
    public static Drawable getPickerIcon(PackageManager packageManager, Intent intent) {
        try {
            ResolveInfo info = packageManager.resolveActivity(intent, 0);
            if (info != null) {
                Drawable icon = info.loadIcon(packageManager);
                if (icon != null) return icon;
            }
        } catch (Throwable t) {
            Log.e(TAG, "getPickerIcon failed", t);
        }
        return new ColorDrawable(Color.TRANSPARENT);
    }

    public static String getPickerLabel() {
        return "Photo picker";
    }
}
