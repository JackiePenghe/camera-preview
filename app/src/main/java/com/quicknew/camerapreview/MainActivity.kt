package com.quicknew.camerapreview

import android.graphics.Rect
import android.media.Image
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.quicknew.camerapreview.databinding.ActivityMainBinding
import com.quicknew.camerapreview.interfaces.FrameListener

class MainActivity : AppCompatActivity() {

    private val frameListener = object : FrameListener {
        /**
         * 数据帧回调-旧版
         * @param data 帧数据
         * @param isMultipleCamera 是否双目摄像头
         * @param isRgbData 是否RGB相机数据源
         */
        override fun frameDataLegacy(
            data: ByteArray,
            isMultipleCamera: Boolean,
            isRgbData: Boolean,
            width: Int,
            height: Int
        ) {
//            Log.w(TAG, "frameDataLegacy isMultipleCamera:$isMultipleCamera isRgbData:$isRgbData")
        }

        /**
         * 数据帧回调-camera2
         * @param imageData 帧数据
         * @param isMultipleCamera 是否双目摄像头
         * @param isRgbData 是否RGB相机数据源
         */
        override fun frameDataCamera2(
            imageData: Array<Image.Plane>,
            isMultipleCamera: Boolean,
            isRgbData: Boolean,
            width: Int,
            height: Int
        ) {
//            Log.w(TAG, "frameDataCamera2")
        }
    }

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.cameraPreviewView.setFrameListener(frameListener)
        Handler(Looper.getMainLooper()).postDelayed({
            binding.cameraPreviewView.isCoordinatesOutScreen(Rect())
        }, 2000)
    }


    override fun onResume() {
        super.onResume()
        binding.cameraPreviewView.startPreview()
    }

    override fun onPause() {
        super.onPause()
        binding.cameraPreviewView.stopPreview()
    }
}