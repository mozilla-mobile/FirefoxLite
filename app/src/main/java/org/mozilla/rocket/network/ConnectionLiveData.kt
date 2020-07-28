package org.mozilla.rocket.network

import android.annotation.TargetApi
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkInfo
import android.net.NetworkRequest
import android.os.Build
import androidx.lifecycle.LiveData
import java.util.concurrent.atomic.AtomicBoolean

class ConnectionLiveData(private val connectivityManager: ConnectivityManager) : LiveData<Boolean>() {

    private val mCurrentNetworkStatus = AtomicBoolean(false)

    private val connectivityManagerCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network?) {
                if (mCurrentNetworkStatus.compareAndSet(false, true)) {
                    postValue(true)
                }
            }

            override fun onLost(network: Network?) {
                if (mCurrentNetworkStatus.compareAndSet(true, false)) {
                    postValue(false)
                }
            }
        }

    override fun onActive() {
        super.onActive()
        updateConnection()
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.N -> connectivityManager.registerDefaultNetworkCallback(connectivityManagerCallback)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP -> lollipopNetworkAvailableRequest()
        }
    }

    override fun onInactive() {
        super.onInactive()
        connectivityManager.unregisterNetworkCallback(connectivityManagerCallback)
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private fun lollipopNetworkAvailableRequest() {
        val builder = NetworkRequest.Builder()
                .addTransportType(android.net.NetworkCapabilities.TRANSPORT_CELLULAR)
                .addTransportType(android.net.NetworkCapabilities.TRANSPORT_WIFI)
        connectivityManager.registerNetworkCallback(builder.build(), connectivityManagerCallback)
    }

    private fun updateConnection() {
        val activeNetwork: NetworkInfo? = connectivityManager.activeNetworkInfo
        mCurrentNetworkStatus.set(activeNetwork?.isConnected == true)
        postValue(activeNetwork?.isConnected == true)
    }
}