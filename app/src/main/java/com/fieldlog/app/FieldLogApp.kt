package com.fieldlog.app

import android.app.Application
import com.fieldlog.app.data.Entitlement
import com.fieldlog.app.ui.theme.SettingsStore

class FieldLogApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Entitlement.ensureStamped(this)   // see data/Entitlement.kt
        SettingsStore.load(this)          // the user's chosen background + accent
    }
}
