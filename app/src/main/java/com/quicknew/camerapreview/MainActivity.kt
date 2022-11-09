package com.quicknew.camerapreview

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.quicknew.camerapreview.databinding.ActivityMainBinding
import com.quicknew.camerapreview.interfaces.FrameListener

class MainActivity : AppCompatActivity() {

    companion object {
        private val TAG: String = MainActivity::class.java.simpleName ?: "MainActivity"
    }

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
            Log.w(TAG, "frameDataLegacy ")
        }

        /**
         * 数据帧回调-camera2
         * @param data 帧数据
         * @param isMultipleCamera 是否双目摄像头
         * @param isRgbData 是否RGB相机数据源
         */
        override fun frameDataCamera2(
            data: ByteArray,
            isMultipleCamera: Boolean,
            isRgbData: Boolean,
            width: Int,
            height: Int
        ) {
            Log.w(TAG, "frameDataCamera2")
        }

    }

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.cameraPreviewView.setFrameListener(frameListener)
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