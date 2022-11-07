package com.quicknew.camerapreview

import android.app.Application

class App:Application() {

    override fun onCreate() {
        super.onCreate()
        CameraManagement.enableDebug(true)
        CameraManagement.init(this)
    }
}