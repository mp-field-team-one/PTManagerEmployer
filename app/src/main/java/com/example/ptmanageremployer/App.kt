package com.example.ptmanageremployer

import android.app.Application
import android.content.Intent
import com.example.ptmanageremployer.data.Network
import com.example.ptmanageremployer.data.Push
import com.example.ptmanageremployer.data.TokenStore

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        TokenStore.init(this)
        Push.ensureChannel(this)

        // 리프레시 토큰까지 만료돼 세션 복구가 불가능하면 로그인 화면으로 보낸다.
        // (토큰 정리는 Network 의 authenticator 가 이미 수행한다.)
        Network.onSessionExpired = {
            val i = Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(i)
        }
    }
}
