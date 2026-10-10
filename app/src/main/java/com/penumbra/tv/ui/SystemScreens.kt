package com.penumbra.tv.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

/** Opens system settings screens; Google TV lacks some of them, so every call reports success. */
object SystemScreens {
    fun openAccessibility(context: Context): Boolean =
        open(context, Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) ||
            open(context, Intent(Settings.ACTION_SETTINGS))

    fun openOverlayPermission(context: Context): Boolean =
        open(context, Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, packageUri(context))) ||
            open(context, Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))

    fun openWriteSettings(context: Context): Boolean =
        open(context, Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, packageUri(context))) ||
            open(context, Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS))

    private fun packageUri(context: Context) = Uri.parse("package:${context.packageName}")

    private fun open(context: Context, intent: Intent): Boolean = try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (e: ActivityNotFoundException) {
        false
    } catch (e: SecurityException) {
        false
    }
}
