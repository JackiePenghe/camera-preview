package com.quicknew.camerapreview.interfaces

interface FrameListener {

    /**
     * 数据帧回调-旧版
     * @param data 帧数据
     * @param isMultipleCamera 是否双目摄像头
     * @param isRgbData 是否RGB相机数据源
     */
    fun frameDataLegacy(
        data: ByteArray,
        isMultipleCamera: Boolean,
        isRgbData: Boolean,
        width: Int,
        height: Int
    )

    /**
     * 数据帧回调-camera2
     * @param data 帧数据
     * @param isMultipleCamera 是否双目摄像头
     * @param isRgbData 是否RGB相机数据源
     */
    fun frameDataCamera2(
        data: ByteArray,
        isMultipleCamera: Boolean,
        isRgbData: Boolean,
        width: Int,
        height: Int
    )

    /**
     * 数据帧回调
     * @param data NV21数据
     * @param isMultipleCamera 是否双目摄像头
     * @param isRgbData 是否RGB相机数据源
     */
    fun frameDataCameraX(
        data: ByteArray,
        isMultipleCamera: Boolean,
        isRgbData: Boolean,
        width: Int,
        height: Int
    )

    /**
     * 出现错误
     */
    fun error(errCode: Int, errMsg: String)
}