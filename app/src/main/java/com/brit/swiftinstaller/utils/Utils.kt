package com.brit.swiftinstaller.utils

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Build
import android.os.Bundle
import com.brit.swiftinstaller.installer.rom.RomInfo
import com.brit.swiftinstaller.ui.applist.AppItem
import com.brit.swiftinstaller.utils.OverlayUtils.getOverlayPackageName
import org.json.JSONObject

@Suppress("unused")
object Utils {

    class ListEntry {
        var title: String = ""
        var packageName: String = ""
    }

    private val sortedOverlays = arrayListOf<AppItem>()

    fun sortedOverlaysList(context: Context): ArrayList<AppItem> {
        if (sortedOverlays.isNotEmpty()) return sortedOverlays
        sortedOverlays.clear()
        val disabledOverlays = RomInfo.getRomInfo(context).getDisabledOverlays()
        val hiddenOverlays = getHiddenApps(context)
        val pm = context.packageManager
        val overlays = context.assets.list("overlays") ?: emptyArray()
        for (pn: String in overlays) {
            if (disabledOverlays.contains(pn)) continue
            if (hiddenOverlays.contains(pn)) continue
            var info: ApplicationInfo?
            var pInfo: PackageInfo?
            try {
                info = pm.getApplicationInfo(pn, PackageManager.GET_META_DATA)
                pInfo = pm.getPackageInfo(pn, 0)
            } catch (e: PackageManager.NameNotFoundException) {
                continue
            }
            if (info != null) {
                if (!info.enabled) continue
                val item = AppItem()
                item.packageName = pn
                item.title = info.loadLabel(pm) as String
                item.versionCode = pInfo!!.getVersionCode()
                item.versionName = pInfo.versionName
                sortedOverlays.add(item)
            }
        }
        sortedOverlays.sortWith(Comparator { o1, o2 ->
            o1.title.compareTo(o2.title)
        })
        return sortedOverlays
    }

    fun mapToBundle(map: HashMap<String, String>): Bundle {
        val bundle = Bundle()
        for (key in map.keys) {
            bundle.putString(key, map[key])
        }
        return bundle
    }

    fun bundleToMap(bundle: Bundle): HashMap<String, String> {
        val map = HashMap<String, String>()
        for (key in bundle.keySet()) {
            map[key] = bundle.getString(key) ?: ""
        }
        return map
    }

    fun isAppInstalled(context: Context, packageName: String): Boolean {
        return try {
            val ai = context.packageManager.getApplicationInfo(packageName, 0)
            ai.enabled
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    /*fun checkOverlayStatus() : Boolean {
        try {
            val pi = Class.forName("android.content.pm.PackageInfo")
            for (field : Field in pi.declaredFields) {
                if (field.name == "FLAG_OVERLAY_STATIC" || field.name == "FLAG_OVERLAY_TRUSTED") {
                    return true
                }
            }
        } catch (e : Exception) {
            e.printStackTrace()
        }
        return false
    }*/

    fun isSamsungOreo(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                getProperty("ro.config.knox", "def") != "def"
    }

    fun checkAppVersion(context: Context, packageName: String): Boolean {
        if (!isAppInstalled(context, getOverlayPackageName(packageName))) return false
        val appVersionCode = context.packageManager.getPackageInfo(packageName, 0).getVersionCode()
        val curVersionCode = context.packageManager.getApplicationInfo(
                getOverlayPackageName(packageName),
                PackageManager.GET_META_DATA).metaData.getInt("app_version_code")
        return appVersionCode > curVersionCode
    }

    fun createImage(width:Int, height:Int, color:Int):Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()
        paint.color = color
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        return bitmap
    }

    fun enableAllOverlays(context: Context) : Boolean {
        var hasEnabledOverlays = false
        val overlays = runCommand("cmd overlay list", true).output
        for (overlay in overlays!!.split("\n")) {
            if (overlay.startsWith("[")) {
                val pn = overlay.split("]")[1].trim()
                if (overlay.startsWith("[ ]")) {
                    if (pn.endsWith(".swiftinstaller.overlay")) {
                        runCommand("cmd overlay enable $pn", true)
                        hasEnabledOverlays = true
                    }
                }
            }
        }
        return hasEnabledOverlays
    }

    fun mapToJsonString(map: HashMap<String, String>): String {
        val json = JSONObject()
        for (key in map.keys) {
            json.put(key, map[key])
        }
        return json.toString()
    }

    fun jsonStringToMap(json: String): HashMap<String, String> {
        val jsonObject = JSONObject(json)
        val map = HashMap<String, String>()
        for (key in jsonObject.keys()) {
            map[key] = jsonObject[key] as String
        }
        return map
    }

    /*fun makeKey(key: File) {
        val keyPass = "overlay".toCharArray()

        val keyGen = KeyPairGenerator.getInstance("RSA")
        keyGen.initialize(1024, SecureRandom.getInstance("SHA1PRNG"))
        val keyPair = keyGen.genKeyPair()
        val privKey = keyPair.private

        val cert = generateX509Certificate(keyPair)
        val chain = Array(1, { cert!! })

        val store = KeyStore.getInstance(KeyStore.getDefaultType())
        store.load(null, null)
        store.setKeyEntry("key", privKey, keyPass, chain)
        store.setCertificateEntry("cert", cert)
        store.store(FileOutputStream(key), keyPass)
        key.copyTo(File(Environment.getExternalStorageDirectory(), "signing-key"))
    }

    private fun generateX509Certificate(keyPair: KeyPair): X509Certificate? {
        try {
            val calendar = Calendar.getInstance()
            calendar.time = Date(System.currentTimeMillis())
            val begDate = calendar.time
            calendar.add(Calendar.YEAR, 25)
            val endDate = calendar.time

            val gen = X509V3CertificateGenerator()
            val dnn = X500Principal("CN=swift-installer")
            gen.setSignatureAlgorithm("SHA256WithRSAEncryption")
            gen.setSerialNumber(BigInteger.valueOf(System.currentTimeMillis()))
            gen.setSubjectDN(dnn)
            gen.setIssuerDN(dnn)
            gen.setNotBefore(begDate)
            gen.setNotAfter(endDate)
            gen.setPublicKey(keyPair.public)
            return gen.generate(keyPair.private)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }*/
}