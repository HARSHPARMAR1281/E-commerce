package com.example.email_password

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.database.FirebaseDatabase

class EmailPasswordApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        
        // Enable Firebase Database persistence
        FirebaseDatabase.getInstance().setPersistenceEnabled(true)
    }
} 