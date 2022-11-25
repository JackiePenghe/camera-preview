package com.quicknew.camerapreview

import android.os.Bundle
import android.util.Log
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
            Log.w("TAG", "frameDataLegacy isMultipleCamera:$isMultipleCamera isRgbData:$isRgbData")
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
            Log.w("TAG", "frameDataCamera2")
        }

        /**
         * 数据帧回调
         * @param data NV21数据
         * @param isMultipleCamera 是否双目摄像头
         * @param isRgbData 是否RGB相机数据源
         */
        override fun frameDataCameraX(
            data: ByteArray,
            isMultipleCamera: Boolean,
            isRgbData: Boolean,
            width: Int,
            height: Int
        ) {
            Log.w("TAG", "frameDataCameraX")
        }

        /**
         * 出现错误
         */
        override fun error(errCode: Int, errMsg: String) {
//            Log.e("TAG", "errCode $errCode : $errMsg")
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