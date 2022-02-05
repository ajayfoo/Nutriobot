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
import kotlin.math.exp
import kotlin.math.roundToInt

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
    private fun softmax(input:Float,neuronValues: FloatArray): Double {
        val total: Float = neuronValues.asSequence().map { exp(it) }.sum()
        return exp(input.toDouble()) / total
    }
    private fun getProbability(results:MutableList<Category>):Double{
        val neuronValues=FloatArray(1001)
        val score=results[0].score
        for (item in results) {
            neuronValues[item.index]=item.score
        }
        return softmax(score,neuronValues)
    }
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
    private fun rawPrediction(imgFile: File): MutableList<Category> {
        val img = imageToTensorImage(imgFile)
        val results: List<Classifications> = imageClassifier.classify(img)
        return results[0].categories
    }
    fun filteredPrediction(imgFile: File):Triple<Int,String,Int>{
        val predictionCat = rawPrediction(imgFile)
        val probability=getProbability(predictionCat)
        val confidence= (probability * 100).roundToInt()
        val index=predictionCat[0].index
        if(index in 949..956){
            return Triple(1,predictionCat[0].label,confidence)
        }
        return if((index in 937..948) or (index in listOf(957,958,988))){
            Triple(0,predictionCat[0].label,confidence)
        } else{
            Triple(-1,"",0)
        }
    }
}