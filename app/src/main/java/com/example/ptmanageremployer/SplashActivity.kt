package com.example.ptmanageremployer

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.ptmanageremployer.data.TokenStore

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash)
        Handler(Looper.getMainLooper()).postDelayed({
            val next = when {
                !TokenStore.isLoggedIn -> LoginActivity::class.java
                TokenStore.workplaceId > 0 -> MainActivity::class.java
                else -> CreateStoreActivity::class.java
            }
            startActivity(Intent(this, next))
            finish()
        }, 1100)
    }
}
