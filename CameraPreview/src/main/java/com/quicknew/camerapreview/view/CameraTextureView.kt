package com.quicknew.camerapreview.view

import android.content.Context
import android.graphics.Outline
import android.util.AttributeSet
import android.view.TextureView
import android.view.View
import android.view.ViewOutlineProvider
import com.quicknew.camerapreview.utils.*

/**
 * 重写TextureView,处理一些数据变化
 */
class CameraTextureView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyles: Int = 0,
    defStyleRes: Int = 0
) : TextureView(context, attrs, defStyles, defStyleRes) {
    init {
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                if (enableCirclePreview) {
                    roundX = width / 2
                    roundY = height / 2
                    //设置裁剪的圆心，半径
                    radius =
                        if (faceCameraViewRightOffset - faceCameraViewLeftOffset > faceCameraViewDownOffset - faceCameraViewTopOffset) {
                            (faceCameraViewDownOffset - faceCameraViewTopOffset) / 2
                        } else {
                            (faceCameraViewRightOffset - faceCameraViewLeftOffset) / 2
                        }
                    outline.setOval(
                        roundX - radius,
                        roundY - radius,
                        roundX + radius,
                        roundY + radius
                    )
                    clipToOutline = true
                }
            }
        }
    }
}