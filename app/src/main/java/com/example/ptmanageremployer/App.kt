package com.example.ptmanageremployer

import android.app.Application
import com.example.ptmanageremployer.data.TokenStore

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        TokenStore.init(this)
    }
}
