package com.quicknew.camerapreview

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.quicknew.camerapreview.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    override fun onPostResume() {
        super.onPostResume()
        binding.cameraPreviewView.startPreview()
    }
}