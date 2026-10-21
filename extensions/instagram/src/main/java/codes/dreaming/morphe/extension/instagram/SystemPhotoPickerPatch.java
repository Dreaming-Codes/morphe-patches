package codes.dreaming.morphe.extension.instagram;

import android.content.Context;
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

    // Instagram's own gallery glyph. The album lists otherwise show the Google Photos logo.
    private static final String ICON_NAME = "instagram_photo_gallery_outline_24";
    private static final String INSTAGRAM_PACKAGE = "com.instagram.android";

    private static int iconRes = -1;

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

    /**
     * Drawable resource for the album picker entry, or the original resource when
     * the glyph is missing. The album picker stores the icon as a resource id.
     */
    public static Integer getPickerIconRes(Integer original) {
        int res = findIconRes();
        return res != 0 ? Integer.valueOf(res) : original;
    }

    /** Drawable for the album menu entry, or the original drawable when the glyph is missing. */
    public static Drawable getPickerIconDrawable(Drawable original) {
        try {
            Context context = getApplicationContext();
            int res = findIconRes();
            if (context != null && res != 0) {
                Drawable icon = context.getDrawable(res);
                if (icon != null) return icon;
            }
        } catch (Throwable t) {
            Log.e(TAG, "getPickerIconDrawable failed", t);
        }
        return original;
    }

    private static int findIconRes() {
        if (iconRes != -1) return iconRes;
        Context context = getApplicationContext();
        if (context == null) return 0;
        int res = 0;
        try {
            // Resources keep the original package name even when the app is renamed.
            res = context.getResources().getIdentifier(ICON_NAME, "drawable", INSTAGRAM_PACKAGE);
            if (res == 0) {
                res = context.getResources().getIdentifier(ICON_NAME, "drawable", context.getPackageName());
            }
        } catch (Throwable t) {
            Log.e(TAG, "findIconRes failed", t);
        }
        if (res == 0) Log.w(TAG, "Icon " + ICON_NAME + " not found");
        iconRes = res;
        return res;
    }

    private static Context getApplicationContext() {
        try {
            return (Context) Class.forName("android.app.ActivityThread")
                    .getMethod("currentApplication")
                    .invoke(null);
        } catch (Throwable t) {
            Log.e(TAG, "getApplicationContext failed", t);
            return null;
        }
    }
}
