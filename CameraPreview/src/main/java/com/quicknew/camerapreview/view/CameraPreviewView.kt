package com.quicknew.camerapreview.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.Camera
import android.hardware.camera2.*
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.media.Image
import android.media.ImageReader
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.AttributeSet
import android.view.Surface
import android.view.TextureView
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.annotation.RequiresApi
import com.quicknew.camerapreview.CameraManagement.cameraEnable
import com.quicknew.camerapreview.CameraManagement.cameraIdList
import com.quicknew.camerapreview.CameraManagement.cameraManager
import com.quicknew.camerapreview.CameraManagement.cameraNumbers
import com.quicknew.camerapreview.CameraManagement.isInit
import com.quicknew.camerapreview.CameraManagement.threadFactory
import com.quicknew.camerapreview.R
import com.quicknew.camerapreview.interfaces.FrameListener
import com.quicknew.camerapreview.utils.*
import java.util.concurrent.Executor
import java.util.concurrent.ScheduledThreadPoolExecutor


class CameraPreviewView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyles: Int = 0, defStyleRes: Int = 0
) : RelativeLayout(context, attrs, defStyles, defStyleRes) {

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

        /**
         * 默认的相机预览大小-宽度
         */
        private const val DEFAULT_PREVIEW_WIDTH = 640

        /**
         * 默认的相机预览大小-高度
         */
        private const val DEFAULT_PREVIEW_HEIGHT = 480
    }

    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *
     * 属性声明
     *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    /* * * * * * * * * * * * * * * * * * * view自定义属性 * * * * * * * * * * * * * * * * * * */

    /**
     * 彩色相机ID
     */
    private var rgbCameraId = 0

    /**
     * 相机版本，使用camera还是使用camera2
     */
    private var cameraVersion = CameraVersion.AUTO.value

    /**
     * 相机预览分辨率宽度
     */
    private var previewWidth: Int = DEFAULT_PREVIEW_WIDTH

    /**
     * 相机预览分辨率高度
     */
    private var previewHeight: Int = DEFAULT_PREVIEW_HEIGHT

    /**
     * 相机旋转角度 （0，90，180，270）
     */
    private var cameraRotation: Int = 0

    /**
     * 是否使用圆形预览
     */
    private var enableRoundPreview: Boolean = false

    /**
     * 是否交换宽高比
     */
    private var needExchangeWidthAndHeight: Boolean = false

    /**
     * 是否开启镜像
     */
    private var mirrored: Boolean = false

    /**
     * 是否启用双目摄像头
     */
    private var useMultipleCamera: Boolean = true

    /* * * * * * * * * * * * * * * * * * * 可空属性 * * * * * * * * * * * * * * * * * * */

    /**
     * 旧版相机实例
     */
    @Suppress("DEPRECATION")
    private var cameraLegacySingle: Camera? = null

    /**
     * 旧版RGB相机实例
     */
    @Suppress("DEPRECATION")
    private var cameraLegacyRgb: Camera? = null

    /**
     * 旧版黑白相机实例
     */
    @Suppress("DEPRECATION")
    private var cameraLegacyIR: Camera? = null

    /**
     * camera2相机处理Handler
     */
    private var camera2Handler: Handler? = null

    /**
     * camera2单目相机实例
     */
    private var camera2Single: CameraDevice? = null

    /**
     * camera2单目相机图片获取实例
     */
    private var camera2SingleImageReader: ImageReader? = null

    /**
     * camera2双目-RGB相机图片获取实例
     */
    private var camera2RgbImageReader: ImageReader? = null

    /**
     * camera2双目-IR相机图片获取实例
     */
    private var camera2IrImageReader: ImageReader? = null

    /**
     * camera2预览的Surface
     */
    private var camera2PreviewSurface: Surface? = null

    /**
     * camera2预览的SurfaceTexture
     */
    private var camera2PreviewSurfaceTexture: SurfaceTexture? = null

    /**
     * camera2Ir的Surface
     */
    private var camera2IrSurface: Surface? = null

    /**
     * camera2单目相机会话的请求
     */
    private var camera2SingleCaptureRequest: CaptureRequest? = null

    /**
     * camera2 双目相机-RGB相机实例
     */
    private var camera2Rgb: CameraDevice? = null

    /**
     * camera2 双目相机-IR相机实例
     */
    private var camera2IR: CameraDevice? = null

    /**
     * camera2 双目相机-RGB相机会话请求
     */
    private var camera2RgbCaptureRequest: CaptureRequest? = null

    /**
     * camera2 双目相机-IR相机会话请求
     */
    private var camera2IrCaptureRequest: CaptureRequest? = null

    /**
     * 相机数据帧回调
     */
    private var frameListener: FrameListener? = null

    /* * * * * * * * * * * * * * * * * * * 可变属性 * * * * * * * * * * * * * * * * * * */


    /* * * * * * * * * * * * * * * * * * * 延时初始化属性 * * * * * * * * * * * * * * * * * * */

    /**
     * 根布局
     */
    private lateinit var faceRelativeLayout: RelativeLayout

    /**
     * 显示预览画面的View
     */
    private lateinit var previewTextureView: CameraTextureView

    /**
     * ir画面的View
     */
    private lateinit var irTextureView: CameraTextureView

    /* * * * * * * * * * * * * * * * * * * 常量属性 * * * * * * * * * * * * * * * * * * */

    /**
     * camera2执行器实例
     */
    private val camera2Executor: Executor = ScheduledThreadPoolExecutor(1, threadFactory)

    /**
     * preview surface画布监听
     */
    private val previewSurfaceTextureListener = object : TextureView.SurfaceTextureListener {

        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
            warnOut("preview onSurfaceTextureAvailable")
            if (camera2PreviewSurface == null) {
                camera2PreviewSurfaceTexture = surface
                camera2PreviewSurface = Surface(surface)
            }

            if (cameraNumbers == 1) {
                openCamera()
            } else {
                if (irTextureView.isAvailable) {
                    openCamera()
                }
            }
        }

        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
            warnOut("preview onSurfaceTextureSizeChanged")
        }

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
            warnOut("preview onSurfaceTextureDestroyed")
            return true
        }

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
        }
    }

    /**
     * ir surface画布监听
     */
    private val irSurfaceTextureListener = object : TextureView.SurfaceTextureListener {

        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
            warnOut("ir onSurfaceTextureAvailable")
            if (camera2IrSurface == null) {
                camera2IrSurface = Surface(surface)
            }
        }

        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
            warnOut("ir onSurfaceTextureSizeChanged")
        }

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
            warnOut("ir onSurfaceTextureDestroyed")
            return true
        }

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {

        }
    }

    /**
     * 旧版相机的单目相机预览数据回调
     */
    @Suppress("DEPRECATION")
    private val cameraLegacySinglePreviewCallback = Camera.PreviewCallback { data, camera ->
        frameCallbackHandler.post {
            frameListener?.frameDataLegacy(
                data,
                isMultipleCamera = false,
                isRgbData = true,
                previewWidth,
                previewHeight
            )
        }
    }

    /**
     * 旧版相机的双目RGB相机预览相关回调
     */
    @Suppress("DEPRECATION")
    private val cameraLegacyRgbPreviewCallback = Camera.PreviewCallback { data, camera ->
        frameCallbackHandler.post {
            frameListener?.frameDataLegacy(
                data,
                isMultipleCamera = false,
                isRgbData = true,
                previewWidth,
                previewHeight
            )
        }
    }

    /**
     * 旧版相机的双目IR相机预览相关回调
     */
    @Suppress("DEPRECATION")
    private val cameraLegacyIRPreviewCallback = Camera.PreviewCallback { data, camera ->
        frameCallbackHandler.post {
            frameListener?.frameDataLegacy(
                data,
                isMultipleCamera = false,
                isRgbData = true,
                previewWidth,
                previewHeight
            )
        }
    }

    /**
     * camera2相机处理线程
     */
    private val camera2HandlerThread = HandlerThread("camera2HandlerThread")

    /**
     * camera2单目相机回调
     */
    private val camera2SingleStateCallback = object : CameraDevice.StateCallback() {

        override fun onOpened(camera: CameraDevice) {
            warnOut("camera2单目相机已连接")
            val camera2PreviewSurface = camera2PreviewSurface ?: return
            camera2Single = camera
            val camera2SingleCaptureRequestBuilder =
                camera2Single?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            camera2SingleCaptureRequestBuilder?.addTarget(camera2PreviewSurface)
            if (camera2SingleImageReader != null) {
                camera2SingleCaptureRequestBuilder?.addTarget(camera2SingleImageReader!!.surface)
            }
            camera2SingleCaptureRequest = camera2SingleCaptureRequestBuilder?.build()
            if (!isCamera2FullSupport()) {
                errorOut("设备不支持完整的Camera2功能，可能无法获取预览图像信息")
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val previewOutputConfiguration = OutputConfiguration(camera2PreviewSurface)
                //如果设置了高速模式并且设备支持，使用高速模式预览
                val camera2SingleImageReaderOutputConfiguration =
                    if (camera2SingleImageReader != null) {
                        OutputConfiguration(camera2SingleImageReader!!.surface)
                    } else {
                        null
                    }
                val sessionConfiguration = SessionConfiguration(
                    SessionConfiguration.SESSION_REGULAR,
                    if (camera2SingleImageReaderOutputConfiguration != null)
                        listOf(
                            previewOutputConfiguration,
                            camera2SingleImageReaderOutputConfiguration
                        ) else listOf(previewOutputConfiguration),
                    camera2Executor,
                    camera2SingleSessionCallback
                )

                camera2Single?.createCaptureSession(sessionConfiguration)
            } else {
                @Suppress("DEPRECATION")
                camera2Single?.createCaptureSession(
                    if (camera2SingleImageReader == null)
                        listOf(camera2PreviewSurface) else listOf(
                        camera2PreviewSurface,
                        camera2SingleImageReader!!.surface
                    ), camera2SingleSessionCallback, camera2Handler
                )
            }
        }

        override fun onDisconnected(camera: CameraDevice) {
            warnOut("camera2单目相机已断开")
            camera2Single = null
            camera2SingleImageReader = null
        }

        override fun onError(camera: CameraDevice, error: Int) {
            val errMsg: String =
                when (error) {
                    ERROR_CAMERA_IN_USE -> {
                        "相机被占用"
                    }
                    ERROR_MAX_CAMERAS_IN_USE -> {
                        "相机被太多设备使用"
                    }
                    ERROR_CAMERA_DISABLED -> {
                        "系统策略已禁用相机"
                    }
                    ERROR_CAMERA_DEVICE -> {
                        "摄像头致命错误"
                    }
                    ERROR_CAMERA_SERVICE -> {
                        "相机服务致命错误"
                    }
                    else -> {
                        "未知异常"
                    }
                }
            warnOut("camera2单目相机异常 $errMsg")
        }
    }

    /**
     * camera2单目相机会话创建回调
     */
    private val camera2SingleSessionCallback = object : CameraCaptureSession.StateCallback() {

        override fun onConfigured(session: CameraCaptureSession) {
            warnOut("camera2单目相机会话创建成功")
            session.setRepeatingRequest(
                camera2SingleCaptureRequest ?: return, camera2SingleCaptureCallback, camera2Handler
            )
        }

        override fun onConfigureFailed(session: CameraCaptureSession) {
            warnOut("camera2单目相机会话创建失败")
        }
    }

    /**
     * camera2单目相机预览请求回调
     */
    private val camera2SingleCaptureCallback = object : CameraCaptureSession.CaptureCallback() {

        private var hasProcess = false

        override fun onCaptureStarted(
            session: CameraCaptureSession,
            request: CaptureRequest,
            timestamp: Long,
            frameNumber: Long
        ) {
            super.onCaptureStarted(session, request, timestamp, frameNumber)
            hasProcess = false
        }

        override fun onCaptureProgressed(
            session: CameraCaptureSession, request: CaptureRequest, partialResult: CaptureResult
        ) {
            super.onCaptureProgressed(session, request, partialResult)
            hasProcess = true
            getCamera2FrameData(camera2SingleImageReader, isMultipleCamera = false, isRgb = true)
        }

        override fun onCaptureCompleted(
            session: CameraCaptureSession, request: CaptureRequest, result: TotalCaptureResult
        ) {
            super.onCaptureCompleted(session, request, result)
            if (hasProcess) {
                return
            }
            getCamera2FrameData(camera2SingleImageReader, isMultipleCamera = false, isRgb = true)
        }
    }

    /**
     * camera2双目相机-RGB相机相关回调
     */
    private val camera2RgbStateCallback = object : CameraDevice.StateCallback() {

        override fun onOpened(camera: CameraDevice) {
            warnOut("camera2双目相机-RGB相机已连接")
            camera2Rgb = camera
        }

        override fun onDisconnected(camera: CameraDevice) {
            warnOut("camera2双目相机-RGB相机已断开")
            camera2Rgb = null
        }

        override fun onError(camera: CameraDevice, error: Int) {
            val errMsg = when (error) {
                ERROR_CAMERA_IN_USE -> {
                    "相机被占用"
                }
                ERROR_MAX_CAMERAS_IN_USE -> {
                    "相机被太多设备使用"
                }
                ERROR_CAMERA_DISABLED -> {
                    "系统策略已禁用相机"
                }
                ERROR_CAMERA_DEVICE -> {
                    "摄像头致命错误"
                }
                ERROR_CAMERA_SERVICE -> {
                    "相机服务致命错误"
                }
                else -> {
                    "未知异常"
                }
            }
            warnOut("camera2双目相机-RGB相机异常 $errMsg")
        }

    }

    /**
     * camera2双目相机-IR相机相关回调
     */
    private val camera2IrStateCallback = object : CameraDevice.StateCallback() {

        override fun onOpened(camera: CameraDevice) {
            warnOut("camera2双目相机-IR相机已连接")
            camera2IR = camera
            initCamera2MultiSession()
        }

        override fun onDisconnected(camera: CameraDevice) {
            warnOut("camera2双目相机-IR相机已断开")
            camera2IR = null
        }

        override fun onError(camera: CameraDevice, error: Int) {
            val errMsg: String
            when (error) {
                ERROR_CAMERA_IN_USE -> {
                    errMsg = "相机被占用"
                }
                ERROR_MAX_CAMERAS_IN_USE -> {
                    errMsg = "相机被太多设备使用"
                }
                ERROR_CAMERA_DISABLED -> {
                    errMsg = "系统策略已禁用相机"
                }
                ERROR_CAMERA_DEVICE -> {
                    errMsg = "摄像头致命错误"
                }
                ERROR_CAMERA_SERVICE -> {
                    errMsg = "相机服务致命错误"
                }
                else -> {
                    errMsg = "未知异常"
                }
            }
            warnOut("camera2双目相机-IR相机异常 $errMsg")

            when (error) {
                //相机被占用
                ERROR_CAMERA_IN_USE -> {
                }
                //相机被太多设备使用
                ERROR_MAX_CAMERAS_IN_USE -> {
                    warnOut("无法同时开启双目camera2,使用单目方式预览")
                    stopCamera2Preview()
                    openCamera2SingleCamera()
                }
                //系统策略已禁用相机
                ERROR_CAMERA_DISABLED -> {
                }
                //摄像头致命错误
                ERROR_CAMERA_DEVICE -> {
                }
                //相机服务致命错误
                ERROR_CAMERA_SERVICE -> {
                }
                //未知异常
                else -> {
                }
            }
        }
    }

    /**
     * camera2双目相机-RGB相机会话相关回调
     */
    private val camera2RgbSessionCallback = object : CameraCaptureSession.StateCallback() {

        override fun onConfigured(session: CameraCaptureSession) {
            warnOut("camera2双目相机-RGB相机会话创建成功")
            session.setRepeatingRequest(
                camera2RgbCaptureRequest ?: return,
                camera2RgbCaptureCallback,
                camera2Handler
            )
        }

        override fun onConfigureFailed(session: CameraCaptureSession) {
            warnOut("camera2双目相机-RGB相机会话创建失败")
        }
    }

    /**
     * camera2双目相机-RGB相机预览请求回调
     */
    private val camera2RgbCaptureCallback = object : CameraCaptureSession.CaptureCallback() {

        private var hasProcess = false

        override fun onCaptureStarted(
            session: CameraCaptureSession,
            request: CaptureRequest,
            timestamp: Long,
            frameNumber: Long
        ) {
            super.onCaptureStarted(session, request, timestamp, frameNumber)
            hasProcess = false
        }

        override fun onCaptureProgressed(
            session: CameraCaptureSession,
            request: CaptureRequest,
            partialResult: CaptureResult
        ) {
            super.onCaptureProgressed(session, request, partialResult)
            hasProcess = true
            getCamera2FrameData(camera2RgbImageReader, isMultipleCamera = true, isRgb = true)
        }

        override fun onCaptureCompleted(
            session: CameraCaptureSession,
            request: CaptureRequest,
            result: TotalCaptureResult
        ) {
            super.onCaptureCompleted(session, request, result)
            if (hasProcess) {
                return
            }
            getCamera2FrameData(camera2RgbImageReader, isMultipleCamera = true, isRgb = true)
        }
    }

    /**
     * camera2双目相机-IR相机预览请求回调
     */
    private val camera2IrCaptureCallback = object : CameraCaptureSession.CaptureCallback() {

        private var hasProcess = false

        override fun onCaptureStarted(
            session: CameraCaptureSession,
            request: CaptureRequest,
            timestamp: Long,
            frameNumber: Long
        ) {
            super.onCaptureStarted(session, request, timestamp, frameNumber)
            hasProcess = false
        }

        override fun onCaptureProgressed(
            session: CameraCaptureSession,
            request: CaptureRequest,
            partialResult: CaptureResult
        ) {
            super.onCaptureProgressed(session, request, partialResult)
            hasProcess = true
            getCamera2FrameData(camera2RgbImageReader, isMultipleCamera = true, isRgb = false)
        }

        override fun onCaptureCompleted(
            session: CameraCaptureSession,
            request: CaptureRequest,
            result: TotalCaptureResult
        ) {
            super.onCaptureCompleted(session, request, result)
            if (hasProcess) {
                return
            }
            getCamera2FrameData(camera2RgbImageReader, isMultipleCamera = true, isRgb = false)
        }
    }

    /**
     * camera2双目相机-IR相机会话相关回调
     */
    private val camera2IrSessionCallback = object : CameraCaptureSession.StateCallback() {

        override fun onConfigured(session: CameraCaptureSession) {
            warnOut("camera2双目相机-IR相机会话创建成功")
            session.setRepeatingRequest(
                camera2IrCaptureRequest ?: return,
                camera2IrCaptureCallback,
                camera2Handler
            )
        }

        override fun onConfigureFailed(session: CameraCaptureSession) {
            warnOut("camera2双目相机-IR相机会话创建失败")
        }
    }

    private val frameCallbackHandlerThread =
        HandlerThread("frameCallbackHandlerThread").apply { start() }

    private val frameCallbackHandler: Handler = Handler(frameCallbackHandlerThread.looper)

    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *
     * 构造方法
     *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    init {
        if (isInit) {
            if (cameraEnable) {
                faceRelativeLayout = RelativeLayout(context)
                addView(faceRelativeLayout)
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
        //计算缩放比例
        post {
            calculateProportion()
            addPreviewViews()
            if (previewTextureView.isAvailable && irTextureView.isAvailable) {
                if (camera2PreviewSurface == null) {
                    camera2PreviewSurface = Surface(previewTextureView.surfaceTexture)
                }
                if (camera2IrSurface == null) {
                    camera2IrSurface = Surface(irTextureView.surfaceTexture)
                }
                openCamera()
            } else {
                previewTextureView.surfaceTextureListener = previewSurfaceTextureListener
                irTextureView.surfaceTextureListener = irSurfaceTextureListener
            }

        }
    }

    /**
     * 停止预览
     */
    fun stopPreview() {
        if (!isInit) {
            warnOut("相机管理未初始化,中止预览")
            return
        }
        if (!cameraEnable) {
            warnOut("设备没有相机，中止预览")
            return
        }
        if (cameraVersion == CameraVersion.AUTO.value) {
            if (isCamera2FullSupport()) {
                //达到Camera2基本要求，使用Camera2
                warnOut("自动版本：使用Camera2")
                stopCamera2Preview()
            } else {
                //未达到Camera2基本要求，使用旧版相机
                warnOut("自动版本：使用旧版")
                stopLegacyPreview()
            }
        } else if (cameraVersion == CameraVersion.LEGACY.value) {
            stopLegacyPreview()
        } else if (cameraVersion == CameraVersion.CAMERA2.value) {
            stopCamera2Preview()
        }
    }

    /**
     * 设置数据帧回调
     */
    fun setFrameListener(frameListener: FrameListener?) {
        this.frameListener = frameListener
    }

    /* * * * * * * * * * * * * * * * * * * 自定义属性相关方法 * * * * * * * * * * * * * * * * * * */

    /**
     * 设置相机版本
     */
    @Suppress("unused")
    fun setCameraVersion(cameraVersion: CameraVersion) {
        this.cameraVersion = cameraVersion.value
    }

    /**
     * 设置RGB相机ID
     */
    @Suppress("unused")
    fun setRgbCameraId(rgbCameraId: Int) {
        this.rgbCameraId = rgbCameraId
    }

    /**
     * 设置相机预览分辨率
     */
    @Suppress("unused")
    fun setPreviewSize(width: Int, height: Int) {
        previewWidth = width
        previewHeight = height
    }

    /**
     * 设置相机旋转角度
     */
    @Suppress("unused")
    fun setCameraRotation(cameraRotation: Int) {
        this.cameraRotation = cameraRotation
    }

    /**
     * 获取相机旋转角度
     */
    @Suppress("unused")
    fun getCameraRotation(): Int {
        return cameraRotation
    }

    /**
     * 设置是否使用圆形预览
     */
    @Suppress("unused")
    fun setEnableRoundPreview(enableRoundPreview: Boolean) {
        this.enableRoundPreview = enableRoundPreview
    }

    /**
     * 设置是否交换宽高
     */
    @Suppress("unused")
    fun setNeedExchangeWidthAndHeight(needExchangeWidthAndHeight: Boolean) {
        this.needExchangeWidthAndHeight = needExchangeWidthAndHeight
    }

    /**
     * 设置是否开启镜像
     */
    @Suppress("unused")
    fun setMirrored(mirrored: Boolean) {
        this.mirrored = mirrored
    }

    /**
     * 是否开启镜像
     */
    @Suppress("unused")
    fun isMirrored(): Boolean {
        return mirrored
    }

    /**
     * 设置是否使用双目相机
     */
    @Suppress("unused")
    fun setUseMultipleCamera(useMultipleCamera: Boolean) {
        this.useMultipleCamera = useMultipleCamera
    }

    /**
     * 获取是否使用双目相机
     */
    @Suppress("unused")
    fun isUseMultipleCamera(): Boolean {
        return useMultipleCamera
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
        context: Context, attrs: AttributeSet?
    ) {
        val obtainStyledAttributes =
            context.obtainStyledAttributes(attrs, R.styleable.CameraPreviewView)
        cameraVersion = obtainStyledAttributes.getInt(
            R.styleable.CameraPreviewView_cameraVersion, CameraVersion.AUTO.value
        )
        warnOut("相机适配版本 ${getCameraVersion(cameraVersion)}")
        rgbCameraId = obtainStyledAttributes.getInt(R.styleable.CameraPreviewView_rgbCameraId, 0)
        warnOut("彩色相机ID $rgbCameraId")
        previewWidth = obtainStyledAttributes.getInt(
            R.styleable.CameraPreviewView_previewWidth, DEFAULT_PREVIEW_WIDTH
        )
        previewHeight = obtainStyledAttributes.getInt(
            R.styleable.CameraPreviewView_previewHeight, DEFAULT_PREVIEW_HEIGHT
        )
        warnOut("相机预览分辨率 ${previewWidth}x${previewHeight}")
        cameraRotation =
            obtainStyledAttributes.getInt(R.styleable.CameraPreviewView_cameraRotation, 0)
        warnOut("相机旋转角度 $cameraRotation")
        enableRoundPreview = obtainStyledAttributes.getBoolean(
            R.styleable.CameraPreviewView_enableRoundPreview, false
        )
        warnOut("相机是否使用圆形预览 $enableRoundPreview")
        needExchangeWidthAndHeight = obtainStyledAttributes.getBoolean(
            R.styleable.CameraPreviewView_needExchangeWidthAndHeight, false
        )
        warnOut("是否交换宽高比 $needExchangeWidthAndHeight")
        mirrored =
            obtainStyledAttributes.getBoolean(R.styleable.CameraPreviewView_cameraMirror, false)
        warnOut("是否镜像展示画面 $mirrored")
        useMultipleCamera =
            obtainStyledAttributes.getBoolean(R.styleable.CameraPreviewView_useMultipleCamera, true)
        warnOut("是否启用双目摄像头 $useMultipleCamera")
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
     * 是否支持完整的Camera2（检测Camera2的硬件支持情况）
     */
    private fun isCamera2FullSupport(): Boolean {
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
        if (camera2Handler == null) {
            camera2HandlerThread.start()
            camera2Handler = Handler(camera2HandlerThread.looper)
            previewTextureView.surfaceTexture?.setDefaultBufferSize(previewWidth, previewHeight)
        }
        if (useMultipleCamera) {
            if (cameraNumbers == 1) {
                openCamera2SingleCamera()
            } else {
                warnOut("开启多个摄像头")
                openCamera2MultiCamera()
            }
        } else {
            openCamera2SingleCamera()
        }
    }

    /**
     * 使用旧版相机进行预览
     */
    private fun openLegacy() {
        if (useMultipleCamera) {
            if (cameraNumbers == 1) {
                openLegacySingleCamera()
            } else {
                warnOut("开启多个摄像头")
                openLegacyMultiCamera()
            }
        } else {
            openLegacySingleCamera()
        }
    }

    /**
     * 开启双目摄像头(旧版)
     */
    @Suppress("DEPRECATION")
    private fun openLegacyMultiCamera() {
        cameraLegacyRgb = Camera.open(rgbCameraId)
        val parametersRgb = cameraLegacyRgb?.parameters
        val supportedPreviewSizesRgb = parametersRgb?.supportedPreviewSizes
        for (supportedPreviewSize in supportedPreviewSizesRgb ?: listOf()) {
            warnOut("${supportedPreviewSize.width}x${supportedPreviewSize.height}")
        }
        parametersRgb?.setPreviewSize(
            previewWidth, previewHeight
        )
        cameraLegacyRgb?.parameters = parametersRgb
        cameraLegacyRgb?.setDisplayOrientation(cameraRotation)
        cameraLegacyRgb?.setPreviewTexture(previewTextureView.surfaceTexture)
        cameraLegacyRgb?.setPreviewCallback(cameraLegacyRgbPreviewCallback)
        cameraLegacyRgb?.startPreview()

        try {
            cameraLegacyIR = Camera.open(if ((rgbCameraId == 1)) 0 else 1)
            val parametersIR = cameraLegacyIR?.parameters
            val supportedPreviewSizesIR = parametersIR?.supportedPreviewSizes
            for (supportedPreviewSize in supportedPreviewSizesIR ?: listOf()) {
                warnOut("${supportedPreviewSize.width}x${supportedPreviewSize.height}")
            }
            parametersIR?.setPreviewSize(
                previewWidth, previewHeight
            )
            cameraLegacyIR?.parameters = parametersIR
            cameraLegacyIR?.setDisplayOrientation(cameraRotation)
            cameraLegacyIR?.setPreviewCallback(cameraLegacyIRPreviewCallback)
            cameraLegacyIR?.startPreview()
        } catch (e: Exception) {
            cameraLegacyRgb?.stopPreview()
            cameraLegacyIR?.stopPreview()
            //无法同时打开双目，使用单目模式打开
            openLegacySingleCamera()
        }
    }

    /**
     * 开启单目摄像头(旧版)
     */
    @Suppress("DEPRECATION")
    private fun openLegacySingleCamera() {
        cameraLegacySingle = Camera.open()
        val parameters = cameraLegacySingle?.parameters
        val supportedPreviewSizes = parameters?.supportedPreviewSizes
        for (supportedPreviewSize in supportedPreviewSizes ?: listOf()) {
            warnOut("${supportedPreviewSize.width}x${supportedPreviewSize.height}")
        }
        parameters?.setPreviewSize(
            previewWidth, previewHeight
        )
        cameraLegacySingle?.parameters = parameters
        cameraLegacySingle?.setDisplayOrientation(cameraRotation)
        cameraLegacySingle?.setPreviewTexture(previewTextureView.surfaceTexture)
        cameraLegacySingle?.setPreviewCallback(cameraLegacySinglePreviewCallback)
        cameraLegacySingle?.startPreview()
    }

    /**
     * 打开相机
     */
    private fun openCamera() {
        if (cameraVersion == CameraVersion.AUTO.value) {
            if (isCamera2FullSupport()) {
                //达到Camera2基本要求，使用Camera2
                warnOut("自动版本：达到Camera2基本要求，使用Camera2")
                openCamera2()
            } else {
                //未达到Camera2基本要求，使用旧版相机
                warnOut("自动版本：未达到Camera2基本要求，使用旧版相机")
                openLegacy()
            }
        } else if (cameraVersion == CameraVersion.LEGACY.value) {
            openLegacy()
        } else if (cameraVersion == CameraVersion.CAMERA2.value) {
            if (!isCamera2FullSupport()) {
                errorOut("设备不支持完整的Camera2功能，可能出现未知的异常，可能无法获取预览图像信息")
            }
            openCamera2()
        }
    }

    /**
     * 计算缩放比例
     */
    private fun calculateProportion() {
        val surfaceViewWidth: Int
        val surfaceViewHeight: Int
        if (enableRoundPreview) {
            if (width < height) {
                surfaceViewWidth = width
                surfaceViewHeight = width
            } else {
                surfaceViewWidth = height
                surfaceViewHeight = height
            }
        } else {
            surfaceViewWidth = width
            surfaceViewHeight = height
        }

        val cameraWidth: Int
        val cameraHeight: Int
        val layoutParams = faceRelativeLayout.layoutParams as LayoutParams
        //判断相机方向需要改变更改宽和高
        if (needExchangeWidthAndHeight) {
            cameraWidth = previewWidth
            cameraHeight = previewHeight
        } else {
            cameraWidth = previewHeight
            cameraHeight = previewWidth
        }
        //如果相机比例相等 不做拉伸
        if (proportion(
                surfaceViewWidth, cameraWidth
            ) == proportion(
                surfaceViewHeight, cameraHeight
            )
        ) {
            layoutParams.width = surfaceViewWidth
            layoutParams.height = surfaceViewHeight
            faceCameraViewWidth = layoutParams.width
            faceCameraViewHeight = layoutParams.height
            faceCameraViewLeftOffset = 0
            faceCameraViewRightOffset = surfaceViewWidth
            faceCameraViewTopOffset = 0
            faceCameraViewDownOffset = surfaceViewHeight
        } else  //如果相机与画布比例不等 ，计算画布大小 以及识别范围
            if (proportion(
                    surfaceViewWidth, cameraWidth
                ) > proportion(
                    surfaceViewHeight, cameraHeight
                )
            ) {
                layoutParams.width = surfaceViewWidth
                layoutParams.height = (cameraHeight * proportion(
                    surfaceViewWidth, cameraWidth
                )).toInt()
                //                layoutParams.topMargin = (surfaceViewHeight - layoutParams.height) / 2;
                layoutParams.bottomMargin = surfaceViewHeight - layoutParams.height
                faceCameraViewWidth = layoutParams.width
                faceCameraViewHeight = layoutParams.height
                faceCameraViewLeftOffset = 0
                faceCameraViewRightOffset = surfaceViewWidth
                faceCameraViewTopOffset = -layoutParams.topMargin
                faceCameraViewDownOffset = -layoutParams.topMargin + surfaceViewHeight
            } //如果相机与画布比例不等 ，计算画布大小 以及识别范围
            else {
                layoutParams.width = (cameraWidth * proportion(
                    surfaceViewHeight, cameraHeight
                )).toInt()
                layoutParams.height = surfaceViewHeight
                layoutParams.leftMargin = (surfaceViewWidth - layoutParams.width) / 2
                layoutParams.rightMargin = (surfaceViewWidth - layoutParams.width) / 2
                faceCameraViewWidth = layoutParams.width
                faceCameraViewHeight = layoutParams.height
                faceCameraViewLeftOffset = -layoutParams.leftMargin
                faceCameraViewRightOffset = -layoutParams.leftMargin + surfaceViewWidth
                faceCameraViewTopOffset = 0
                faceCameraViewDownOffset = surfaceViewHeight
            }

        faceRelativeLayout.layoutParams = layoutParams
        faceRelativeLayout.requestLayout()
    }

    /**
     * 计算比例
     */
    private fun proportion(value1: Int, value2: Int): Double {
        return value1.toDouble() / value2.toDouble()
    }

    /**
     * 添加预览相关的View
     */
    private fun addPreviewViews() {
        if (faceRelativeLayout.childCount > 0) {
            if (mirrored) {
                previewTextureView.scaleX = -1f
            } else {
                previewTextureView.scaleX = 1f
            }
            return
        }

        irTextureView = CameraTextureView(context)
        faceRelativeLayout.addView(irTextureView)

        previewTextureView = CameraTextureView(context)
        faceRelativeLayout.addView(previewTextureView)
        if (mirrored) {
            previewTextureView.scaleX = -1f
        } else {
            previewTextureView.scaleX = 1f
        }
        val layoutParamsPreview = previewTextureView.layoutParams
        layoutParamsPreview.width = ViewGroup.LayoutParams.MATCH_PARENT
        layoutParamsPreview.height = ViewGroup.LayoutParams.MATCH_PARENT
        previewTextureView.layoutParams = layoutParamsPreview

        val layoutParamsIr = irTextureView.layoutParams
        layoutParamsIr.width = ViewGroup.LayoutParams.MATCH_PARENT
        layoutParamsIr.height = ViewGroup.LayoutParams.MATCH_PARENT
        irTextureView.layoutParams = layoutParamsIr
        previewTextureView.requestLayout()
    }

    /**
     * 停止Camera2相机预览
     */
    private fun stopCamera2Preview() {
        warnOut("停止camera2相机预览")
        camera2Single?.close()
        camera2Rgb?.close()
        camera2IR?.close()
        camera2Single = null
        camera2Rgb = null
        camera2IR = null
        camera2SingleImageReader?.close()
        camera2SingleImageReader = null
    }

    /**
     * 停止旧版相机预览
     */
    @Suppress("DEPRECATION")
    private fun stopLegacyPreview() {
        warnOut("停止旧版相机预览")
        cameraLegacySingle?.stopPreview()
        cameraLegacyRgb?.stopPreview()
        cameraLegacyIR?.stopPreview()

        cameraLegacySingle = null
        cameraLegacyRgb = null
        cameraLegacyIR = null
    }

    /**
     * 打开Camera2单目
     */
    @SuppressLint("MissingPermission")
    private fun openCamera2SingleCamera() {
        val cameraId = cameraIdList[0]
        if (!isCamera2FullSupport()) {
            errorOut("设备不支持完整的Camera2功能，可能出现未知的异常，可能无法获取预览图像信息")
        }
        initCamera2SingleImageLoader(cameraId)
        cameraManager.openCamera(cameraId, camera2SingleStateCallback, camera2Handler)
    }

    /**
     * 打开Camera2双目
     */
    @SuppressLint("MissingPermission")
    private fun openCamera2MultiCamera() {
        val cameraRgbId = cameraIdList[rgbCameraId]
        val cameraIrId = cameraIdList[cameraNumbers - rgbCameraId - 1]
        if (!isCamera2FullSupport()) {
            errorOut("设备不支持完整的Camera2功能，可能出现未知的异常，可能无法获取预览图像信息")
        }
        initCamera2RgbImageLoader(cameraRgbId)
        initCamera2IrImageLoader(cameraIrId)
        cameraManager.openCamera(cameraRgbId, camera2RgbStateCallback, camera2Handler)
        cameraManager.openCamera(cameraIrId, camera2IrStateCallback, camera2Handler)
    }

    /**
     * 初始化camera2双目摄像头会话
     */
    private fun initCamera2MultiSession() {
        initCamera2RgbSession()
        initCamera2IrSession()
    }

    /**
     * 初始化camera2双目摄像头RGB相机会话
     */
    private fun initCamera2RgbSession() {
        val camera2PreviewSurface = camera2PreviewSurface ?: return
        val createCaptureRequestBuilder =
            camera2Rgb?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        createCaptureRequestBuilder?.addTarget(camera2PreviewSurface)
        camera2RgbCaptureRequest = createCaptureRequestBuilder?.build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val sessionConfiguration = SessionConfiguration(
                SessionConfiguration.SESSION_REGULAR,
                listOf(OutputConfiguration(camera2PreviewSurface)),
                camera2Executor,
                camera2RgbSessionCallback
            )
            camera2Rgb?.createCaptureSession(sessionConfiguration)
        } else {
            @Suppress("DEPRECATION")
            camera2Rgb?.createCaptureSession(
                listOf(camera2PreviewSurface),
                camera2RgbSessionCallback,
                camera2Handler
            )
        }
    }

    /**
     * 初始化camera2双目摄像头IR相机会话
     */
    private fun initCamera2IrSession() {
        val camera2IrSurface = camera2IrSurface ?: return
        val createCaptureRequestBuilder =
            camera2IR?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        createCaptureRequestBuilder?.addTarget(camera2IrSurface)
        camera2IrCaptureRequest = createCaptureRequestBuilder?.build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val sessionConfiguration = SessionConfiguration(
                SessionConfiguration.SESSION_REGULAR,
                listOf(OutputConfiguration(camera2IrSurface)),
                camera2Executor,
                camera2IrSessionCallback
            )
            camera2IR?.createCaptureSession(sessionConfiguration)
        } else {
            @Suppress("DEPRECATION")
            camera2IR?.createCaptureSession(
                listOf(camera2IrSurface),
                camera2IrSessionCallback,
                camera2Handler
            )
        }
    }

    /**
     * 创建camera2单目相机图片读取实例
     */
    private fun initCamera2SingleImageLoader(cameraId: String) {
        if (camera2SingleImageReader == null) {
            val cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId)
            val availableCapabilities =
                cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                    ?: return
            val outputFormats = availableCapabilities.outputFormats
            if (outputFormats.isEmpty()) {
                return
            }
            val outputFormatList: ArrayList<Int> =
                ArrayList(outputFormats.toMutableList())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                initImageLoaderM(outputFormatList, isMultipleCamera = false, isRgb = true)
            } else {
                initImageLoader(outputFormatList, isMultipleCamera = false, isRgb = true)
            }
        }
    }

    /**
     * 创建camera2双目-RGB相机图片读取实例
     */
    private fun initCamera2RgbImageLoader(cameraId: String) {
        if (camera2RgbImageReader == null) {
            val cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId)
            val availableCapabilities =
                cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                    ?: return
            val outputFormats = availableCapabilities.outputFormats
            if (outputFormats.isEmpty()) {
                return
            }
            val outputFormatList: ArrayList<Int> =
                ArrayList(outputFormats.toMutableList())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                initImageLoaderM(outputFormatList, isMultipleCamera = true, isRgb = true)
            } else {
                initImageLoader(outputFormatList, isMultipleCamera = true, isRgb = true)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun initImageLoaderM(
        outputFormatList: java.util.ArrayList<Int>,
        isMultipleCamera: Boolean,
        isRgb: Boolean
    ) {
        if (outputFormatList.contains(ImageFormat.FLEX_RGB_888)) {
            if (isMultipleCamera) {
                if (isRgb) {
                    camera2RgbImageReader =
                        ImageReader.newInstance(
                            previewWidth,
                            previewHeight,
                            ImageFormat.FLEX_RGB_888,
                            2
                        )
                } else {
                    camera2IrImageReader =
                        ImageReader.newInstance(
                            previewWidth,
                            previewHeight,
                            ImageFormat.FLEX_RGB_888,
                            2
                        )
                }
            } else {
                camera2SingleImageReader = ImageReader.newInstance(
                    previewWidth,
                    previewHeight,
                    ImageFormat.FLEX_RGB_888,
                    2
                )
            }
        } else if (outputFormatList.contains(ImageFormat.YUV_420_888)) {
            initImageLoader(outputFormatList, isMultipleCamera, isRgb)
        } else {
            errorOut("相机不支持使用 ImageFormat.FLEX_RGB_888 或 ImageFormat.YUV_420_888 无法进行人脸识别")
        }
    }

    private fun initImageLoader(
        outputFormatList: java.util.ArrayList<Int>,
        isMultipleCamera: Boolean,
        isRgb: Boolean
    ) {
        if (outputFormatList.contains(ImageFormat.YUV_420_888)) {
            if (isMultipleCamera) {
                if (isRgb) {
                    camera2RgbImageReader =
                        ImageReader.newInstance(
                            previewWidth,
                            previewHeight,
                            ImageFormat.YUV_420_888,
                            2
                        )
                } else {
                    camera2IrImageReader =
                        ImageReader.newInstance(
                            previewWidth,
                            previewHeight,
                            ImageFormat.YUV_420_888,
                            2
                        )
                }
            } else {
                camera2SingleImageReader =
                    ImageReader.newInstance(
                        previewWidth,
                        previewHeight,
                        ImageFormat.YUV_420_888,
                        2
                    )
            }
        } else {
            errorOut("相机不支持使用 ImageFormat.YUV_420_888 无法进行人脸识别")
        }
    }

    /**
     * 创建camera2单目相机图片读取实例
     */
    private fun initCamera2IrImageLoader(cameraId: String) {
        if (camera2IrImageReader == null) {
            val cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId)
            val availableCapabilities =
                cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                    ?: return
            val outputFormats = availableCapabilities.outputFormats
            if (outputFormats.isEmpty()) {
                return
            }
            val outputFormatList: ArrayList<Int> =
                ArrayList(outputFormats.toMutableList())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                initImageLoaderM(outputFormatList, isMultipleCamera = true, isRgb = false)
            } else {
                initImageLoader(outputFormatList, isMultipleCamera = true, isRgb = false)
            }
        }
    }

    /**
     * 获取Camera2帧数据，并触发回调
     */
    private fun getCamera2FrameData(
        imageReader: ImageReader?,
        isMultipleCamera: Boolean,
        isRgb: Boolean
    ) {
        imageReader ?: return
        val acquireLatestImage = imageReader.acquireLatestImage()
        val planes: Array<Image.Plane>
        if (!isCamera2FullSupport()) {
            if (acquireLatestImage == null) {
                if (!isMultipleCamera) {
                    errorOut("camera2 单目相机 处理数据帧为空")
                } else {
                    if (isRgb) {
                        errorOut("camera2 双目-RGB相机 处理数据帧为空")
                    } else {
                        errorOut("camera2 双目-IR相机 处理数据帧为空")
                    }
                }
                return
            }
            val planesCache = acquireLatestImage.planes
            if (planesCache == null || planesCache.isEmpty()) {
                if (!isMultipleCamera) {
                    errorOut("camera2 单目相机 planes数据为空")
                } else {
                    if (isRgb) {
                        errorOut("camera2 双目-RGB相机 planes数据为空")
                    } else {
                        errorOut("camera2 双目-IR相机 planes数据为空")
                    }
                }
                acquireLatestImage.close()
                return
            }
            planes = planesCache
        } else {
            if (acquireLatestImage == null) {
                if (!isMultipleCamera) {
                    warnOut("camera2 单目相机 处理数据帧为空")
                } else {
                    if (isRgb) {
                        warnOut("camera2 双目-RGB相机 处理数据帧为空")
                    } else {
                        warnOut("camera2 双目-IR相机 处理数据帧为空")
                    }
                }
                return
            }
            val planesCache = acquireLatestImage.planes
            if (planesCache == null || planesCache.isEmpty()) {
                if (!isMultipleCamera) {
                    warnOut("camera2 单目相机 planes数据为空")
                } else {
                    if (isRgb) {
                        warnOut("camera2 双目-RGB相机 planes数据为空")
                    } else {
                        warnOut("camera2 双目-IR相机 planes数据为空")
                    }
                }
                acquireLatestImage.close()
                return
            }
            planes = planesCache
        }
        frameListener?.frameDataCamera2(
            planes,
            isMultipleCamera = isMultipleCamera,
            isRgbData = isRgb,
            width = acquireLatestImage.width,
            height = acquireLatestImage.height
        )
        acquireLatestImage.close()
    }
}