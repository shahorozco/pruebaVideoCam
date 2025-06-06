package com.coordinadora.pruebavideocam.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.coordinadora.pruebavideocam.application.dagger.PruebasCamApplication

class Connectivity(context: Context) {
    init {
        (context.applicationContext as PruebasCamApplication).getPruebasCamComponent().inject(this)
    }
    fun checkForInternetData(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false

        return activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
    }
}
