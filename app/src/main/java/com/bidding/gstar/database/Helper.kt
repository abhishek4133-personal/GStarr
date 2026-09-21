package com.bidding.gstar.database

import android.content.Context
import android.net.ConnectivityManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Helper {

    fun isOnline(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val netInfo = cm.activeNetworkInfo
        return netInfo != null && netInfo.isConnected
    }

    fun convertStringToArrayListOfInteger(pArrayString: String?): List<Int> {
        val lTypeToken: TypeToken<List<Int?>?> = object : TypeToken<List<Int?>?>() {}
        val lGson = Gson()
        return lGson.fromJson(pArrayString, lTypeToken.type)
    }

    fun convertToPattiValue(lValue: String): String {
        val lPattiValueInt = lValue[0].toString().toInt() + lValue[1].toString().toInt() + lValue[2].toString().toInt()
        return if (lPattiValueInt < 10) {
            lPattiValueInt.toString()
        } else {
            lPattiValueInt.toString().takeLast(1)
        }
    }
}