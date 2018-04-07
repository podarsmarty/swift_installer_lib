package com.brit.swiftinstaller.utils.rom

import android.app.ActivityManager
import android.app.AlertDialog
import android.app.Dialog
import android.os.Build
import android.os.Environment
import android.text.TextUtils
import com.brit.swiftinstaller.R
import com.brit.swiftinstaller.ui.activities.MainActivity
import com.brit.swiftinstaller.utils.*
import org.apache.commons.io.FileUtils
import java.io.File
import android.R.attr.targetPackage
import android.app.Activity
import android.support.v4.app.ActivityCompat.startActivityForResult
import android.app.admin.DevicePolicyManager
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.SyncStateContract
import android.support.v4.content.PermissionChecker.checkCallingOrSelfPermission
import android.support.v4.app.ActivityCompat.finishAffinity
import android.support.v4.content.FileProvider
import android.util.Log
import com.brit.swiftinstaller.BuildConfig
import com.brit.swiftinstaller.installer.PackageInstallActivity
import com.brit.swiftinstaller.utils.constants.ACTION_KNOX_LICENSE_STATUS
import com.brit.swiftinstaller.utils.constants.ACTION_LICENSE_STATUS
import com.brit.swiftinstaller.utils.constants.MDM_APP_MGMT_PERM
import com.brit.swiftinstaller.utils.constants.MDM_SECURITY
import com.swift.installer.CustomDialogFragment
import org.bouncycastle.jce.provider.PBE


class RomInfo internal constructor(var context: Context, var name: String,
                                   var version: String, vararg vars: String) {
    private val overlayFolder: String? = null

    var defaultAccent: Int = 0
    var overlayDirectory: String

    init {
        defaultAccent = context.getColor(R.color.minimal_blue)
        if (ShellUtils.isRootAvailable)
            overlayDirectory = context.cacheDir.absolutePath
        else
            overlayDirectory = Environment.getExternalStorageDirectory().absolutePath + ".swift-installer"
    }

    val variants = vars

    //TODO expand installer
    val isSamsung: Boolean
        get() = true

    fun preInstall(context: Context, themePackage: String) {
        //TODO
    }

    fun installOverlay(context: Context, targetPackage: String, overlayPath: String) {
        val installed = Utils.isOverlayInstalled(context, Utils.getOverlayPackageName(targetPackage))
        if (ShellUtils.isRootAvailable) {
            runCommand("pm install -r " + overlayPath, true)
            if (installed) {
                runCommand("cmd overlay enable " + Utils.getOverlayPackageName(targetPackage), true)
            } else {
                addAppToInstall(context, overlayPath)
            }
        } else {
            addAppToInstall(context, overlayPath)
        }
    }

    fun postInstall(uninstall: Boolean) {
        val apps = if (uninstall) { getAppsToUninstall(context) } else { getAppsToInstall(context) }
        Log.d("TEST", "apps - $apps")

        val intents = Array<Intent>(apps.size, { i ->
            Intent(if (uninstall) { Intent.ACTION_DELETE } else { Intent.ACTION_VIEW })
                    .setData( if (!uninstall) { FileProvider.getUriForFile(context,
                            BuildConfig.APPLICATION_ID + ".myprovider",
                            File(apps.elementAt(i))) } else {
                        Uri.fromParts("package", apps.elementAt(i), null) })
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })

        context.startActivities(intents)

        clearAppsToUninstall(context)
        clearAppsToInstall(context)
    }

    fun uninstallOverlay(context: Context, packageName: String) {
        if (ShellUtils.isRootAvailable) {
            runCommand("pm uninstall " + Utils.getOverlayPackageName(packageName), true)
        } else {
            addAppToUninstall(context, Utils.getOverlayPackageName(packageName))
        }
    }

    fun createFinishedDialog(activity: MainActivity): Dialog {
        //TODO
        return AlertDialog.Builder(activity)
                .setMessage("Would you like to reboot?")
                .setPositiveButton(android.R.string.ok
                ) { dialog, which -> }
                .setNegativeButton("Later") { dialog, which -> }.create()
    }

    fun isOverlayCompatible(packageName: String): Boolean {
        return true
    }

    companion object {

        private val TAG = "RomInfo"

        @JvmStatic
        private var sInfo: RomInfo? = null

        @Synchronized
        @JvmStatic
        fun getRomInfo(context: Context): RomInfo {
            if (sInfo == null) {
                sInfo = RomInfo(context, "AOSP", Build.VERSION.RELEASE, "type3-common", "type3_Dark")
            }
            return sInfo!!
        }

        private val isTouchwiz: Boolean
            get() = File("/system/framework/touchwiz.jar").exists()

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

        fun isSupported(context: Context): Boolean {
            return true
        }
    }
}
