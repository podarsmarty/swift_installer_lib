@file:Suppress("unused")

package com.brit.swiftinstaller.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import android.system.Os
import android.text.TextUtils
import android.util.Log
import com.android.apksig.ApkSigner
import java.io.*
import java.lang.reflect.InvocationTargetException
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.X509Certificate
import java.util.*

@Suppress("unused", "MemberVisibilityCanBePrivate")
object ShellUtils {

    private val TAG = ShellUtils::class.java.simpleName

    val isRootAvailable: Boolean
        get() {
            return try {
                var output: CommandOutput? = runCommand("id", true)
                if (output != null && TextUtils.isEmpty(output.error) && output.exitCode == 0) {
                    output.output != null && output.output!!.contains("uid=0")
                } else {
                    output = runCommand("echo _TEST_", true)
                    output.output!!.contains("_TEST_")
                }
            } catch (e: Exception) {
                false
            }
        }

    fun listFiles(path: String): Array<String> {
        val output = runCommand("ls $path")
        return output.output!!.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
    }

    fun inputStreamToString(`is`: InputStream?): String {
        val s = java.util.Scanner(`is`!!).useDelimiter("\\A")
        return if (s.hasNext()) s.next() else ""
    }

    fun copyFile(path: String, output: String) {
        runCommand("cp $path $output", true)
    }

    fun mkdir(path: String) {
        runCommand("mkdir -p $path", true)
    }

    fun setPermissions(perms: Int, path: String) {
        runCommand("chmod $perms $path", true)
    }

    fun compileOverlay(context: Context, themePackage: String, res: String?, manifest: String,
                       overlayPath: String, assetPath: String?, targetInfo: ApplicationInfo?): CommandOutput {
        var output: CommandOutput
        val overlay = File(overlayPath)
        @Suppress("LocalVariableName")
        val unsigned_unaligned = File(overlay.parent, "unsigned_unaligned" + overlay.name)
        val unsigned = File(overlay.parent, "unsigned_${overlay.name}")
        val overlayFolder = File(context.cacheDir.toString() + "/" + themePackage + "/overlays")
        if (!overlayFolder.exists()) {
            if (!overlayFolder.mkdirs()) {
                Log.e(TAG, "Unable to create " + overlayFolder.absolutePath)
            }
        }

        if (fileExists(unsigned_unaligned.absolutePath) && !deleteFileShell(unsigned_unaligned.absolutePath)) {
            Log.e(TAG, "Unable to delete " + unsigned_unaligned.absolutePath)
        }
        if (fileExists(unsigned.absolutePath) && !deleteFileShell(unsigned.absolutePath)) {
            Log.e(TAG, "Unable to delete " + unsigned.absolutePath)
        }
        if (fileExists(overlay.absolutePath) && !deleteFileShell(overlay.absolutePath)) {
            Log.e(TAG, "Unable to delete " + overlay.absolutePath)
        }

        val cmd = StringBuilder()
        cmd.append(getAapt(context))
        cmd.append(" p")
        cmd.append(" -M ").append(manifest)
        if (res != null) {
            cmd.append(" -S ").append(res)
        }
        if (assetPath != null) {
            cmd.append(" -A ").append(assetPath)
        }
        cmd.append(" -I ").append("/system/framework/framework-res.apk")
        if (targetInfo != null && targetInfo.packageName != "android") {
            cmd.append(" -I ").append(targetInfo.sourceDir)
        }
        cmd.append(" -F ").append(unsigned_unaligned.absolutePath)
        //ShellUtils.runCommand(cmd.toString());
        try {
            val aapt = Runtime.getRuntime().exec(cmd.toString().split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())
            val exitCode = aapt.waitFor()
            val error = inputStreamToString(aapt.errorStream)
            val out = inputStreamToString(aapt.inputStream)
            output = CommandOutput(out, error, exitCode)
            aapt.destroy()
        } catch (e: Exception) {
            output = CommandOutput("", "", 1)
            e.printStackTrace()
        }

        // Zipalign
        if (unsigned_unaligned.exists()) {
            val zipalign = StringBuilder()
            zipalign.append(getZipalign(context))
            zipalign.append(" 4")
            zipalign.append(" ${unsigned_unaligned.absolutePath}")
            zipalign.append(" ${unsigned.absolutePath}")
            runCommand(zipalign.toString())
        }

        if (unsigned.exists()) {
            runCommand("chmod 777 $unsigned", false)
            val key = File(context.dataDir, "/signing-key")
            val keyPass = "overlay".toCharArray()
            if (key.exists()) {
                key.delete()
            }
            //Utils.makeKey(key)

            val ks = KeyStore.getInstance(KeyStore.getDefaultType())
            ks.load(context.assets.open("signing-key"), keyPass)
            val pk = ks.getKey("key", keyPass) as PrivateKey
            val certs = ArrayList<X509Certificate>()
            certs.add(ks.getCertificateChain("key")[0] as X509Certificate)
            val signConfig = ApkSigner.SignerConfig.Builder("overlay", pk, certs).build()
            val signConfigs = ArrayList<ApkSigner.SignerConfig>()
            signConfigs.add(signConfig)
            val signer = ApkSigner.Builder(signConfigs)
            signer.setV1SigningEnabled(false)
                    .setV2SigningEnabled(true)
                    .setInputApk(unsigned)
                    .setOutputApk(overlay)
                    .setMinSdkVersion(Build.VERSION.SDK_INT)
                    .build()
                    .sign()
        }
        return output
    }

    private fun getAapt(context: Context): String? {
        val aapt = File(context.cacheDir, "aapt")
        if (aapt.exists()) return aapt.absolutePath
        if (!AssetHelper.copyAsset(context.assets, "aapt${getArchString()}", aapt.absolutePath, null)) {
            return null
        }
        Os.chmod(aapt.absolutePath, 755)
        return aapt.absolutePath
    }

    private fun getZipalign(context: Context): String? {
        val zipalign = File(context.cacheDir, "zipalign")
        if (zipalign.exists()) return zipalign.absolutePath
        if (!AssetHelper.copyAsset(context.assets, "zipalign${getArchString()}", zipalign.absolutePath, null)) {
            return null
        }
        Os.chmod(zipalign.absolutePath, 755)
        return zipalign.absolutePath
    }

    private fun getArchString(): String {
        if (Arrays.toString(Build.SUPPORTED_ABIS).contains("86")) {
            return "86"
        } else {
            if (Build.SUPPORTED_64_BIT_ABIS.size > 0) {
                return "64"
            }
        }
        return ""
    }
}

fun runCommand(cmd: String): CommandOutput {
    return runCommand(cmd, false)
}

fun runCommand(cmd: String, root: Boolean): CommandOutput {
    var os: DataOutputStream? = null
    var process: Process? = null
    try {

        process = Runtime.getRuntime().exec(if (root) "su" else "sh")
        os = DataOutputStream(process!!.outputStream)
        os.writeBytes(cmd + "\n")
        os.flush()
        os.writeBytes("exit\n")
        os.flush()

        val input = process.inputStream
        val error = process.errorStream

        val `in` = ShellUtils.inputStreamToString(input)
        val err = ShellUtils.inputStreamToString(error)

        input?.close()
        error?.close()

        process.waitFor()

        return CommandOutput(`in`, err, process.exitValue())
    } catch (e: IOException) {
        //e.printStackTrace()
        return CommandOutput("", "", 1)
    } catch (e: InterruptedException) {
        //e.printStackTrace()
        return CommandOutput("", "", 1)
    } finally {
        try {
            os?.close()
            process?.destroy()
        } catch (ignored: IOException) {
        }

    }
}

fun fileExists(path: String): Boolean {
    val output = runCommand("ls $path")
    return output.exitCode == 0
}

fun remountRW(path: String): Boolean {
    return remount(path, "rw")
}

fun remountRO(path: String): Boolean {
    return remount(path, "ro")
}

private fun remount(path: String, type: String): Boolean {
    val readlink = runCommand("readlink $(which mount)")
    val mount: CommandOutput
    mount = if (readlink.output!!.contains("toolbox")) {
        runCommand("mount -o remount,$type $path")
    } else {
        runCommand("mount -o $type,remount $path")
    }
    return mount.exitCode == 0
}

fun deleteFileShell(path: String): Boolean {
    val output = runCommand("rm -rf $path", false)
    return output.exitCode == 0
}

fun deleteFileRoot(path: String): Boolean {
    val output = runCommand("rm -rf $path", true)
    return output.exitCode == 0
}

fun fileExistsRoot(path: String) : Boolean {
    val output = runCommand("test -f $path", true)
    return output.exitCode == 0
}

@SuppressLint("PrivateApi")
fun getProperty(name: String): String? {
    try {
        @SuppressLint("PrivateApi") val clazz = Class.forName("android.os.SystemProperties")
        val m = clazz.getDeclaredMethod("get", String::class.java)
        return m.invoke(null, name) as String
    } catch (e: ClassNotFoundException) {
        e.printStackTrace()
        return null
    } catch (e: NoSuchMethodException) {
        e.printStackTrace()
        return null
    } catch (e: InvocationTargetException) {
        e.printStackTrace()
        return null
    } catch (e: IllegalAccessException) {
        e.printStackTrace()
        return null
    }

}
