package com.vidyavahini.app

import android.app.Application
import android.util.Log
import com.google.firebase.database.FirebaseDatabase

import dagger.hilt.android.HiltAndroidApp

/**
 * Application class — initializes Firebase offline persistence.
 * CRITICAL for rural 2G areas: caches last known ping when offline.
 * Must be registered in AndroidManifest.xml as android:name=".MyApplication"
 */
@HiltAndroidApp
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        try {
            // Enable offline persistence BEFORE any DB calls — order matters!
            FirebaseDatabase.getInstance("https://vidya-vahini-20c3d-default-rtdb.asia-southeast1.firebasedatabase.app").setPersistenceEnabled(true)
        } catch (e: Exception) {
            Log.e("MyApplication", "Failed to setPersistenceEnabled: ${e.message}")
        }
    }
}
