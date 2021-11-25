package com.zyrosite.nikunjgarg.familyTrackerApp.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.zyrosite.nikunjgarg.familyTrackerApp.utils.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val Context.dataStore by preferencesDataStore(Constants.PREFERENCE_NAME)

class DataStoreManager(private val context: Context) {

    // Create some keys we will use them to store and retrieve the data
    companion object {
        val PHONE_NO = stringPreferencesKey(Constants.PHONE_NO)
        val CONTACT_INFO = stringPreferencesKey(Constants.CONTACT_INFO)
        val myTrackers: MutableMap<String, String> = HashMap()
    }

    // Store data
    // refer to the data store and using edit
    // we can store values using the keys
    suspend fun storePhoneNo(phoneNo: String) {
        context.dataStore.edit {
            it[PHONE_NO] = phoneNo
        }
    }

    suspend fun storeContactInfo() {
        var listOfTrackers = ""
        for ((key, value) in myTrackers) {
            if (listOfTrackers.isEmpty()) {
                listOfTrackers = "$key%$value"
            } else {
                listOfTrackers += "%$key%$value"
            }
        }

        if (listOfTrackers.isEmpty()) listOfTrackers = Constants.EMPTY

        context.dataStore.edit {
            it[CONTACT_INFO] = listOfTrackers
        }
    }

    // get the value from data store
    fun getPhoneNo(): Flow<String> = context.dataStore.data.map {
        it[PHONE_NO] ?: Constants.NOT_AVAILABLE
    }

    private fun getContactInfo(): Flow<String> = context.dataStore.data.map {
        it[CONTACT_INFO] ?: Constants.EMPTY
    }

    suspend fun loadTrackersList() {
        myTrackers.clear()
        CoroutineScope(Dispatchers.IO).launch {
            getContactInfo().collect {
                if (it != Constants.EMPTY) {
                    val usersInfo = it.split("%").toTypedArray()
                    var i = 0
                    while (i < usersInfo.size) {
                        myTrackers[usersInfo[i]] = usersInfo[i + 1]
                        i += 2
                    }
                }
            }
        }
    }
}