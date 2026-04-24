package com.example.app

import android.app.Application
import com.yandex.mobile.ads.MobileAds

class PasswordGeneratorApp : Application() {
    override fun onCreate() {
        super.onCreate()
        MobileAds.initialize(this) {}
    }
}