package com.connor.asyncdownload.test

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class Weather(
    val country: String,
    val provinceList: List<String>
)


fun parseCityData(jsonString: String): Map<String, List<String>> {
    val jsonObject = JSONObject(jsonString)
    val resultMap = mutableMapOf<String, List<String>>()

    val keys = jsonObject.keys()
    while (keys.hasNext()) {
        val key = keys.next() as String
        val jsonArray = jsonObject.getJSONArray(key)

        val itemList = mutableListOf<String>()
        for (i in 0 until jsonArray.length()) {
            val item = jsonArray.getString(i)
            itemList.add(item)
        }

        resultMap[key] = itemList
    }

    return resultMap
}

suspend fun Context.readFile(name: String) = withContext(Dispatchers.IO) {
    resources.assets.open(name).bufferedReader().readText()
}

