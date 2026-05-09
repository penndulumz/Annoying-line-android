package com.annoyingline.app.data

import android.content.Context
import android.content.SharedPreferences

class PreferenceManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    var isOverlayEnabled: Boolean
        get() = prefs.getBoolean(KEY_OVERLAY_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_OVERLAY_ENABLED, value).apply()

    var isBootStartEnabled: Boolean
        get() = prefs.getBoolean(KEY_BOOT_START_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_BOOT_START_ENABLED, value).apply()

    var linePosition: Float
        get() = prefs.getFloat(KEY_LINE_POSITION, 0.45f)
        set(value) = prefs.edit().putFloat(KEY_LINE_POSITION, value).apply()

    var lineAlpha: Int
        get() = prefs.getInt(KEY_LINE_ALPHA, 128)
        set(value) = prefs.edit().putInt(KEY_LINE_ALPHA, value).apply()

    var isLauncherIconHidden: Boolean
        get() = prefs.getBoolean(KEY_LAUNCHER_ICON_HIDDEN, false)
        set(value) = prefs.edit().putBoolean(KEY_LAUNCHER_ICON_HIDDEN, value).apply()

    companion object {
        private const val PREF_NAME = "annoying_line_prefs"
        private const val KEY_OVERLAY_ENABLED = "overlay_enabled"
        private const val KEY_BOOT_START_ENABLED = "boot_start_enabled"
        private const val KEY_LINE_POSITION = "line_position"
        private const val KEY_LINE_ALPHA = "line_alpha"
        private const val KEY_LAUNCHER_ICON_HIDDEN = "launcher_icon_hidden"
    }
}
