@file:Suppress("unused", "DEPRECATION")

package com.quicknew.camerapreview.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.hardware.Camera
import android.hardware.camera2.*
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.media.ImageReader
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.AttributeSet
import android.util.Size
import android.view.*
import android.widget.RelativeLayout
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.common.util.concurrent.ListenableFuture
import com.quicknew.camerapreview.CameraManagement.cameraEnable
import com.quicknew.camerapreview.CameraManagement.cameraIdList
import com.quicknew.camerapreview.CameraManagement.cameraManager
import com.quicknew.camerapreview.CameraManagement.cameraNumbers
import com.quicknew.camerapreview.CameraManagement.isInit
import com.quicknew.camerapreview.CameraManagement.threadFactory
import com.quicknew.camerapreview.R
import com.quicknew.camerapreview.interfaces.FrameListener
import com.quicknew.camerapreview.utils.*
import com.quicknew.camerapreview.utils.errorOut
import com.quicknew.camerapreview.utils.faceCameraViewHeight
import com.quicknew.camerapreview.utils.faceCameraViewWidth
import com.quicknew.camerapreview.utils.warnOut
import java.util.concurrent.Executor
import java.util.concurrent.ScheduledThreadPoolExecutor
import kotlin.math.sqrt


class CameraPreviewView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyles: Int = 0, defStyleRes: Int = 0
) : RelativeLayout(context, attrs, defStyles, defStyleRes) {

    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *
     * ????????????
     *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    companion object {

        /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
         *
         * ??????????????????
         *
         * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

        enum class CameraVersion(val value: Int) {
            /**
             * ????????????[android.hardware.Camera]
             */
            LEGACY(1),

            /**
             * ????????????[android.hardware.camera2.CameraDevice]
             */
            CAMERA2(2),

            /**
             * ??????x???
             */
            CAMERA_X(3)
        }

        enum class CameraRotation(val value: Int) {
            /**
             * ?????????
             */
            CAMERA_ROTATION_DEFAULT(0),

            /**
             * ??????90???
             */
            CAMERA_ROTATION_90(90),

            /**
             * ??????180???
             */
            CAMERA_ROTATION_180(180),

            /**
             * ??????270???
             */
            CAMERA_ROTATION_270(270),
        }

        /**
         * ???????????????????????????-??????
         */
        private const val DEFAULT_PREVIEW_WIDTH = 640

        /**
         * ???????????????????????????-??????
         */
        private const val DEFAULT_PREVIEW_HEIGHT = 480

        /* * * * * * * * * * * * * * * * * * * ???????????? * * * * * * * * * * * * * * * * * * */

        /**
         * ???????????????????????????
         */
        const val ERR_NO_DATA_HANDLE = 1
    }

    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *
     * ????????????
     *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    /* * * * * * * * * * * * * * * * * * * view??????????????? * * * * * * * * * * * * * * * * * * */

    /**
     * ????????????ID
     */
    private var rgbCameraId = 0

    /**
     * ?????????????????????camera????????????camera2
     */
    private var cameraVersion = CameraVersion.LEGACY.value

    /**
     * ???????????????????????????
     */
    private var previewWidth: Int = DEFAULT_PREVIEW_WIDTH

    /**
     * ???????????????????????????
     */
    private var previewHeight: Int = DEFAULT_PREVIEW_HEIGHT

    /**
     * ?????????????????? ???0???90???180???270???
     */
    private var cameraRotation = CameraRotation.CAMERA_ROTATION_DEFAULT.value

    /**
     * ?????????????????????
     */
    private var needExchangeWidthAndHeight: Boolean = false

    /**
     * ??????????????????
     */
    private var mirrored: Boolean = false

    /**
     * ???????????????????????????
     */
    private var useMultipleCamera: Boolean = true

    /**
     * camera2 RGB??????????????????
     */
    private var camera2RgbRotation: Int = cameraRotation

    /**
     * camera2 IR??????????????????
     */
    private var camera2IrRotation: Int = cameraRotation

    /* * * * * * * * * * * * * * * * * * * ???????????? * * * * * * * * * * * * * * * * * * */

    /**
     * ??????????????????
     */
    private var cameraLegacySingle: Camera? = null

    /**
     * ??????RGB????????????
     */
    private var cameraLegacyRgb: Camera? = null

    /**
     * ????????????????????????
     */
    private var cameraLegacyIR: Camera? = null

    /**
     * camera2????????????Handler
     */
    private var camera2Handler: Handler? = null

    /**
     * camera2??????????????????
     */
    private var camera2Single: CameraDevice? = null

    /**
     * camera2??????????????????????????????
     */
    private var camera2SingleImageReader: ImageReader? = null

    /**
     * camera2??????-RGB????????????????????????
     */
    private var camera2RgbImageReader: ImageReader? = null

    /**
     * camera2??????-IR????????????????????????
     */
    private var camera2IrImageReader: ImageReader? = null

    /**
     * camera2?????????Surface
     */
    private var camera2PreviewSurface: Surface? = null

    /**
     * camera2?????????SurfaceTexture
     */
    private var camera2PreviewSurfaceTexture: SurfaceTexture? = null

    /**
     * camera2Ir???Surface
     */
    private var camera2IrSurface: Surface? = null

    /**
     * camera2???????????????????????????
     */
    private var camera2SingleCaptureRequest: CaptureRequest? = null

    /**
     * camera2 ????????????-RGB????????????
     */
    private var camera2Rgb: CameraDevice? = null

    /**
     * camera2 ????????????-IR????????????
     */
    private var camera2IR: CameraDevice? = null

    /**
     * camera2 ????????????-RGB??????????????????
     */
    private var camera2RgbCaptureRequest: CaptureRequest? = null

    /**
     * camera2 ????????????-IR??????????????????
     */
    private var camera2IrCaptureRequest: CaptureRequest? = null

    /**
     * ?????????????????????
     */
    private var frameListener: FrameListener? = null

    /**
     * cameraX??????Provider
     */
    private var cameraXProviderFuture: ListenableFuture<ProcessCameraProvider>? = null

    /**
     * cameraX??????????????????
     */
    private var cameraXSingleCamera: androidx.camera.core.Camera? = null

    /* * * * * * * * * * * * * * * * * * * ????????????????????? * * * * * * * * * * * * * * * * * * */

    /**
     * ?????????
     */
    private lateinit var faceRelativeLayout: RelativeLayout

    /**
     * ?????????????????????View
     */
    private lateinit var previewTextureView: CameraTextureView

    /**
     * ir?????????View
     */
    private lateinit var irTextureView: CameraTextureView

    /**
     * camerax??????View
     */
    private lateinit var cameraXRgbPreviewView: PreviewView

    /* * * * * * * * * * * * * * * * * * * ???????????? * * * * * * * * * * * * * * * * * * */

    /**
     * camera2???????????????
     */
    private val camera2Executor: Executor = ScheduledThreadPoolExecutor(1, threadFactory)

    /**
     * preview surface????????????
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
     * ir surface????????????
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
     * ?????????????????????????????????????????????
     */
    private val cameraLegacySinglePreviewCallback = Camera.PreviewCallback { data, camera ->
        cameraLegacySingle = camera
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
     * ?????????????????????RGB????????????????????????
     */
    private val cameraLegacyRgbPreviewCallback = Camera.PreviewCallback { data, camera ->
        frameCallbackHandler.post {
            cameraLegacyRgb = camera
            frameListener?.frameDataLegacy(
                data,
                isMultipleCamera = true,
                isRgbData = true,
                previewWidth,
                previewHeight
            )
        }
    }

    /**
     * ?????????????????????IR????????????????????????
     */
    private val cameraLegacyIRPreviewCallback = Camera.PreviewCallback { data, _ ->
        frameCallbackHandler.post {
            frameListener?.frameDataLegacy(
                data,
                isMultipleCamera = true,
                isRgbData = false,
                previewWidth,
                previewHeight
            )
        }
    }

    /**
     * camera2??????????????????
     */
    private val camera2HandlerThread = HandlerThread("camera2HandlerThread")

    /**
     * camera2??????????????????
     */
    private val camera2SingleStateCallback = object : CameraDevice.StateCallback() {

        override fun onOpened(camera: CameraDevice) {
            warnOut("camera2?????????????????????")
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
                errorOut("????????????????????????Camera2?????????????????????????????????????????????")
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val previewOutputConfiguration = OutputConfiguration(camera2PreviewSurface)
                //????????????????????????????????????????????????????????????????????????
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
            warnOut("camera2?????????????????????")
            camera2Single = null
            camera2SingleImageReader = null
        }

        override fun onError(camera: CameraDevice, error: Int) {
            val errMsg: String =
                when (error) {
                    ERROR_CAMERA_IN_USE -> {
                        "???????????????"
                    }
                    ERROR_MAX_CAMERAS_IN_USE -> {
                        "???????????????????????????"
                    }
                    ERROR_CAMERA_DISABLED -> {
                        "???????????????????????????"
                    }
                    ERROR_CAMERA_DEVICE -> {
                        "?????????????????????"
                    }
                    ERROR_CAMERA_SERVICE -> {
                        "????????????????????????"
                    }
                    else -> {
                        "????????????"
                    }
                }
            warnOut("camera2?????????????????? $errMsg")
        }
    }

    /**
     * camera2??????????????????????????????
     */
    private val camera2SingleSessionCallback = object : CameraCaptureSession.StateCallback() {

        override fun onConfigured(session: CameraCaptureSession) {
            warnOut("camera2??????????????????????????????")
            session.setRepeatingRequest(
                camera2SingleCaptureRequest ?: return, camera2SingleCaptureCallback, camera2Handler
            )
        }

        override fun onConfigureFailed(session: CameraCaptureSession) {
            warnOut("camera2??????????????????????????????")
        }
    }

    /**
     * camera2??????????????????????????????
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
     * camera2????????????-RGB??????????????????
     */
    private val camera2RgbStateCallback = object : CameraDevice.StateCallback() {

        override fun onOpened(camera: CameraDevice) {
            warnOut("camera2????????????-RGB???????????????")
            camera2Rgb = camera
        }

        override fun onDisconnected(camera: CameraDevice) {
            warnOut("camera2????????????-RGB???????????????")
            camera2Rgb = null
        }

        override fun onError(camera: CameraDevice, error: Int) {
            val errMsg = when (error) {
                ERROR_CAMERA_IN_USE -> {
                    "???????????????"
                }
                ERROR_MAX_CAMERAS_IN_USE -> {
                    "???????????????????????????"
                }
                ERROR_CAMERA_DISABLED -> {
                    "???????????????????????????"
                }
                ERROR_CAMERA_DEVICE -> {
                    "?????????????????????"
                }
                ERROR_CAMERA_SERVICE -> {
                    "????????????????????????"
                }
                else -> {
                    "????????????"
                }
            }
            warnOut("camera2????????????-RGB???????????? $errMsg")
        }

    }

    /**
     * camera2????????????-IR??????????????????
     */
    private val camera2IrStateCallback = object : CameraDevice.StateCallback() {

        override fun onOpened(camera: CameraDevice) {
            warnOut("camera2????????????-IR???????????????")
            camera2IR = camera
            initCamera2MultiSession()
        }

        override fun onDisconnected(camera: CameraDevice) {
            warnOut("camera2????????????-IR???????????????")
            camera2IR = null
        }

        override fun onError(camera: CameraDevice, error: Int) {
            val errMsg: String
            when (error) {
                ERROR_CAMERA_IN_USE -> {
                    errMsg = "???????????????"
                }
                ERROR_MAX_CAMERAS_IN_USE -> {
                    errMsg = "???????????????????????????"
                }
                ERROR_CAMERA_DISABLED -> {
                    errMsg = "???????????????????????????"
                }
                ERROR_CAMERA_DEVICE -> {
                    errMsg = "?????????????????????"
                }
                ERROR_CAMERA_SERVICE -> {
                    errMsg = "????????????????????????"
                }
                else -> {
                    errMsg = "????????????"
                }
            }
            warnOut("camera2????????????-IR???????????? $errMsg")

            when (error) {
                //???????????????
                ERROR_CAMERA_IN_USE -> {
                }
                //???????????????????????????
                ERROR_MAX_CAMERAS_IN_USE -> {
                    warnOut("????????????????????????camera2,????????????????????????")
                    stopCamera2Preview()
                    openCamera2SingleCamera()
                }
                //???????????????????????????
                ERROR_CAMERA_DISABLED -> {
                }
                //?????????????????????
                ERROR_CAMERA_DEVICE -> {
                }
                //????????????????????????
                ERROR_CAMERA_SERVICE -> {
                }
                //????????????
                else -> {
                }
            }
        }
    }

    /**
     * camera2????????????-RGB????????????????????????
     */
    private val camera2RgbSessionCallback = object : CameraCaptureSession.StateCallback() {

        override fun onConfigured(session: CameraCaptureSession) {
            warnOut("camera2????????????-RGB????????????????????????")
            session.setRepeatingRequest(
                camera2RgbCaptureRequest ?: return,
                camera2RgbCaptureCallback,
                camera2Handler
            )
        }

        override fun onConfigureFailed(session: CameraCaptureSession) {
            warnOut("camera2????????????-RGB????????????????????????")
        }
    }

    /**
     * camera2????????????-RGB????????????????????????
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
     * camera2????????????-IR????????????????????????
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
            getCamera2FrameData(camera2IrImageReader, isMultipleCamera = true, isRgb = false)
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
            getCamera2FrameData(camera2IrImageReader, isMultipleCamera = true, isRgb = false)
        }
    }

    /**
     * camera2????????????-IR????????????????????????
     */
    private val camera2IrSessionCallback = object : CameraCaptureSession.StateCallback() {

        override fun onConfigured(session: CameraCaptureSession) {
            warnOut("camera2????????????-IR????????????????????????")
            session.setRepeatingRequest(
                camera2IrCaptureRequest ?: return,
                camera2IrCaptureCallback,
                camera2Handler
            )
        }

        override fun onConfigureFailed(session: CameraCaptureSession) {
            warnOut("camera2????????????-IR????????????????????????")
        }
    }

    private val frameCallbackHandlerThread =
        HandlerThread("frameCallbackHandlerThread").apply { start() }

    private val frameCallbackHandler: Handler = Handler(frameCallbackHandlerThread.looper)

    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *
     * ????????????
     *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    init {
        if (isInit) {
            if (cameraEnable) {
                faceRelativeLayout = RelativeLayout(context)
                addView(faceRelativeLayout)
                obtainStyledAttributes(context, attrs)
            } else {
                warnOut("??????????????????")
            }
        } else {
            warnOut("????????????????????????")
        }
    }

    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *
     * ????????????
     *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    /**
     * ??????CameraX??????
     */
    private fun stopCameraXPreview() {
        cameraXProviderFuture?.get()?.unbindAll()
    }

    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *
     * ????????????
     *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    /**
     * ?????????????????????
     */
    fun setFrameListener(frameListener: FrameListener?) {
        this.frameListener = frameListener
    }

    /**
     * ??????????????????
     */
    fun startPreview() {
        if (!isInit) {
            warnOut("????????????????????????,????????????")
            return
        }
        if (!cameraEnable) {
            warnOut("?????????????????????????????????")
            return
        }
        //??????????????????
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
     * ????????????
     */
    fun stopPreview() {
        if (!isInit) {
            warnOut("????????????????????????,????????????")
            return
        }
        if (!cameraEnable) {
            warnOut("?????????????????????????????????")
            return
        }
        when (cameraVersion) {
            CameraVersion.LEGACY.value -> {
                stopLegacyPreview()
            }
            CameraVersion.CAMERA2.value -> {
                stopCamera2Preview()
            }
            CameraVersion.CAMERA_X.value -> {
                stopCameraXPreview()
            }
        }
    }

    /**
     * ??????????????????Rect??????
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun calculateFaceRect(rect: Rect): Rect {
        val cameraWidth: Int
        val cameraHeight: Int
        if (!needExchangeWidthAndHeight) {
            cameraWidth = previewWidth
            cameraHeight = previewHeight
        } else {
            cameraWidth = previewHeight
            cameraHeight = previewWidth
        }
        val leftCoordinate = (rect.left * proportion(faceCameraViewWidth, cameraWidth)).toInt()
        val topCoordinate = (rect.top * proportion(
            faceCameraViewHeight,
            cameraHeight
        )).toInt()
        val rightCoordinate = (rect.right * proportion(
            faceCameraViewWidth,
            cameraWidth
        )).toInt()
        val bottomCoordinate = (rect.bottom * proportion(
            faceCameraViewHeight,
            cameraHeight
        )).toInt()
        return Rect(leftCoordinate, topCoordinate, rightCoordinate, bottomCoordinate)
    }

    /**
     * ????????????????????????????????????
     */
    @Suppress("unused", "MemberVisibilityCanBePrivate")
    fun isCoordinatesOutScreen(rect: Rect): Boolean {
        val calculateFaceRect = calculateFaceRect(rect)

        //?????????????????????
        return if (enableCirclePreview) {
            (isOutCircle(
                calculateFaceRect.left,
                calculateFaceRect.top,
                roundX,
                roundY,
                radius
            )
                    || isOutCircle(
                calculateFaceRect.left,
                calculateFaceRect.bottom,
                roundX,
                roundY,
                radius
            )
                    || isOutCircle(
                calculateFaceRect.right,
                calculateFaceRect.top,
                roundX,
                roundY,
                radius
            )
                    || isOutCircle(
                calculateFaceRect.right,
                calculateFaceRect.bottom,
                roundX,
                roundY,
                radius
            ))
        } else {
            calculateFaceRect.left < faceCameraViewLeftOffset || calculateFaceRect.right > faceCameraViewRightOffset || calculateFaceRect.top < faceCameraViewTopOffset || calculateFaceRect.bottom > faceCameraViewBottomOffset
        }
    }

    /* * * * * * * * * * * * * * * * * * * ??????????????????????????? * * * * * * * * * * * * * * * * * * */

    /**
     * ??????????????????
     */
    @Suppress("unused")
    fun setCameraVersion(cameraVersion: CameraVersion) {
        this.cameraVersion = cameraVersion.value
    }

    /**
     * ??????RGB??????ID
     */
    @Suppress("unused")
    fun setRgbCameraId(rgbCameraId: Int) {
        this.rgbCameraId = rgbCameraId
    }

    /**
     * ???????????????????????????
     */
    @Suppress("unused")
    fun setPreviewSize(width: Int, height: Int) {
        previewWidth = width
        previewHeight = height
    }

    /**
     * ????????????????????????
     */
    @Suppress("unused")
    fun setCameraRotation(cameraRotation: CameraRotation) {
        this.cameraRotation = cameraRotation.value
    }

    /**
     * ??????????????????????????????
     */
    @Suppress("unused")
    fun setEnableRoundPreview(enableCirclePreview: Boolean) {
        com.quicknew.camerapreview.utils.enableCirclePreview = enableCirclePreview
    }

    /**
     * ????????????????????????
     */
    @Suppress("unused")
    fun setNeedExchangeWidthAndHeight(needExchangeWidthAndHeight: Boolean) {
        this.needExchangeWidthAndHeight = needExchangeWidthAndHeight
    }

    /**
     * ????????????????????????
     */
    @Suppress("unused")
    fun setMirrored(mirrored: Boolean) {
        this.mirrored = mirrored
    }

    /**
     * ??????????????????
     */
    @Suppress("unused")
    fun isMirrored(): Boolean {
        return mirrored
    }

    /**
     * ??????????????????????????????
     */
    @Suppress("unused")
    fun setUseMultipleCamera(useMultipleCamera: Boolean) {
        this.useMultipleCamera = useMultipleCamera
    }

    /**
     * ??????????????????????????????
     */
    @Suppress("unused")
    fun isUseMultipleCamera(): Boolean {
        return useMultipleCamera
    }

    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *
     * ????????????
     *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    /**
     * ?????????????????????
     */
    private fun obtainStyledAttributes(
        context: Context, attrs: AttributeSet?
    ) {
        val obtainStyledAttributes =
            context.obtainStyledAttributes(attrs, R.styleable.CameraPreviewView)
        cameraVersion = obtainStyledAttributes.getInt(
            R.styleable.CameraPreviewView_cameraVersion, CameraVersion.LEGACY.value
        )
        warnOut("?????????????????? ${getCameraVersion(cameraVersion)}")
        rgbCameraId = obtainStyledAttributes.getInt(R.styleable.CameraPreviewView_rgbCameraId, 0)
        warnOut("????????????ID $rgbCameraId")
        previewWidth = obtainStyledAttributes.getInt(
            R.styleable.CameraPreviewView_previewWidth, DEFAULT_PREVIEW_WIDTH
        )
        previewHeight = obtainStyledAttributes.getInt(
            R.styleable.CameraPreviewView_previewHeight, DEFAULT_PREVIEW_HEIGHT
        )
        warnOut("????????????????????? ${previewWidth}x${previewHeight}")
        cameraRotation =
            obtainStyledAttributes.getInt(R.styleable.CameraPreviewView_cameraRotation, 0)
        warnOut("?????????????????? $cameraRotation")
        enableCirclePreview = obtainStyledAttributes.getBoolean(
            R.styleable.CameraPreviewView_enableCirclePreview, false
        )
        warnOut("?????????????????????????????? $enableCirclePreview")
        needExchangeWidthAndHeight = obtainStyledAttributes.getBoolean(
            R.styleable.CameraPreviewView_needExchangeWidthAndHeight, false
        )
        warnOut("????????????????????? $needExchangeWidthAndHeight")
        mirrored =
            obtainStyledAttributes.getBoolean(R.styleable.CameraPreviewView_cameraMirror, false)
        warnOut("???????????????????????? $mirrored")
        useMultipleCamera =
            obtainStyledAttributes.getBoolean(R.styleable.CameraPreviewView_useMultipleCamera, true)

        camera2RgbRotation = obtainStyledAttributes.getInt(
            R.styleable.CameraPreviewView_camera2RgbRotation,
            cameraRotation
        )
        camera2IrRotation = obtainStyledAttributes.getInt(
            R.styleable.CameraPreviewView_camera2IrRotation,
            cameraRotation
        )
        obtainStyledAttributes.recycle()
    }

    /**
     * ???????????????????????????????????????
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
     * ?????????????????????Camera2?????????Camera2????????????????????????
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
     * ??????Camera2????????????
     */
    private fun openCamera2() {
        if (camera2Handler == null) {
            camera2HandlerThread.start()
            camera2Handler = Handler(camera2HandlerThread.looper)
        }
        if (useMultipleCamera) {
            if (cameraNumbers == 1) {
                openCamera2SingleCamera()
            } else {
                warnOut("?????????????????????")
                openCamera2MultiCamera()
            }
        } else {
            openCamera2SingleCamera()
        }
    }

    /**
     * ??????????????????????????????
     */
    private fun openLegacy() {
        if (useMultipleCamera) {
            if (cameraNumbers == 1) {
                openLegacySingleCamera()
            } else {
                warnOut("?????????????????????")
                openLegacyMultiCamera()
            }
        } else {
            openLegacySingleCamera()
        }
    }

    /**
     * ?????????????????????(??????)
     */
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
            //??????????????????
            stopPreview()
            //???????????????????????????????????????????????????
            openLegacySingleCamera()
        }
    }

    /**
     * ?????????????????????(??????)
     */
    private fun openLegacySingleCamera() {
        cameraLegacySingle = if (rgbCameraId >= cameraNumbers - 1) {
            Camera.open()
        } else {
            Camera.open(rgbCameraId)
        }
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
     * ????????????
     */
    private fun openCamera() {
        when (cameraVersion) {
            CameraVersion.LEGACY.value -> {
                openLegacy()
            }
            CameraVersion.CAMERA2.value -> {
                if (!isCamera2FullSupport()) {
                    errorOut("????????????????????????Camera2???????????????????????????????????????????????????????????????????????????")
                }
                openCamera2()
            }
            CameraVersion.CAMERA_X.value -> {
                openCameraX()
            }
        }
    }

    /**
     * ????????????X
     */
    private fun openCameraX() {
        if (cameraNumbers >= 2 && useMultipleCamera) {
            openCameraXMulti()
        } else {
            openCameraXSingle()
        }
    }

    /**
     * ??????cameraX??????????????????
     */
    private fun openCameraXMulti() {
        warnOut("cameraX ???????????????????????????????????????????????????????????????")
        openCameraXSingle()
    }

    /**
     * ??????cameraX??????????????????
     */
    private fun openCameraXSingle() {
        if (cameraXProviderFuture == null) {
            cameraXProviderFuture = ProcessCameraProvider.getInstance(context)
        }
        cameraXProviderFuture?.addListener({
            cameraXRgbPreviewView.rotation = cameraRotation.toFloat()
            val cameraProvider = cameraXProviderFuture?.get() ?: return@addListener
            tryCount = 0
            bindCameraXSinglePreview(cameraProvider)
        }, ContextCompat.getMainExecutor(context))
    }

    private var tryCount = 0

    private fun bindCameraXSinglePreview(cameraProvider: ProcessCameraProvider) {

        val context = context ?: return
        val targetCameraId = if (cameraNumbers >= 0) {
            rgbCameraId
        } else {
            0
        }
        cameraProvider.unbindAll()
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(targetCameraId)
            .build()
        val preview = Preview.Builder()
            .build()
        if (context is LifecycleOwner) {
            val cameraXSingleImageAnalysis = ImageAnalysis.Builder()
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                .setTargetResolution(Size(previewWidth, previewHeight))
                .build()
            cameraXSingleImageAnalysis.targetRotation = getCameraXTargetRotation()

            cameraXSingleCamera = cameraProvider.bindToLifecycle(
                context,
                cameraSelector,
                preview,
                cameraXSingleImageAnalysis
            )
            preview.setSurfaceProvider(cameraXRgbPreviewView.surfaceProvider)
            cameraXSingleImageAnalysis.clearAnalyzer()
            cameraXSingleImageAnalysis.setAnalyzer(
                ContextCompat.getMainExecutor(context)
            ) {

                @androidx.camera.core.ExperimentalGetImage
                val image = it.image
                if (image == null) {
                    it.close()
                    return@setAnalyzer
                }
                val data =
                    ImageConverter.getByteDataFromImage(image, ImageConverter.COLOR_FormatNV21)
                val width = it.width
                val height = it.height
                it.close()
                frameListener?.frameDataCameraX(
                    data,
                    isMultipleCamera = false,
                    isRgbData = true,
                    width,
                    height
                )
            }
        } else {
            throw RuntimeException("context not a LifecycleOwner instance")
        }
    }

    private fun getCameraXTargetRotation(): Int {
        return when (cameraRotation) {
            90 -> {
                Surface.ROTATION_90
            }
            180 -> {
                Surface.ROTATION_180
            }
            270 -> {
                Surface.ROTATION_270
            }
            0 -> {
                Surface.ROTATION_0
            }
            else -> {
                Surface.ROTATION_0
            }
        }
    }

    /**
     * ??????????????????
     */
    private fun calculateProportion() {
        when (cameraVersion) {
            CameraVersion.CAMERA2.value -> {
                calculateProportionCamera2()
            }
            CameraVersion.LEGACY.value -> {
                calculateProportionLegacy()
            }
            CameraVersion.CAMERA_X.value -> {
                calculateProportionCameraX()
            }
        }

    }

    private fun calculateProportionCameraX() {
        val surfaceViewWidth: Int
        val surfaceViewHeight: Int
        if (enableCirclePreview) {
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

        //?????????????????????????????????????????????
        if (!needExchangeWidthAndHeight) {
            cameraWidth = previewWidth
            cameraHeight = previewHeight
        } else {
            cameraWidth = previewHeight
            cameraHeight = previewWidth
        }
        //???????????????????????? ????????????
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
            faceCameraViewBottomOffset = surfaceViewHeight
        } else  //????????????????????????????????? ????????????????????? ??????????????????
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
                faceCameraViewBottomOffset = -layoutParams.topMargin + surfaceViewHeight
            } //????????????????????????????????? ????????????????????? ??????????????????
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
                faceCameraViewBottomOffset = surfaceViewHeight

            }

        faceRelativeLayout.layoutParams = layoutParams
        faceRelativeLayout.requestLayout()
    }

    private fun calculateProportionCamera2() {
        val surfaceViewWidth: Int
        val surfaceViewHeight: Int
        if (enableCirclePreview) {
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
        if (needExchangeWidthAndHeight) {
            cameraWidth = previewHeight
            cameraHeight = previewWidth
        } else {
            cameraWidth = previewWidth
            cameraHeight = previewHeight
        }
        val layoutParams = faceRelativeLayout.layoutParams as LayoutParams

        //???????????????????????? ????????????
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
            faceCameraViewBottomOffset = surfaceViewHeight
        } else  //????????????????????????????????? ????????????????????? ??????????????????
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
                layoutParams.bottomMargin = surfaceViewHeight - layoutParams.height
                faceCameraViewWidth = layoutParams.width
                faceCameraViewHeight = layoutParams.height
                faceCameraViewLeftOffset = 0
                faceCameraViewRightOffset = surfaceViewWidth
                faceCameraViewTopOffset = -layoutParams.topMargin
                faceCameraViewBottomOffset = -layoutParams.topMargin + surfaceViewHeight
            } //????????????????????????????????? ????????????????????? ??????????????????
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
                faceCameraViewBottomOffset = surfaceViewHeight
            }
        faceRelativeLayout.layoutParams = layoutParams
        faceRelativeLayout.requestLayout()
    }

    private fun calculateProportionLegacy() {
        val surfaceViewWidth: Int
        val surfaceViewHeight: Int
        if (enableCirclePreview) {
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

        //?????????????????????????????????????????????
        if (!needExchangeWidthAndHeight) {
            cameraWidth = previewWidth
            cameraHeight = previewHeight
        } else {
            cameraWidth = previewHeight
            cameraHeight = previewWidth
        }
        //???????????????????????? ????????????
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
            faceCameraViewBottomOffset = surfaceViewHeight
        } else  //????????????????????????????????? ????????????????????? ??????????????????
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
                faceCameraViewBottomOffset = -layoutParams.topMargin + surfaceViewHeight
            } //????????????????????????????????? ????????????????????? ??????????????????
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
                faceCameraViewBottomOffset = surfaceViewHeight

            }

        faceRelativeLayout.layoutParams = layoutParams
        faceRelativeLayout.requestLayout()
    }

    /**
     * ????????????
     */
    private fun proportion(value1: Int, value2: Int): Double {
        return value1.toDouble() / value2.toDouble()
    }

    /**
     * ?????????????????????View
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

        if (cameraVersion == CameraVersion.CAMERA_X.value) {
            cameraXRgbPreviewView = PreviewView(context)
            faceRelativeLayout.addView(cameraXRgbPreviewView)
            val rgbLayoutParams = cameraXRgbPreviewView.layoutParams
            rgbLayoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
            rgbLayoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
            cameraXRgbPreviewView.layoutParams = rgbLayoutParams
        }

        if (mirrored) {
            previewTextureView.scaleX = -1f
        } else {
            previewTextureView.scaleX = 1f
        }

    }

    /**
     * ??????Camera2????????????
     */
    private fun stopCamera2Preview() {
        warnOut("??????camera2????????????")
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
     * ????????????????????????
     */
    private fun stopLegacyPreview() {
        warnOut("????????????????????????")
        cameraLegacySingle?.stopPreview()
        cameraLegacyRgb?.stopPreview()
        cameraLegacyIR?.stopPreview()

        cameraLegacySingle?.setPreviewCallback(null)
        cameraLegacyRgb?.setPreviewCallback(null)
        cameraLegacyIR?.setPreviewCallback(null)

        cameraLegacySingle?.release()
        cameraLegacyRgb?.release()
        cameraLegacyIR?.release()

        cameraLegacySingle = null
        cameraLegacyRgb = null
        cameraLegacyIR = null
    }

    /**
     * ??????Camera2??????
     */
    @SuppressLint("MissingPermission")
    private fun openCamera2SingleCamera() {
        val cameraId = cameraIdList[0]
        initCamera2Param()
        if (!isCamera2FullSupport()) {
            errorOut("????????????????????????Camera2???????????????????????????????????????????????????????????????????????????")
        }
        initCamera2SingleImageLoader(cameraId)
        cameraManager.openCamera(cameraId, camera2SingleStateCallback, camera2Handler)
    }

    /**
     * ??????Camera2??????
     */
    @SuppressLint("MissingPermission")
    private fun openCamera2MultiCamera() {
        initCamera2Param()
        val cameraRgbId = cameraIdList[rgbCameraId]
        val cameraIrId = cameraIdList[cameraNumbers - rgbCameraId - 1]
        if (!isCamera2FullSupport()) {
            errorOut("????????????????????????Camera2???????????????????????????????????????????????????????????????????????????")
        }
        initCamera2RgbImageLoader(cameraRgbId)
        initCamera2IrImageLoader(cameraIrId)
        cameraManager.openCamera(cameraRgbId, camera2RgbStateCallback, camera2Handler)
        cameraManager.openCamera(cameraIrId, camera2IrStateCallback, camera2Handler)
    }

    /**
     * ?????????camera2?????????????????????
     */
    private fun initCamera2MultiSession() {
        initCamera2RgbSession()
        initCamera2IrSession()
    }

    /**
     * ?????????camera2???????????????RGB????????????
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
            camera2Rgb?.createCaptureSession(
                listOf(camera2PreviewSurface),
                camera2RgbSessionCallback,
                camera2Handler
            )
        }
    }

    /**
     * ?????????camera2???????????????IR????????????
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
            camera2IR?.createCaptureSession(
                listOf(camera2IrSurface),
                camera2IrSessionCallback,
                camera2Handler
            )
        }
    }

    /**
     * ??????camera2??????????????????????????????
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
            initImageLoader(outputFormatList, isMultipleCamera = false, isRgb = true)
        }
    }

    /**
     * ??????camera2??????-RGB????????????????????????
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
            initImageLoader(outputFormatList, isMultipleCamera = true, isRgb = true)

        }
    }

    private fun initImageLoader(
        outputFormatList: java.util.ArrayList<Int>,
        isMultipleCamera: Boolean,
        isRgb: Boolean
    ) {

        for (outputFormat in outputFormatList) {
            warnOut(
                "isMultipleCamera $isMultipleCamera isRgb $isRgb supported output format ${
                    outputFormat.toString(
                        16
                    )
                }"
            )
        }

        if (outputFormatList.contains(ImageFormat.NV21)) {
            if (isMultipleCamera) {
                if (isRgb) {
                    camera2RgbImageReader =
                        ImageReader.newInstance(
                            previewWidth,
                            previewHeight,
                            ImageFormat.NV21,
                            2
                        )
                } else {
                    camera2IrImageReader =
                        ImageReader.newInstance(
                            previewWidth,
                            previewHeight,
                            ImageFormat.NV21,
                            2
                        )
                }
            } else {
                camera2SingleImageReader =
                    ImageReader.newInstance(
                        previewWidth,
                        previewHeight,
                        ImageFormat.NV21,
                        2
                    )
            }
        } else if (outputFormatList.contains(ImageFormat.YV12)) {
            if (isMultipleCamera) {
                if (isRgb) {
                    camera2RgbImageReader =
                        ImageReader.newInstance(
                            previewWidth,
                            previewHeight,
                            ImageFormat.YV12,
                            2
                        )
                } else {
                    camera2IrImageReader =
                        ImageReader.newInstance(
                            previewWidth,
                            previewHeight,
                            ImageFormat.YV12,
                            2
                        )
                }
            } else {
                camera2SingleImageReader =
                    ImageReader.newInstance(
                        previewWidth,
                        previewHeight,
                        ImageFormat.YV12,
                        2
                    )
            }
        } else if (outputFormatList.contains(ImageFormat.YUV_420_888)) {
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
            errorOut("????????????????????? ImageFormat.YUV_420_888 ????????????????????????")
        }
    }

    /**
     * ??????camera2??????????????????????????????
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
            initImageLoader(outputFormatList, isMultipleCamera = true, isRgb = false)
        }
    }

    /**
     * ??????Camera2???????????????????????????
     */
    private fun getCamera2FrameData(
        imageReader: ImageReader?,
        isMultipleCamera: Boolean,
        isRgb: Boolean
    ) {
        if (imageReader == null) {
            frameListener?.error(ERR_NO_DATA_HANDLE, "no camera2 data can be handle")
            return
        }
        val acquireLatestImage = imageReader.acquireLatestImage()
        if (acquireLatestImage == null) {
            frameListener?.error(ERR_NO_DATA_HANDLE, "no camera2 data can be handle")
            return
        }
        val data =
            ImageConverter.getByteDataFromImage(acquireLatestImage, ImageConverter.COLOR_FormatNV21)
        val width = acquireLatestImage.width
        val height = acquireLatestImage.height
        acquireLatestImage.close()
        frameListener?.frameDataCamera2(
            data,
            isMultipleCamera = isMultipleCamera,
            isRgbData = isRgb,
            width = width,
            height = height
        )
    }

    /**
     * ??????????????????????????????????????????
     *
     * @param x1
     * @param y1
     * @param x2
     * @param y2
     * @param radius
     * @return
     */
    private fun isOutCircle(x1: Int, y1: Int, x2: Int, y2: Int, radius: Int): Boolean {
        return sqrt(((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2)).toDouble()) > radius
    }

    /**
     * ?????????camera2????????????
     */
    private fun initCamera2Param() {

        previewTextureView.rotation = camera2RgbRotation.toFloat()

        irTextureView.rotation = camera2IrRotation.toFloat()

        if (isMirrored()) {
            previewTextureView.scaleX = -1f
        } else {
            previewTextureView.scaleX = 1f
        }


        if (needExchangeWidthAndHeight) {
            previewTextureView.surfaceTexture?.setDefaultBufferSize(previewWidth, previewHeight)
        } else {
            previewTextureView.surfaceTexture?.setDefaultBufferSize(previewHeight, previewWidth)
        }
    }
}