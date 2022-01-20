package com.ajayk.nutriobot

import android.content.Context
import android.widget.TextView
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

        suspend fun formatFruitInfo(textView: TextView, context: Context) {
            while (responseString == "") {
                delay(1000)
            }
            if(responseString==context.getText(R.string.request_failed).toString()){
                textView.text= responseString
                return
            }
            val jsonStr= responseString
            val fruitInfoJson=JSONTokener(jsonStr).nextValue() as JSONObject
            val nutritionJson=fruitInfoJson.getJSONObject("nutritions")
            val nutritionArray=arrayOf("carbohydrates","protein","fat","calories","sugar")
            var fruitInfoStr=""
            for (item in nutritionArray){
                fruitInfoStr+="\n${item.uppercase()} : ${nutritionJson.getString(item)}"
            }
            textView.text=fruitInfoStr
        }
    }
}