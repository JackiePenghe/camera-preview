package com.quicknew.camerapreview.view

import android.content.Context
import android.util.AttributeSet
import android.view.TextureView

/**
 * 重写TextureView,处理一些数据变化
 */
class CameraTextureView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyles: Int = 0,
    defStyleRes: Int = 0
) : TextureView(context, attrs, defStyles, defStyleRes) {

}