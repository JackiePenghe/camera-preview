@file:Suppress("DEPRECATION")

package com.quicknew.camerapreview.view

import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.Camera
import android.hardware.camera2.CameraCharacteristics
import android.util.AttributeSet
import android.view.TextureView
import android.widget.FrameLayout
import com.quicknew.camerapreview.CameraManagement.cameraEnable
import com.quicknew.camerapreview.CameraManagement.cameraIdList
import com.quicknew.camerapreview.CameraManagement.cameraManager
import com.quicknew.camerapreview.CameraManagement.cameraNumbers
import com.quicknew.camerapreview.CameraManagement.isInit
import com.quicknew.camerapreview.R
import com.quicknew.camerapreview.utils.StaticVariables
import com.quicknew.camerapreview.utils.warnOut

class CameraPreviewView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyles: Int = 0,
    defStyleRes: Int = 0
) : FrameLayout(context, attrs, defStyles, defStyleRes) {

    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *
     * 静态声明
     *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    companion object {

        /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
         *
         * 相机版本枚举
         *
         * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

        enum class CameraVersion(val value: Int) {
            /**
             * 自动选择
             */
            AUTO(0),

            /**
             * 旧版相机[android.hardware.Camera]
             */
            LEGACY(1),

            /**
             * 新版相机[android.hardware.camera2.CameraDevice]
             */
            CAMERA2(2)
        }
    }

    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *
     * 属性声明
     *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    /* * * * * * * * * * * * * * * * * * * 延时初始化属性 * * * * * * * * * * * * * * * * * * */

    /**
     * 旧版相机实例
     */
    @Suppress("DEPRECATION")
    private lateinit var cameraLegacySingle: Camera

    /**
     * 显示预览画面的View
     */
    private lateinit var textureView: TextureView

    /* * * * * * * * * * * * * * * * * * * 可变属性 * * * * * * * * * * * * * * * * * * */

    /**
     * 相机版本，使用camera还是使用camera2
     */
    private var cameraVersion = CameraVersion.AUTO.value

    /* * * * * * * * * * * * * * * * * * * 常量属性 * * * * * * * * * * * * * * * * * * */

    /**
     * surface画布监听
     */
    private val surfaceTextureListener = object : TextureView.SurfaceTextureListener {

        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
            openCamera()
        }

        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {

        }

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
            return true
        }

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {

        }
    }

    /**
     * 旧版相机的单目预览相关回调
     */
    @Suppress("DEPRECATION")
    private val cameraLegacySinglePreviewCallback = Camera.PreviewCallback { data, camera ->
        warnOut("旧版相机的预览数据回调")
    }

    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *
     * 构造方法
     *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    init {
        if (isInit) {
            if (cameraEnable) {
                inflate(context, R.layout.view_camera_preview, this)
                textureView = findViewById(R.id.texture_view)
                obtainStyledAttributes(context, attrs)
            } else {
                warnOut("设备没有相机")
            }
        } else {
            warnOut("相机管理未初始化")
        }
    }

    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *
     * 公开方法
     *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    /**
     * 开启相机预览
     */
    fun startPreview() {
        if (!isInit) {
            warnOut("相机管理未初始化,中止预览")
            return
        }
        if (!cameraEnable) {
            warnOut("设备没有相机，中止预览")
            return
        }
        if (textureView.isAvailable) {
            openCamera()
        } else {
            textureView.surfaceTextureListener = surfaceTextureListener
        }
    }

    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *
     * 私有方法
     *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    /**
     * 获取自定义属性
     */
    private fun obtainStyledAttributes(
        context: Context,
        attrs: AttributeSet?
    ) {
        val obtainStyledAttributes =
            context.obtainStyledAttributes(attrs, R.styleable.CameraPreviewView)
        cameraVersion = obtainStyledAttributes.getInt(
            R.styleable.CameraPreviewView_cameraVersion,
            CameraVersion.AUTO.value
        )
        warnOut("获取相机适配版本 ${getCameraVersion(cameraVersion)}")
        obtainStyledAttributes.recycle()
    }

    /**
     * 获取自定义属性中的相机版本
     */
    private fun getCameraVersion(cameraVersion: Int): String {
        val values = CameraVersion.values()
        for (value in values) {
            if (value.value == cameraVersion) {
                return value.name
            }
        }
        return ""
    }

    /**
     * 是否需要Camera2（检测Camera2的硬件支持情况）
     */
    private fun isCamera2Need(): Boolean {
        if (cameraNumbers == 1) {
            val cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraIdList[0])
            val supportHardwareLevel =
                cameraCharacteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
            return !(supportHardwareLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY || supportHardwareLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED)
        } else {
            var result = true
            for (cameraId in cameraIdList) {
                val cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId)
                val supportHardwareLevel =
                    cameraCharacteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
                val cache =
                    !(supportHardwareLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY || supportHardwareLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED)
                if (!cache) {
                    result = false
                    break
                }
            }
            return result
        }
    }

    /**
     * 使用Camera2进行预览
     */
    private fun openCamera2() {
        //TODO
    }

    /**
     * 使用旧版相机进行预览
     */
    private fun openLegacy() {
        if (cameraNumbers == 1) {
            openLegacySingleCamera()
        } else {
            //TODO 开启多个摄像头
            warnOut("开启多个摄像头")
            openLegacySingleCamera()
        }
    }

    /**
     * 开启单目摄像头
     */
    @Suppress("DEPRECATION")
    private fun openLegacySingleCamera() {
        cameraLegacySingle = Camera.open()
        val parameters = cameraLegacySingle.parameters
        val supportedPreviewSizes = parameters.supportedPreviewSizes
        for (supportedPreviewSize in supportedPreviewSizes) {
            warnOut("${supportedPreviewSize.width}x${supportedPreviewSize.height}")
        }
        parameters.setPreviewSize(
            StaticVariables.cameraPreviewSize.width,
            StaticVariables.cameraPreviewSize.height
        )
        cameraLegacySingle.parameters = parameters
        cameraLegacySingle.setDisplayOrientation(StaticVariables.rotation)
        textureView.surfaceTexture?.setDefaultBufferSize(
            StaticVariables.cameraPreviewSize.width,
            StaticVariables.cameraPreviewSize.height
        )
        cameraLegacySingle.setPreviewTexture(textureView.surfaceTexture)
        cameraLegacySingle.setPreviewCallback(cameraLegacySinglePreviewCallback)
        cameraLegacySingle.startPreview()
    }

    /**
     * 打开相机
     */
    private fun openCamera() {
        if (cameraVersion == CameraVersion.AUTO.value) {
            if (isCamera2Need()) {
                //达到Camera2基本要求，使用Camera2进行预览
                warnOut("自动版本：达到Camera2基本要求，使用Camera2进行预览")
                openCamera2()
            } else {
                //未达到Camera2基本要求，使用旧版进行预览
                warnOut("自动版本：未达到Camera2基本要求，使用旧版进行预览")
                openLegacy()
            }
        } else if (cameraVersion == CameraVersion.LEGACY.value) {
            openLegacy()
        } else if (cameraVersion == CameraVersion.CAMERA2.value) {
            openCamera2()
        }
    }
}