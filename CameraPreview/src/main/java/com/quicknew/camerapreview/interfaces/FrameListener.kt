package com.quicknew.camerapreview.interfaces

import android.media.Image

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
     * @param imageData 帧数据
     * @param isMultipleCamera 是否双目摄像头
     * @param isRgbData 是否RGB相机数据源
     */
    fun frameDataCamera2(
        imageData: Array<Image.Plane>,
        isMultipleCamera: Boolean,
        isRgbData: Boolean,
        width: Int,
        height: Int
    )
}