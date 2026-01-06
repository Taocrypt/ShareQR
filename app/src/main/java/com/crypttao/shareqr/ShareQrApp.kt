package com.crypttao.shareqr

import android.app.Application
import com.google.android.material.color.DynamicColors

class ShareQrApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // 启用MD3动态取色（Android 12+有效，低版本自动降级到静态颜色）
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}
