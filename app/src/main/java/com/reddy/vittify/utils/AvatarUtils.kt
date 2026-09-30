package com.reddy.vittify.utils

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.annotation.DrawableRes
import com.reddy.vittify.R

object AvatarUtils {
    val PRESET_AVATARS = listOf(
        R.drawable.avatar_1,
        R.drawable.avatar_2,
        R.drawable.avatar_3,
        R.drawable.avatar_4,
        R.drawable.avatar_5,
        R.drawable.avatar_6,
        R.drawable.avatar_7,
        R.drawable.avatar_8,
        R.drawable.avatar_9,
        R.drawable.avatar_10
    )

    private val AVATAR_NAME_TO_RES = mapOf(
        "avatar_1" to R.drawable.avatar_1,
        "avatar_2" to R.drawable.avatar_2,
        "avatar_3" to R.drawable.avatar_3,
        "avatar_4" to R.drawable.avatar_4,
        "avatar_5" to R.drawable.avatar_5,
        "avatar_6" to R.drawable.avatar_6,
        "avatar_7" to R.drawable.avatar_7,
        "avatar_8" to R.drawable.avatar_8,
        "avatar_9" to R.drawable.avatar_9,
        "avatar_10" to R.drawable.avatar_10
    )

    /**
     * Constructs a stable resource Uri using the drawable's entry name and current application package name.
     */
    fun getAvatarUri(context: Context, @DrawableRes avatarRes: Int): Uri {
        val entryName = try {
            context.resources.getResourceEntryName(avatarRes)
        } catch (_: Exception) {
            null
        }
        return if (entryName != null) {
            Uri.parse("${ContentResolver.SCHEME_ANDROID_RESOURCE}://${context.packageName}/drawable/$entryName")
        } else {
            Uri.parse("${ContentResolver.SCHEME_ANDROID_RESOURCE}://${context.packageName}/$avatarRes")
        }
    }

    /**
     * Resolves a Uri to a drawable resource ID if it represents one of the preset avatars.
     * Handles named resource URIs (e.g., .../drawable/avatar_3), legacy package URIs, and resource IDs.
     * Returns null if the Uri represents a custom user image (e.g. from gallery file/content).
     */
    fun resolvePresetAvatarRes(context: Context, uri: Uri?): Int? {
        if (uri == null) return null

        val uriString = uri.toString().trim()
        if (uriString.isEmpty() || uriString == "null") return null

        val lastSegment = uri.lastPathSegment?.substringBeforeLast('.')
        if (lastSegment != null) {
            AVATAR_NAME_TO_RES[lastSegment]?.let { return it }
        }

        for ((name, resId) in AVATAR_NAME_TO_RES) {
            if (uriString.endsWith("/$name") ||
                uriString.endsWith("/$name.png") ||
                uriString.endsWith("/$name.webp") ||
                uriString.contains("/drawable/$name/") ||
                uriString.contains("/drawable/$name.") ||
                uriString.contains("/drawable-nodpi/$name/") ||
                uriString.contains("/drawable-nodpi/$name.") ||
                uriString == name
            ) {
                return resId
            }
        }

        if (uri.scheme == ContentResolver.SCHEME_ANDROID_RESOURCE || uri.scheme == "android.resource") {
            // Check if last path segment is a numeric resource ID
            val resId = uri.lastPathSegment?.toIntOrNull()
            if (resId != null) {
                if (PRESET_AVATARS.contains(resId)) {
                    return resId
                }
                try {
                    val entryName = context.resources.getResourceEntryName(resId)
                    AVATAR_NAME_TO_RES[entryName]?.let { return it }
                } catch (_: Exception) {
                    // Resource ID not in current compiled resources
                }
            }

            // Check if last segment is a resource entry name
            uri.lastPathSegment?.let { name ->
                AVATAR_NAME_TO_RES[name]?.let { return it }
                val id = context.resources.getIdentifier(name, "drawable", context.packageName)
                if (id != 0 && PRESET_AVATARS.contains(id)) {
                    return id
                }
            }

            // If it is an android.resource URI that could not be matched to a specific avatar,
            // fall back to avatar_1 so we never attempt to load a broken resource URI with AsyncImage
            return R.drawable.avatar_1
        }

        return null
    }

    /**
     * Checks if a selected Uri matches a specific preset avatar resource.
     */
    fun isAvatarSelected(selectedUri: Uri?, @DrawableRes avatarRes: Int, context: Context): Boolean {
        if (selectedUri == null) return false
        val resolved = resolvePresetAvatarRes(context, selectedUri)
        return resolved == avatarRes
    }
}
