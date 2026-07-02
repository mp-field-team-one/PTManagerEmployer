package com.example.ptmanageremployer

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.ptmanageremployer.data.Extras
import com.example.ptmanageremployer.data.NoticeDto
import com.example.ptmanageremployer.data.Network
import com.example.ptmanageremployer.data.toUserMessage
import kotlinx.coroutines.launch

class NoticeDetailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_notice_detail)
        findViewById<View>(R.id.detail_root).applySystemBarInsets()
        findViewById<View>(R.id.btn_back).setOnClickListener { finish() }

        val noticeId = intent.getLongExtra(Extras.NOTICE_ID, -1)
        if (noticeId <= 0) {
            Toast.makeText(this, "공지를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        loadNotice(noticeId)
    }

    private fun loadNotice(noticeId: Long) {
        lifecycleScope.launch {
            try {
                val notice = Network.api.getNotice(noticeId)
                findViewById<TextView>(R.id.tv_title).text = notice.title ?: "(제목 없음)"
                findViewById<TextView>(R.id.tv_meta).text = noticeMeta(notice)
                findViewById<TextView>(R.id.tv_body).text = notice.body ?: ""
            } catch (e: Exception) {
                Toast.makeText(this@NoticeDetailActivity, e.toUserMessage(), Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun noticeMeta(notice: NoticeDto): String {
        val author = notice.authorName ?: "사장님"
        val date = notice.createdAt?.replace("T", " ")?.take(16) ?: ""
        return listOf(author, date).filter { it.isNotBlank() }.joinToString(" · ")
    }
}
