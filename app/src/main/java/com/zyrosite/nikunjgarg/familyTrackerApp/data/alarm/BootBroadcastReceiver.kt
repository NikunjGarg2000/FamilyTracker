package com.zyrosite.nikunjgarg.familyTrackerApp.data.alarm

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import com.zyrosite.nikunjgarg.familyTrackerApp.data.datastore.DataStoreManager
import com.zyrosite.nikunjgarg.familyTrackerApp.utils.CommonUtils
import com.zyrosite.nikunjgarg.familyTrackerApp.utils.Constants
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class BootBroadcastReceiver : BroadcastReceiver() {

    private lateinit var dataStoreManager: DataStoreManager
    private lateinit var alarmManager: AlarmManager
    private lateinit var intent: Intent
    private lateinit var pendingIntent: PendingIntent
    private lateinit var myPhone: String

    @SuppressLint("UnspecifiedImmutableFlag", "UnsafeProtectedBroadcastReceiver")
    @DelicateCoroutinesApi
    override fun onReceive(context: Context, intent1: Intent) {
        dataStoreManager = DataStoreManager(context)
        alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        intent = Intent(context, MyAlarmBroadcastReceiver::class.java)
        intent.putExtra(Constants.PHONE_NO, myPhone)
        pendingIntent =
            PendingIntent.getBroadcast(
                context,
                Constants.ALARM_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        GlobalScope.launch {
            dataStoreManager.getPhoneNo().collect {
                myPhone = it
                if (CommonUtils.isLocationEnabled(context)) {
                    if (CommonUtils.checkForInternet(context)) {
                        setAlarm()
                    } else {
                        Toast.makeText(context, "Please connect to Internet!!", Toast.LENGTH_SHORT)
                            .show()
                    }
                } else {
                    Toast.makeText(
                        context,
                        "Please turn on" + " your location...",
                        Toast.LENGTH_LONG
                    )
                        .show()
                    val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    context.startActivity(intent)
                }
                setAlarm()
            }
        }
    }

    private fun setAlarm() {
        alarmManager.setRepeating(
            AlarmManager.RTC,
            5000,
            60000,
            pendingIntent
        )
    }
}