package com.reddy.vittify.utils

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.reddy.vittify.data.preferences.AppIcon

object IconSwitchingUtils {
    private const val TAG = "IconSwitchingUtils"
    private const val BASE_PACKAGE = "com.reddy.vittify"

    fun switchAppIcon(context: Context, targetIcon: AppIcon) {
        try {
            val packageManager = context.packageManager
            val currentPackageName = context.packageName

            val iconComponents = mapOf(
                AppIcon.ORIGINAL to "$BASE_PACKAGE.MainActivityOriginal",
                AppIcon.ANARCHY to "$BASE_PACKAGE.MainActivityAnarchy",
                AppIcon.ZENITH to "$BASE_PACKAGE.MainActivityZenith",
                AppIcon.MONOCHROME to "$BASE_PACKAGE.MainActivityMonochrome",
                AppIcon.COMIC to "$BASE_PACKAGE.MainActivityComic"
            )

            val targetComponentName = iconComponents[targetIcon] ?: return
            val targetComponent = ComponentName(currentPackageName, targetComponentName)

            // Enable the target icon first to ensure at least one launcher alias is active
            if (packageManager.getComponentEnabledSetting(targetComponent) != PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
                packageManager.setComponentEnabledSetting(
                    targetComponent,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP
                )
            }

            // Disable all other icon components
            iconComponents.forEach { (icon, componentName) ->
                if (icon != targetIcon) {
                    val component = ComponentName(currentPackageName, componentName)
                    if (packageManager.getComponentEnabledSetting(component) != PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
                        packageManager.setComponentEnabledSetting(
                            component,
                            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                            PackageManager.DONT_KILL_APP
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to switch app icon to $targetIcon", e)
        }
    }
}

