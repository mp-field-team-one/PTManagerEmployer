package com.example.ptmanageremployer.data

import android.content.Context

/**
 * JWT 토큰과 로그인한 사용자 기본 정보를 SharedPreferences 에 보관한다.
 */
object TokenStore {
    private const val PREFS = "ptmanager_auth"
    private const val KEY_ACCESS = "access_token"
    private const val KEY_REFRESH = "refresh_token"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_ROLE = "role"
    private const val KEY_WORKPLACE_ID = "workplace_id"
    private const val KEY_NAME = "name"
    private const val KEY_EMAIL = "email"

    private lateinit var prefs: android.content.SharedPreferences

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    var accessToken: String?
        get() = prefs.getString(KEY_ACCESS, null)
        set(value) = prefs.edit().putString(KEY_ACCESS, value).apply()

    var refreshToken: String?
        get() = prefs.getString(KEY_REFRESH, null)
        set(value) = prefs.edit().putString(KEY_REFRESH, value).apply()

    val userId: Long
        get() = prefs.getLong(KEY_USER_ID, -1L)

    val role: String?
        get() = prefs.getString(KEY_ROLE, null)

    /** 소속 매장 ID. 사장이 아직 매장을 만들지 않았으면 -1. */
    val workplaceId: Long
        get() = prefs.getLong(KEY_WORKPLACE_ID, -1L)

    val name: String?
        get() = prefs.getString(KEY_NAME, null)

    val email: String?
        get() = prefs.getString(KEY_EMAIL, null)

    val isLoggedIn: Boolean
        get() = !accessToken.isNullOrBlank()

    fun saveSession(token: TokenResponse) {
        prefs.edit()
            .putString(KEY_ACCESS, token.accessToken)
            .putString(KEY_REFRESH, token.refreshToken)
            .putLong(KEY_USER_ID, token.user.id)
            .putString(KEY_ROLE, token.user.role)
            .putString(KEY_NAME, token.user.name)
            .putString(KEY_EMAIL, token.user.email)
            .apply {
                val wp = token.user.workplaceId
                if (wp != null) putLong(KEY_WORKPLACE_ID, wp) else remove(KEY_WORKPLACE_ID)
            }
            .apply()
    }

    fun updateUser(user: UserDto) {
        prefs.edit()
            .putLong(KEY_USER_ID, user.id)
            .putString(KEY_ROLE, user.role)
            .putString(KEY_NAME, user.name)
            .putString(KEY_EMAIL, user.email)
            .apply {
                val wp = user.workplaceId
                if (wp != null) putLong(KEY_WORKPLACE_ID, wp) else remove(KEY_WORKPLACE_ID)
            }
            .apply()
    }

    /** 매장 생성 후 소속 매장 ID 를 직접 저장한다. */
    fun setWorkplaceId(id: Long) {
        prefs.edit().putLong(KEY_WORKPLACE_ID, id).apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}
