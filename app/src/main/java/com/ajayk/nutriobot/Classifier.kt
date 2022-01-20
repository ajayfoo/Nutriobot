package com.ajayk.nutriobot

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.label.Category
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.vision.classifier.Classifications
import org.tensorflow.lite.task.vision.classifier.ImageClassifier
import org.tensorflow.lite.task.vision.classifier.ImageClassifier.ImageClassifierOptions
import java.io.File

class Classifier(context: Context) {
    private val imgSize=299
    private val modelPath= "inception_v3.tflite"
    private val options: ImageClassifierOptions = ImageClassifierOptions.builder()
        .setBaseOptions(BaseOptions.builder().useGpu().build())
        .setMaxResults(1)
        .build()
    private val imageClassifier:ImageClassifier = ImageClassifier.createFromFileAndOptions(
        context,modelPath,options
    )
    /*
    private fun argmax(arr:FloatArray):Int{
        return arr.indexOfFirst { num -> num == arr.maxByOrNull { it }}
    }

     */
    private fun imageToBitmap(imgFile: File): Bitmap {
        return BitmapFactory.decodeFile(imgFile.absolutePath)
    }
    private fun bitmapToTensorImage(bmp:Bitmap):TensorImage{
        val imageProcessor:ImageProcessor=ImageProcessor.Builder()
            .add(ResizeOp(imgSize,imgSize,ResizeOp.ResizeMethod.BILINEAR))
            .build()
        var tensorImage= TensorImage(DataType.FLOAT32)
        tensorImage.load(bmp)
        tensorImage=imageProcessor.process(tensorImage)
        return tensorImage
    }
    private fun imageToTensorImage(imgFile: File): TensorImage {
        val bmp = imageToBitmap(imgFile)
        return bitmapToTensorImage(bmp)
    }
    private fun rawPrediction(imgFile: File): Category {
        val img = imageToTensorImage(imgFile)
        val results: List<Classifications> = imageClassifier.classify(img)
        val prediction = results[0].categories
        return prediction[0]
    }
    fun filteredPrediction(imgFile: File):Pair<Int,String>{
        val predictionCat = rawPrediction(imgFile)
        val index=predictionCat.index
        if(index in 949..956){
            return Pair(1,predictionCat.label)
        }
        return if((index in 937..948) or (index in listOf(957,958,988))){
            Pair(0,predictionCat.label)
        } else{
            Pair(-1,"")
        }
    }
}