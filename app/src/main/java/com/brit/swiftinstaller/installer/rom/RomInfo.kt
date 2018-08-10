package com.brit.swiftinstaller.installer.rom

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.support.v4.content.FileProvider
import com.brit.swiftinstaller.library.BuildConfig
import com.brit.swiftinstaller.library.R
import com.brit.swiftinstaller.utils.*
import java.io.File


@Suppress("NON_FINAL_MEMBER_IN_FINAL_CLASS")
open class RomInfo constructor(var context: Context, var name: String,
                               var version: String) {

    var defaultAccent: Int = 0

    init {
        defaultAccent = context.getColor(R.color.minimal_blue)
    }

    open fun installOverlay(context: Context, targetPackage: String, overlayPath: String) {
        if (ShellUtils.isRootAvailable) {
            runCommand("pm install -r $overlayPath", true)
        }
    }

    open fun postInstall(uninstall: Boolean, apps: ArrayList<String>, oppositeApps: ArrayList<String>?, intent: Intent?) {
        val extraIntent = intent != null

        if (ShellUtils.isRootAvailable) {
            return
        }

        if (apps.contains("android")) {
            val index = apps.indexOf("android")
            apps.removeAt(index)
            apps.add(0, "android")
        }
        if (apps.contains("com.google.android.packageinstaller")) {
            val index = apps.indexOf("com.google.android.packageinstaller")
            apps.removeAt(index)
            apps.add(0, "com.google.android.packageinstaller")
        }

        val intents = Array(if (!extraIntent) {
            apps.size
        } else {
            apps.size + 1
        }) { i ->
            val index = if (extraIntent) {
                i - 1
            } else {
                i
            }
            if (!extraIntent || i > 0) {
                val appInstall = Intent()
                if (uninstall) {
                    appInstall.action = Intent.ACTION_UNINSTALL_PACKAGE
                    appInstall.data = Uri.fromParts("package",
                            Utils.getOverlayPackageName(apps.elementAt(index)), null)
                } else {
                    appInstall.action = Intent.ACTION_INSTALL_PACKAGE
                    appInstall.data = FileProvider.getUriForFile(context,
                            BuildConfig.APPLICATION_ID + ".myprovider",
                            File(Utils.getOverlayPath(apps.elementAt(index))))
                }
                appInstall.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                appInstall.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                appInstall
            } else {
                intent!!.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                intent
            }
        }

        if (!intents.isEmpty()) {
            context.startActivities(intents)
        }

        if (oppositeApps != null && !oppositeApps.isEmpty()) {
            val oppositeIntents = Array(oppositeApps.size) {
                val appInstall = Intent()
                if (!uninstall) {
                    appInstall.action = Intent.ACTION_UNINSTALL_PACKAGE
                    appInstall.data = Uri.fromParts("package",
                            Utils.getOverlayPackageName(oppositeApps[it]), null)
                } else {
                    appInstall.action = Intent.ACTION_INSTALL_PACKAGE
                    appInstall.data = FileProvider.getUriForFile(context,
                            BuildConfig.APPLICATION_ID + ".myprovider",
                            File(Utils.getOverlayPath(oppositeApps[it])))
                }
                appInstall.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                appInstall.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                appInstall
            }
            context.startActivities(oppositeIntents)
        }

        clearAppsToUninstall(context)
        clearAppsToInstall(context)
    }

    open fun uninstallOverlay(context: Context, packageName: String) {
        if (ShellUtils.isRootAvailable) {
            runCommand("pm uninstall " + Utils.getOverlayPackageName(packageName), true)
        } else {
            addAppToUninstall(context, Utils.getOverlayPackageName(packageName))
        }
    }

    @Suppress("unused")
    companion object {

        @SuppressLint("StaticFieldLeak")
        private var sInfo: RomInfo? = null

        @Synchronized
        @JvmStatic
        fun getRomInfo(context: Context): RomInfo {
            if (sInfo == null) {
                sInfo = when {
                    Build.VERSION_CODES.P == Build.VERSION.SDK_INT -> PRomInfo(context, Build.VERSION.CODENAME, Build.VERSION.RELEASE)
                    Utils.isSamsungOreo(context) -> RomInfo(context, Build.VERSION.CODENAME, Build.VERSION.RELEASE)
                    else -> AOSPRomInfo(context, Build.VERSION.CODENAME, Build.VERSION.RELEASE)
                }
            }
            return sInfo!!
        }

        private val isTouchwiz: Boolean
            get() = File("/system/framework/touchwiz.jar").exists()

        @Suppress("DEPRECATION", "unused")
        private fun isOMS(context: Context): Boolean {
            val am = context.getSystemService(ActivityManager::class.java)!!
            val services = am.getRunningServices(Integer.MAX_VALUE)
            for (info in services) {
                if (info.service.className.contains("IOverlayManager")) {
                    return true
                }
            }
            return false
        }

        fun isSupported(@Suppress("UNUSED_PARAMETER") context: Context): Boolean {
            return true
        }
    }
}
