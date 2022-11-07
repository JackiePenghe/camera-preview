package com.quicknew.camerapreview

import android.content.Context
import android.hardware.camera2.CameraManager
import com.quicknew.camerapreview.utils.debugEnabled
import com.quicknew.camerapreview.utils.warnOut

object CameraManagement {

    /**
     * 相机ID列表
     */
    internal lateinit var cameraIdList: Array<String>

    /**
     * 相机管理类
     */
    internal lateinit var cameraManager: CameraManager

    /**
     * 相机数量
     */
    internal var cameraNumbers = 0

    /**
     * 是否初始化
     */
    internal var isInit = false

    /**
     * 相机是否准备就绪
     */
    internal var cameraEnable = false

    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *
     * 公开方法
     *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    /**
     * 开启调试打印
     * @param enable 是否开启调试打印
     */
    fun enableDebug(enable: Boolean) {
        debugEnabled = enable
    }

    /**
     * 初始化
     */
    fun init(context: Context) {
        if (isInit) {
            warnOut("已经初始化，请勿重复初始化")
            return
        }
        isInit = true
        cameraManager =
            context.applicationContext.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        cameraIdList = cameraManager.cameraIdList
        cameraNumbers = cameraIdList.size
        if (cameraNumbers == 0) {
            warnOut("没有检测到摄像头，请求中止")
            return
        }
        cameraEnable = true
    }
}