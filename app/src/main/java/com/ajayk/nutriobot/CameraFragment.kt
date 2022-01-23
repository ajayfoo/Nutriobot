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
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
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
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View {
        binding= FragmentCameraBinding.inflate(layoutInflater)
        fragContext=requireContext()
        fragActivity=requireActivity()
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        askCameraAccess()
        binding.cameraCaptureButton.setOnClickListener {
            captureButtonListener()
        }
        outputDirectory = getOutputDirectory()
        cameraExecutor = Executors.newSingleThreadExecutor()
    }
    private fun askCameraAccess(){
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(fragActivity, REQUIRED_PERMISSIONS,
                REQUEST_CODE_PERMISSIONS)
        }
    }
    companion object {
        private const val TAG = "CameraXBasic"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            fragContext, it
        ) == PackageManager.PERMISSION_GRANTED
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
        Log.i("FunInfo","takePhoto() after takePicture")
    }
    private fun captureButtonListener() {
        val isPermsGranted=allPermissionsGranted()
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