package com.example.ptmanageremployer

import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.ptmanageremployer.data.Network
import com.example.ptmanageremployer.data.TokenStore
import com.example.ptmanageremployer.data.toUserMessage
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 매장 출근용 QR을 표시한다. 서버가 회전형 QR(기본 60초 만료 + 최신 토큰만 유효)이므로,
 * 화면이 떠 있는 동안 주기적으로 새 토큰을 폴링해 QR을 갱신한다.
 */
class QrDisplayActivity : AppCompatActivity() {

    /** 서버 만료(기본 60초)보다 짧게 잡아 만료 전에 갱신한다. */
    private val refreshIntervalMs = 45_000L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val image = ImageView(this)
        val caption = TextView(this).apply {
            text = "출근 QR 불러오는 중…"
            gravity = Gravity.CENTER
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(48, 48, 48, 48)
            addView(image, ViewGroup.LayoutParams(720, 720))
            addView(caption)
        }
        setContentView(root)

        val workplaceId = TokenStore.workplaceId
        if (workplaceId <= 0) {
            Toast.makeText(this, "소속 매장이 없습니다.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // 화면이 보이는 동안(STARTED)만 폴링하고, 백그라운드로 가면 자동 중단된다.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (isActive) {
                    try {
                        val token = Network.api.getQrToken(workplaceId).qrToken
                        image.setImageBitmap(
                            BarcodeEncoder().encodeBitmap(token, BarcodeFormat.QR_CODE, 720, 720)
                        )
                        caption.text = "직원에게 이 QR을 스캔하도록 안내하세요 (자동 갱신 중)"
                    } catch (e: Exception) {
                        caption.text = e.toUserMessage()
                    }
                    delay(refreshIntervalMs)
                }
            }
        }
    }
}
