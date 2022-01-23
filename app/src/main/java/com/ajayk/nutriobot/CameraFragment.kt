package com.ajayk.nutriobot

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.navigation.Navigation
import androidx.navigation.findNavController
import com.ajayk.nutriobot.databinding.FragmentCameraBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraFragment : Fragment() {
    private lateinit var binding: FragmentCameraBinding
    private var imageCapture: ImageCapture? = null
    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var imgFile: File
    private lateinit var fragContext: Context
    private lateinit var fragActivity:FragmentActivity
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View {
        binding= FragmentCameraBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        fragContext=context
        fragActivity=requireActivity()
        outputDirectory = getOutputDirectory()
        cameraExecutor = Executors.newSingleThreadExecutor()
        requestPermissionLauncher=registerForActivityResult(ActivityResultContracts.RequestPermission()){
                isGranted: Boolean ->
            if (isGranted){
                startCamera()
            }else{
                Toast.makeText(fragContext,"Camera access is needed to use this app",Toast.LENGTH_LONG).show()
            }
        }
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        checkAndRequestPermission()
        binding.cameraCaptureButton.setOnClickListener {
            captureButtonListener()
        }
    }
    companion object {
        private const val TAG = "CameraXBasic"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUIRED_PERMISSION = Manifest.permission.CAMERA
    }
    private fun getOutputDirectory(): File {
        val mediaDir = fragActivity.externalMediaDirs?.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else {
            fragActivity.filesDir
        }
    }
    private fun checkAndRequestPermission(){
        when (PackageManager.PERMISSION_GRANTED) {
            ContextCompat.checkSelfPermission(fragContext, REQUIRED_PERMISSION) -> {
                startCamera()
            }
            else -> {
                requestPermissionLauncher.launch(REQUIRED_PERMISSION)
            }
        }
    }
    private fun isPermissionGranted():Boolean{
        return ContextCompat.checkSelfPermission(fragContext, REQUIRED_PERMISSION) == PackageManager.PERMISSION_GRANTED
    }
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(fragContext)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            //Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                }
            imageCapture=ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(fragActivity , cameraSelector,preview,imageCapture)
        }, ContextCompat.getMainExecutor(fragContext))
    }
    private fun takePhoto() {
        val imageCapture = imageCapture ?: return
        imgFile = File(
            outputDirectory,
            SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis()) + ".jpg"
        )
        val outputOptions = ImageCapture.OutputFileOptions.Builder(imgFile).build()
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(fragContext),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val savedURi = Uri.fromFile(imgFile)
                    val msg = "Photo capture succeeded: $savedURi"
                    Log.d(TAG, msg)
                }
            }
        )
    }
    private fun captureButtonListener() {
        val isPermsGranted=isPermissionGranted()
        if(isPermsGranted){
            takePhoto()
            val action = CameraFragmentDirections.actionCameraFragmentToInfoFragment(imgFile.absolutePath,isPermsGranted)
            binding.cameraCaptureButton.findNavController().navigate(action)
        }
        else{
            val action=CameraFragmentDirections.actionCameraFragmentToInfoFragment("",isPermsGranted)
            binding.cameraCaptureButton.findNavController().navigate(action)
        }
        Navigation.createNavigateOnClickListener(R.id.action_cameraFragment_to_infoFragment)

    }
}