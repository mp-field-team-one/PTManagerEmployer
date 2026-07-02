package com.example.ptmanageremployer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.ptmanageremployer.data.Network
import com.example.ptmanageremployer.data.Push
import com.example.ptmanageremployer.data.TokenStore
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val home by lazy { HomeFragment() }
    private val schedule by lazy { ScheduleEditFragment() }
    private val approval by lazy { SubFragment() }
    private val stats by lazy { StatsFragment() }
    private val my by lazy { MyFragment() }

    private val notifPermLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val host = findViewById<View>(R.id.nav_host)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            host.updatePadding(top = bars.top)
            bottomNav.updatePadding(bottom = bars.bottom)
            insets
        }

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> show(home)
                R.id.nav_schedule -> show(schedule)
                R.id.nav_approval -> show(approval)
                R.id.nav_stats -> show(stats)
                R.id.nav_my -> show(my)
                else -> return@setOnItemSelectedListener false
            }
            true
        }
        if (savedInstanceState == null) bottomNav.selectedItemId = R.id.nav_home

        // 최신 계정 정보로 로컬 세션 동기화(GET /api/auth/me) — 매장 소속·역할 갱신.
        lifecycleScope.launch {
            runCatching { Network.api.me() }.getOrNull()?.let { TokenStore.updateUser(it) }
        }

        // 알림 권한(Android 13+) 요청 후 FCM 토큰을 백엔드에 등록한다.
        requestNotifPermission()
        Push.registerCurrentToken()
    }

    private fun requestNotifPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun show(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host, fragment)
            .commit()
    }
}
