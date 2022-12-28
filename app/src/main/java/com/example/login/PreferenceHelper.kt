package com.example.login

import android.content.Context
import android.content.SharedPreferences
import android.util.StringBuilderPrinter
import com.example.login.model.Gerai

class PreferenceHelper() {
    private lateinit var preference:SharedPreferences

    fun initPreference(context: Context){
        preference = context.getSharedPreferences("CAMERA_PREFERENCE", Context.MODE_PRIVATE)

    }

    fun saveShopName(shopName: String){
        preference.edit().putString("SHOP_NAME", shopName).apply()
    }

    fun getShopName():String{
        return preference.getString("SHOP_NAME", "").orEmpty()
    }

    fun saveVisit(visit: String){
        preference.edit().putString("VISIT", visit).apply()
    }

    fun getVisit():String{
        return preference.getString("VISIT","").orEmpty()
    }

    fun saveKodeGerai(kodeGerai: String){
        preference.edit().putString("KODE_GERAI", kodeGerai).apply()
    }

    fun getKodeGerai():String{
        return preference.getString("KODE_GERAI","").orEmpty()
    }
    fun saveStatus(status: String){
        preference.edit().putString("STATUS", status).apply()
    }

    fun getStatus():String{
        return preference.getString("STATUS","").orEmpty()
    }
    fun saveRegion(region: String){
        preference.edit().putString("REGION", region).apply()
    }

    fun getRegion():String{
        return preference.getString("REGION","").orEmpty()
    }
    fun saveStoreName(storeName: String){
        preference.edit().putString("STORE_NAME", storeName).apply()
    }

    fun getStoreName():String{
        return preference.getString("STORE_NAME","").orEmpty()
    }

}