package com.example.ptmanageremployer

import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.ptmanageremployer.data.Network
import com.example.ptmanageremployer.data.TokenStore
import com.example.ptmanageremployer.data.toUserMessage
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import kotlinx.coroutines.launch

/** 매장 출근용 QR을 화면에 표시한다. 직원이 이 QR을 스캔해 출근 체크인한다. */
class QrDisplayActivity : AppCompatActivity() {

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

        lifecycleScope.launch {
            try {
                val token = Network.api.getQrToken(workplaceId).qrToken
                image.setImageBitmap(BarcodeEncoder().encodeBitmap(token, BarcodeFormat.QR_CODE, 720, 720))
                caption.text = "직원에게 이 QR을 스캔하도록 안내하세요"
            } catch (e: Exception) {
                caption.text = e.toUserMessage()
            }
        }
    }
}
