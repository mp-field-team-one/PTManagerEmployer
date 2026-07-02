package com.example.ptmanageremployer.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * FCM 토큰 등록/해제와 알림 채널을 관리한다.
 * 토큰은 로그인 상태에서만 백엔드(POST/DELETE /api/users/me/device-tokens)에 반영한다.
 */
object Push {
    const val CHANNEL_ID = "ptm_default"
    private val scope = CoroutineScope(Dispatchers.IO)

    /** 알림 채널 생성(Android 8+). 앱 시작 시 1회 호출한다. */
    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = context.getSystemService(NotificationManager::class.java) ?: return
            if (mgr.getNotificationChannel(CHANNEL_ID) == null) {
                mgr.createNotificationChannel(
                    NotificationChannel(CHANNEL_ID, "PTManager 알림", NotificationManager.IMPORTANCE_HIGH)
                )
            }
        }
    }

    /** 현재 FCM 토큰을 로그로 남기고 백엔드에 등록한다(로그인 상태에서만 등록됨). */
    fun registerCurrentToken() {
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            Log.d("PtmPush", "FCM token: $token")
            registerToken(token)
        }
    }

    /** 지정 토큰을 백엔드에 등록한다(서비스 onNewToken 용). */
    fun registerToken(token: String?) {
        if (token.isNullOrBlank() || !TokenStore.isLoggedIn) return
        scope.launch {
            runCatching {
                Network.api.registerDeviceToken(RegisterDeviceTokenRequest(token, "ANDROID"))
            }
        }
    }

    /** 현재 토큰을 suspend 로 가져온다(실패 시 null). */
    suspend fun currentToken(): String? = suspendCancellableCoroutine { cont ->
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { cont.resume(it) }
            .addOnFailureListener { cont.resume(null) }
    }

    /** 로그아웃 시 이 기기의 FCM 토큰을 무효화한다. */
    fun invalidateLocalToken() {
        FirebaseMessaging.getInstance().deleteToken()
    }
}
