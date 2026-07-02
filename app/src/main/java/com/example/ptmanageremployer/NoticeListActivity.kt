package com.example.ptmanageremployer

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.ptmanageremployer.data.Extras
import com.example.ptmanageremployer.data.Network
import com.example.ptmanageremployer.data.NoticeDto
import com.example.ptmanageremployer.data.TokenStore
import com.example.ptmanageremployer.data.toUserMessage
import kotlinx.coroutines.launch

/** 매장 공지 목록. 조회(탭 → 상세) + 삭제(사장) + 작성 진입. */
class NoticeListActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_notice_list)
        findViewById<View>(R.id.notice_list_root).applySystemBarInsets()
        findViewById<View>(R.id.btn_back).setOnClickListener { finish() }
        findViewById<View>(R.id.btn_write).setOnClickListener {
            startActivity(Intent(this, NoticeWriteActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        // 작성/삭제 후 돌아왔을 때 최신 목록으로 갱신한다.
        loadNotices()
    }

    private fun loadNotices() {
        val workplaceId = TokenStore.workplaceId
        val container = findViewById<LinearLayout>(R.id.notice_container)
        val empty = findViewById<TextView>(R.id.tv_notice_empty)
        // 기존에 그려둔 공지 행을 비우고(빈 상태 뷰는 유지) 다시 그린다.
        for (i in container.childCount - 1 downTo 0) {
            if (container.getChildAt(i).id != R.id.tv_notice_empty) container.removeViewAt(i)
        }
        if (workplaceId <= 0) {
            empty.visibility = View.VISIBLE
            return
        }
        lifecycleScope.launch {
            try {
                val notices = Network.api.getNotices(workplaceId, page = 0, size = 50).content
                if (notices.isEmpty()) {
                    empty.visibility = View.VISIBLE
                    return@launch
                }
                empty.visibility = View.GONE
                val inflater = LayoutInflater.from(this@NoticeListActivity)
                notices.forEach { notice ->
                    val card = inflater.inflate(R.layout.item_notice, container, false)
                    card.findViewById<TextView>(R.id.tv_title).text = notice.title ?: "(제목 없음)"
                    card.findViewById<TextView>(R.id.tv_meta).text = noticeMeta(notice)
                    card.findViewById<View>(R.id.notice_body).setOnClickListener {
                        startActivity(
                            Intent(this@NoticeListActivity, NoticeDetailActivity::class.java)
                                .putExtra(Extras.NOTICE_ID, notice.id)
                        )
                    }
                    card.findViewById<View>(R.id.btn_delete).setOnClickListener {
                        confirmDelete(notice)
                    }
                    container.addView(card)
                }
            } catch (e: Exception) {
                Toast.makeText(this@NoticeListActivity, e.toUserMessage(), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun confirmDelete(notice: NoticeDto) {
        AlertDialog.Builder(this)
            .setTitle("공지 삭제")
            .setMessage("'${notice.title ?: "이 공지"}'를 삭제할까요?")
            .setPositiveButton("삭제") { _, _ ->
                lifecycleScope.launch {
                    runCatching { Network.api.deleteNotice(notice.id) }
                        .onSuccess {
                            Toast.makeText(this@NoticeListActivity, "공지를 삭제했어요", Toast.LENGTH_SHORT).show()
                            loadNotices()
                        }
                        .onFailure {
                            Toast.makeText(this@NoticeListActivity, it.toUserMessage(), Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun noticeMeta(notice: NoticeDto): String {
        val author = notice.authorName ?: "사장님"
        val date = notice.createdAt?.take(10) ?: ""
        return listOf(author, date).filter { it.isNotBlank() }.joinToString(" · ")
    }
}
