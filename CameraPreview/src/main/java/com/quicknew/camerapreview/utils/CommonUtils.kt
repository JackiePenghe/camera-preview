package com.quicknew.camerapreview.utils

import android.os.Build
import android.util.Log

internal var debugEnabled = false


internal fun Any.warnOut(msg: String) {
    if (!debugEnabled) {
        return
    }
    var simpleName = this.javaClass.simpleName
    if (simpleName.isEmpty()) {
        val declaredClasses = this.javaClass.declaredClasses
        simpleName = if (declaredClasses.isNotEmpty()) {
            declaredClasses[0].simpleName
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                this.javaClass.typeName
            } else {
                val split1 = this.javaClass.name.split(".")
                val s = split1[split1.size - 1]
                if (s.contains("$")) {
                    val split2 = s.split("$")
                    if (split2.size >= 2) {
                        split2[split2.size - 2]
                    } else {
                        split2[split2.size - 1]
                    }
                } else {
                    s
                }
            }
        }
    }
    Log.w(simpleName, msg)
}