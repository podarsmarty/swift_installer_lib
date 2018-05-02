package com.brit.swiftinstaller.utils

import android.content.Context
import android.os.AsyncTask

class UpdateChecker(context: Context, val callback: Callback) : AsyncTask<Void, Void, UpdateChecker.Output>() {

    private val context: Context

    init {
        this.context = context.applicationContext
    }

    override fun doInBackground(vararg params: Void?): Output {
        var installedCount = 0
        val updates = ArrayList<String>()

        clearAppsToUpdate(context)
        for (packageName in context.assets.list("overlays")) {
            if (Utils.isOverlayInstalled(context, Utils.getOverlayPackageName(packageName))
                    && Utils.isOverlayInstalled(context, packageName)) {
                installedCount++
                if (Utils.checkOverlayVersion(context, packageName)
                        || Utils.checkAppVersion(context, packageName)) {
                    updates.add(packageName)
                    addAppToUpdate(context, packageName)
                }
            }
        }
        return Output(installedCount, updates)
    }

    override fun onPostExecute(result: Output?) {
        super.onPostExecute(result)
        callback.finished(result!!.installedCount, result.updates)
    }

    abstract class Callback {
        abstract fun finished(installedCount: Int, updates: ArrayList<String>)
    }

    inner class Output(var installedCount: Int, var updates: ArrayList<String>)
}