package com.brit.swiftinstaller.utils

import android.content.Context
import android.os.Bundle
import android.preference.PreferenceManager
import android.text.TextUtils
import android.util.ArraySet
import com.brit.swiftinstaller.R
import com.brit.swiftinstaller.installer.rom.RomInfo

fun getAccentColor(context: Context): Int {
    return PreferenceManager.getDefaultSharedPreferences(context).getInt("accent_color", RomInfo.getRomInfo(context).defaultAccent)
}

fun setAccentColor(context: Context, color: Int) {
    PreferenceManager.getDefaultSharedPreferences(context).edit().putInt("accent_color", color).apply()
}

fun getBackgroundColor(context: Context): Int {
    return PreferenceManager.getDefaultSharedPreferences(context).getInt("background_color", context.getColor(R.color.background_main))
}

fun setBackgroundColor(context: Context, color: Int) {
    PreferenceManager.getDefaultSharedPreferences(context).edit().putInt("background_color", color).apply()
}

fun useBackgroundPalette(context: Context): Boolean {
    return PreferenceManager.getDefaultSharedPreferences(context).getBoolean("background_palette", true)
}

fun setUseBackgroundPalette(context: Context, use: Boolean) {
    PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean("background_palette", use).apply()
}

fun setHideFailedInfoCard(context: Context, hide: Boolean) {
    PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean("hide_failed_info", hide).apply()
}

fun getHideFailedInfoCard(context: Context): Boolean {
    return PreferenceManager.getDefaultSharedPreferences(context).getBoolean("hide_failed_info", false)
}

fun setAppVersion(context: Context, packageName: String, version: Int) {
    val versions = getAppVersions(context)
    versions.putInt(packageName, version)
    setAppVersions(context, versions)
}

fun getAppVersion(context: Context, packageName: String): Int {
    return getAppVersions(context).getInt(packageName, 0)
}

fun getAppVersions(context: Context): Bundle {
    val versions = Bundle()
    val vers = PreferenceManager.getDefaultSharedPreferences(context).getStringSet("overlay_versions", ArraySet<String>())
    for (v in vers) {
        val split = v.split("|")
        versions.putInt(split[0], Integer.parseInt(split[1]))
    }
    return versions
}

fun setAppVersions(context: Context, versions: Bundle) {
    val set = ArraySet<String>()
    for (key in versions.keySet()) {
        set.add("$key|${versions.getInt(key)}")
    }
    PreferenceManager.getDefaultSharedPreferences(context).edit().putStringSet("overlay_versions", set).apply()
}

fun getUserAccents(context: Context): IntArray {
    val temp = PreferenceManager.getDefaultSharedPreferences(context).getString("accents", "")
    if (TextUtils.isEmpty(temp)) {
        return IntArray(0)
    }
    val col = temp.split(",")
    val accents = IntArray(col.size)
    col.indices
            .filterNot { TextUtils.isEmpty(col[it]) }
            .forEach { accents[it] = Integer.parseInt(col[it]) }
    return accents
}

fun getAppsToUpdate(context: Context): Set<String> {
    return PreferenceManager.getDefaultSharedPreferences(context).getStringSet("overlays_to_update", ArraySet<String>())
}

fun addAppToUpdate(context: Context, packageName: String) {
    val apps = getAppsToUpdate(context)
    PreferenceManager.getDefaultSharedPreferences(context).edit().putStringSet("overlays_to_update", apps.plus(packageName)).apply()
}

fun clearAppsToUpdate(context: Context) {
    PreferenceManager.getDefaultSharedPreferences(context).edit().putStringSet("overlays_to_update", ArraySet<String>()).apply()
}

fun getAppsToInstall(context: Context): Set<String> {
    return PreferenceManager.getDefaultSharedPreferences(context).getStringSet("overlays_to_install", ArraySet<String>())
}

fun addAppToInstall(context: Context, packageName: String) {
    val apps = getAppsToInstall(context)
    PreferenceManager.getDefaultSharedPreferences(context).edit().putStringSet("overlays_to_install", apps.plus(packageName)).apply()
}

fun clearAppsToInstall(context: Context) {
    PreferenceManager.getDefaultSharedPreferences(context).edit().putStringSet("overlays_to_install", ArraySet<String>()).apply()
}

fun getAppsToUninstall(context: Context): Set<String> {
    return PreferenceManager.getDefaultSharedPreferences(context).getStringSet("overlays_to_uninstall", ArraySet<String>())
}

fun addAppToUninstall(context: Context, packageName: String) {
    val apps = getAppsToUninstall(context)
    PreferenceManager.getDefaultSharedPreferences(context).edit().putStringSet("overlays_to_uninstall", apps + packageName).apply()
}

fun clearAppsToUninstall(context: Context) {
    PreferenceManager.getDefaultSharedPreferences(context).edit().putStringSet("overlays_to_uninstall", ArraySet<String>()).apply()
}

fun setUserAccents(context: Context, colors: IntArray) {
    if (colors.size > 6) return
    val builder = StringBuilder()
    for (i in colors.indices) {
        if (i > 0) builder.append(",")
        builder.append(colors[i])
    }
    PreferenceManager.getDefaultSharedPreferences(context).edit().putString("accents", builder.toString()).apply()
}

@Suppress("unused")
fun addAccentColor(context: Context, color: Int) {
    val presets = context.resources.getIntArray(R.array.accent_colors)
    for (col: Int in presets) {
        if (col == color)
            return
    }
    val colors = getUserAccents(context)
    val newColors: IntArray
    if (colors.size == 6) {
        colors[0] = color
        newColors = colors
    } else {
        newColors = IntArray(colors.size + 1)
        newColors[0] = color
        for (i in colors.indices) {
            newColors[i + 1] = colors[i]
        }
    }
    setUserAccents(context, newColors)
}