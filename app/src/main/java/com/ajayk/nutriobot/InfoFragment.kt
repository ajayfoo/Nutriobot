package com.ajayk.nutriobot

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import com.ajayk.nutriobot.databinding.FragmentInfoBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

class InfoFragment : Fragment() {
    private lateinit var binding:FragmentInfoBinding
    private lateinit var imgFile: File
    private lateinit var infoFragContext:Context
    private val args : InfoFragmentArgs by navArgs()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding= FragmentInfoBinding.inflate(layoutInflater)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        infoFragContext=requireContext()
        imgFile=File(args.imagePath)
        initiateInfo(args.permsGranted)
        binding.backButton.setOnClickListener {
            backButtonListener()
        }
        Log.i("CAM_PERMISSION",args.permsGranted.toString())
    }
    private fun backButtonListener() {
        binding.backButton.findNavController().navigate(R.id.action_infoFragment_to_cameraFragment)
    }
    private fun initiateInfo(permsGranted:Boolean) {
        if(!permsGranted) {
            binding.fruit.text = getText(R.string.cam_perm_not_granted)
            binding.info.text = getText(R.string.cam_perm_help)
            return
        }
        lifecycleScope.launch{
            while(!imgFile.exists()){
                delay(500)
            }
            val classifier = Classifier(infoFragContext)
            val result = classifier.filteredPrediction(imgFile)
            setInfo(result, infoFragContext)
            imgFile.delete()
        }
    }
    private fun setInfo(result:Pair<Int,String?>,context: Context){
        if (result.first==-1){
            binding.fruit.text=getText(R.string.unidentified)
            binding.info.text=getText(R.string.unidentified_info)
        }
        else{
            binding.fruit.text=result.second
            if(result.first==1){
                lifecycleScope.launch(Dispatchers.IO){
                    result.second?.let { FruityResponse.requestFruitInfo(it, context ) }
                    FruityResponse.formatFruitInfo(binding.info, context)
                }
            }
            else{
                binding.info.text=getText(R.string.soon)
            }
        }
    }
}