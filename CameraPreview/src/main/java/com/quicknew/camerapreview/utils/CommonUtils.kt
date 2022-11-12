package com.quicknew.camerapreview.utils

import android.os.Build
import android.util.Log

/**
 * 是否开启调试打印
 */
internal var debugEnabled = false

/**
 * 人脸预览区域的宽度
 */
internal var faceCameraViewWidth = 0

/**
 * 人脸预览区域的高度
 */
internal var faceCameraViewHeight = 0

/**
 * 人脸预览区域偏移量-左侧
 */
internal var faceCameraViewLeftOffset = 0

/**
 * 人脸预览区域偏移量-右侧
 */
internal var faceCameraViewRightOffset = 0

/**
 * 人脸预览区域偏移量-顶部
 */
internal var faceCameraViewTopOffset = 0

/**
 * 人脸预览区域偏移量-底部
 */
internal var faceCameraViewBottomOffset = 0

/**
 * 是否使用圆形预览
 */
internal var enableCirclePreview: Boolean = false

/**
 * 圆形预览时，圆心的X坐标
 */
internal var roundX: Int = 0

/**
 * 圆形预览时，圆心的Y坐标
 */
internal var roundY: Int = 0

/**
 * 圆形预览时，半径的大小
 */
internal var radius: Int = 0

/**
 * 使用Log.w打印信息
 */
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

/**
 * 使用Log.w打印信息
 */
internal fun Any.errorOut(msg: String) {
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
    Log.e(simpleName, msg)
}