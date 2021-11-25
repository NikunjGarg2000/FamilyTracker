package com.zyrosite.nikunjgarg.familyTrackerApp.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.view.View
import android.view.inputmethod.InputMethodManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.*

object CommonUtils {

    fun isLocationEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    fun getDateAndTime(): String {
        val calendar = Calendar.getInstance()
        calendar.timeZone = TimeZone.getTimeZone("Asia/Calcutta")

        return "${calendar.get(Calendar.YEAR)}/${calendar.get(Calendar.MONTH) + 1}/${
            calendar.get(
                Calendar.DAY_OF_MONTH
            )
        }" + " " +
                "${calendar.get(Calendar.HOUR_OF_DAY)}:${calendar.get(Calendar.MINUTE)}:${
                    calendar.get(
                        Calendar.SECOND
                    )
                }"
    }

    @SuppressLint("MissingPermission")
    fun checkForInternet(context: Context): Boolean {

        // register activity with the connectivity manager service
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // Returns a Network object corresponding to
        // the currently active default data network.
        val network = connectivityManager.activeNetwork ?: return false

        // Representation of the capabilities of an active network.
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false

        return when {
            // Indicates this network uses a Wi-Fi transport,
            // or WiFi has network connectivity
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true

            // Indicates this network uses a Cellular transport. or
            // Cellular has network connectivity
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true

            // else return false
            else -> false
        }
    }

    fun closeKeyboard(activity: Activity) {
        // this will give us the view
        // which is currently focus
        // in this layout
        val view: View? = activity.currentFocus

        // if nothing is currently
        // focus then this will protect
        // the app from crash
        if (view != null) {

            // now assign the system
            // service to InputMethodManager
            val manager: InputMethodManager = activity.getSystemService(
                Context.INPUT_METHOD_SERVICE
            ) as InputMethodManager
            manager
                .hideSoftInputFromWindow(
                    view.windowToken, 0
                )
        }
    }

    fun showDialog(context: Context, message: String, isCancellable: Boolean): MaterialAlertDialogBuilder {
        return MaterialAlertDialogBuilder(context)
            .setMessage(message)
            .setCancelable(isCancellable)
    }
}