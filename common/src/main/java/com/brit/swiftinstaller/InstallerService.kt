package com.brit.swiftinstaller

import android.app.job.JobParameters
import android.app.job.JobService
import com.brit.swiftinstaller.installer.OverlayManager
import com.brit.swiftinstaller.utils.InstallerServiceHelper

class InstallerService : JobService() {
    override fun onStopJob(params: JobParameters?): Boolean {
        return om != null && om!!.isRunning()
    }

    override fun onStartJob(params: JobParameters): Boolean {
        om = OverlayManager(this)
        om!!.setCallback(object : OverlayManager.Callback {
            override fun installFinished() {
                jobFinished(params, false)
            }
        })
        val apps = params.extras.getStringArray(InstallerServiceHelper.EXTRAS_APPS) ?: emptyArray()
        if (params.jobId == InstallerServiceHelper.INSTALL_JOB) {
            om!!.installOverlays(apps)
        } else if (params.jobId == InstallerServiceHelper.UNINSTALL_JOB) {
            om!!.uninstallOverlays(apps)
        }
        return true
    }

    private var om: OverlayManager? = null
}