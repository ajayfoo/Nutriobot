package com.ajayk.nutriobot

import android.content.Context
import android.view.View
import com.ajayk.nutriobot.databinding.FragmentInfoBinding
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import kotlinx.coroutines.delay
import org.json.JSONObject
import org.json.JSONTokener

class FruityResponse {
    companion object {
        private var responseString:String=""
        fun requestFruitInfo(fruitName: String, context: Context) {
            val queue = Volley.newRequestQueue(context)
            val url = "https://www.fruityvice.com/api/fruit/${fruitName.lowercase()}"
            val stringRequest = StringRequest(
                Request.Method.GET, url,
                { response ->
                    responseString = response
                },
                { responseString  = context.getText(R.string.request_failed).toString() }
            )
            queue.add(stringRequest)
        }

        suspend fun formatFruitInfo(context: Context,binding:FragmentInfoBinding) {
            while (responseString == "") {
                delay(1000)
            }
            if(responseString==context.getText(R.string.request_failed).toString()){
                binding.info.text= responseString
                return
            }
            binding.info.visibility=View.INVISIBLE
            binding.nutriInfo.visibility= View.VISIBLE
            val jsonStr= responseString
            val fruitInfoJson=JSONTokener(jsonStr).nextValue() as JSONObject
            val nutritionJson=fruitInfoJson.getJSONObject("nutritions")
            val nutritionArray=arrayOf("carbohydrates","protein","fat","calories","sugar")
            var i=0
            for (item in nutritionArray){
                val finalValue="\t\t\t${nutritionJson.getString(item)}"
                val nutritionValueView=when(i){
                    0->binding.r1c2
                    1->binding.r2c2
                    2->binding.r3c2
                    3->binding.r4c2
                    else->binding.r5c2
                }
                i+=1
                nutritionValueView.text=finalValue
            }
        }
    }
}