package com.ajayk.nutriobot

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.concurrent.Executors
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.lifecycle.lifecycleScope
import com.ajayk.nutriobot.databinding.ActivityMainBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {
    private var imageCapture: ImageCapture? = null
    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var binding: ActivityMainBinding
    private lateinit var photoFile: File
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
        // Set up the listener for take photo button
        binding.cameraCaptureButton.setOnClickListener {
            captureButtonListener()
        }
        binding.backButton.setOnClickListener {
            backButtonListener()
        }
        outputDirectory = getOutputDirectory()

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return
        photoFile = File(
            outputDirectory,
            SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis()) + ".jpg"
        )
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val savedURi = Uri.fromFile(photoFile)
                    val msg = "Photo capture succeeded: $savedURi"
                    //Toast.makeText(baseContext,msg,Toast.LENGTH_SHORT).show()
                    Log.d(TAG, msg)
                }
            }
        )
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            //Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                }
            imageCapture = ImageCapture.Builder().build()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else {
            filesDir
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "CameraXBasic"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            startCamera()
        } else {
            Toast.makeText(this, "Permission(s) not granted by the user", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun disappearCamera() {
        binding.viewFinder.visibility = View.INVISIBLE
        binding.cameraCaptureButton.visibility = View.INVISIBLE
    }

    private fun showInfo() {
        binding.fruit.visibility = View.VISIBLE
        binding.backButton.visibility = View.VISIBLE
        binding.info.visibility = View.VISIBLE
    }

    private fun disappearInfo() {
        binding.fruit.visibility = View.INVISIBLE
        binding.backButton.visibility = View.INVISIBLE
        binding.info.visibility = View.INVISIBLE
        binding.viewFinder.visibility = View.VISIBLE
        binding.cameraCaptureButton.visibility = View.VISIBLE
    }

    private suspend fun waitForImage() {
        while(!photoFile.exists()){
            delay(500)
        }
    }

    private fun captureButtonListener() {
        if(!allPermissionsGranted()){
            disappearCamera()
            refreshFruitInfo()
            showInfo()
            binding.fruit.text = getText(R.string.cam_perm_not_granted)
            binding.info.text = getText(R.string.cam_perm_help)
        }
        else {
            val mainContext = this
            takePhoto()
            lifecycleScope.launch(Dispatchers.Main) {
                disappearCamera()
                refreshFruitInfo()
                showInfo()
                waitForImage()
                val classifier = Classifier(mainContext)
                val result = classifier.filteredPrediction(photoFile)
                setInfo(result,mainContext)
                photoFile.delete()
            }
        }
    }
    private fun backButtonListener() {
        disappearInfo()
    }
    private fun refreshFruitInfo(){
        binding.fruit.text=getText(R.string.finding)
        binding.info.text=getText(R.string.loading)
    }
    private suspend fun setInfo(result:Pair<Int,String?>,context: Context){
        if (result.first==-1){
            binding.fruit.text=getText(R.string.unidentified)
            binding.info.text=getText(R.string.unidentified_info)
        }
        else{
            binding.fruit.text=result.second
            if(result.first==1){
                result.second?.let { FruityResponse.requestFruitInfo(it, context ) }
                FruityResponse.formatFruitInfo(binding.info, context)
            }
            else{
                binding.info.text=getText(R.string.soon)
            }
        }
    }
}