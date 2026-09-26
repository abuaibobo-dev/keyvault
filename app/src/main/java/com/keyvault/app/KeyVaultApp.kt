package com.keyvault.app

import android.app.Activity
import android.app.Application
import android.os.Bundle

class KeyVaultApp : Application(), Application.ActivityLifecycleCallbacks {
    private var started = 0
    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(this)
    }
    override fun onActivityStarted(activity: Activity) { started++ }
    override fun onActivityStopped(activity: Activity) {
        started--
        if (started <= 0) Session.clear()   // 所有界面都退到后台 → 清空内存密钥
    }
    override fun onActivityCreated(a: Activity, b: Bundle?) {}
    override fun onActivityResumed(a: Activity) {}
    override fun onActivityPaused(a: Activity) {}
    override fun onActivitySaveInstanceState(a: Activity, b: Bundle) {}
    override fun onActivityDestroyed(a: Activity) {}
}
