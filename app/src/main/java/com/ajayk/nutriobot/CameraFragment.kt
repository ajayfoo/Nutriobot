package com.ajayk.nutriobot

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.navigation.Navigation
import androidx.navigation.findNavController
import com.ajayk.nutriobot.databinding.FragmentCameraBinding
import com.yalantis.ucrop.UCrop
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
    private lateinit var fragContext: Context
    private lateinit var fragActivity:FragmentActivity
    private lateinit var imgFile:File
    private lateinit var croppedImgFile:File
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var cropImageLauncher:ActivityResultLauncher<Any?>
    private lateinit var cameraProvider: ProcessCameraProvider
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
        imgFile = File.createTempFile(
            "IMG_${SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis())}",
            ".jpg", outputDirectory)
        croppedImgFile = File.createTempFile(
            "IMG_${SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis())}",
            ".jpg", outputDirectory)
        requestPermissionLauncher=registerForActivityResult(ActivityResultContracts.RequestPermission()){
                isGranted: Boolean ->
            if (isGranted){
                startCamera()
            }else{
                Toast.makeText(fragContext,"Camera access is needed to use this app",Toast.LENGTH_LONG).show()
            }
        }
        val cropImageContract=object: ActivityResultContract<Any?,Uri?>(){
            override fun createIntent(context: Context, input: Any?): Intent {
                val options=UCrop.Options()
                options.setCompressionQuality(100)
                options.withAspectRatio(1f,1f)
                return UCrop.of(Uri.parse(imgFile.absolutePath),Uri.parse((croppedImgFile.absolutePath)))
                    .withOptions(options)
                    .getIntent(fragContext)
            }
            override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
                return intent?.let { UCrop.getOutput(it) }
            }
        }
        cropImageLauncher=registerForActivityResult(cropImageContract){
            navigateToInfoFragment(File(it?.path!!))
        }
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        checkAndRequestPermission()
        binding.cameraCaptureButton.setOnClickListener {
            takePhoto()
            showProgressBar(true)
        }
    }
    override fun onDetach() {
        super.onDetach()
        cameraExecutor.shutdown()
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
                showProgressBar(true)
            }
        }
    }
    private fun isPermissionGranted():Boolean{
        return ContextCompat.checkSelfPermission(fragContext, REQUIRED_PERMISSION) == PackageManager.PERMISSION_GRANTED
    }
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(fragContext)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            //Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                }
            imageCapture=ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .setIoExecutor(cameraExecutor)
                .build()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(fragActivity , cameraSelector,preview,imageCapture)
        }, ContextCompat.getMainExecutor(fragContext))
    }
    private fun takePhoto(){
        val imageCapture = imageCapture ?: return
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
                    navigateToInfoFragment(imgFile)
                    showProgressBar(false)
                }
            }
        )
    }
    private fun navigateToInfoFragment(imgFile:File){
        val action = CameraFragmentDirections.actionCameraFragmentToInfoFragment(imgFile.absolutePath,isPermissionGranted())
        binding.cameraCaptureButton.findNavController().navigate(action)
        Navigation.createNavigateOnClickListener(R.id.action_cameraFragment_to_infoFragment)
    }
    private fun showProgressBar(show:Boolean){
        if(show){
            binding.progressBarLayout.visibility=View.VISIBLE
            fragActivity.window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
        }
        else{
            binding.progressBarLayout.visibility=View.GONE
            fragActivity.window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
        }
    }
}