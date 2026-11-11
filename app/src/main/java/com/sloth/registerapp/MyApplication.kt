// MApplication.kt
package com.sloth.registerapp // Certifique-se de que o package name está correto

import android.app.Application
import android.content.Context
import com.sloth.registerapp.core.observability.AppObservability
import com.cySdkyc.clx.Helper
class MyApplication : Application() {

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        Helper.install(this) // 'this' refere-se à instância de MyApplication
    }

    override fun onCreate() {
        super.onCreate()
        AppObservability.configureAppScope()
        AppObservability.breadcrumb(
            category = "app",
            message = "Application started",
            data = mapOf("process" to packageName)
        )
    }
}
