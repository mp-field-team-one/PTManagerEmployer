package com.example.ptmanageremployer

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.ptmanageremployer.data.Extras
import com.example.ptmanageremployer.data.Network
import com.example.ptmanageremployer.data.TokenStore
import com.example.ptmanageremployer.data.toUserMessage
import kotlinx.coroutines.launch

class SubFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_sub, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val pending = view.findViewById<View>(R.id.panel_pending)
        val done = view.findViewById<View>(R.id.panel_done)
        val tabPending = view.findViewById<View>(R.id.tab_pending)
        val tabDone = view.findViewById<View>(R.id.tab_done)
        fun select(pendingSel: Boolean) {
            tabPending.setBackgroundResource(if (pendingSel) R.drawable.bg_segment_active else 0)
            tabDone.setBackgroundResource(if (pendingSel) 0 else R.drawable.bg_segment_active)
            pending.visibility = if (pendingSel) View.VISIBLE else View.GONE
            done.visibility = if (pendingSel) View.GONE else View.VISIBLE
        }
        tabPending.setOnClickListener { select(true) }
        tabDone.setOnClickListener { select(false) }
        select(true)

        // 백엔드는 처리 완료 대타 목록 조회 view 를 제공하지 않아 안내만 표시한다.
        view.findViewById<TextView>(R.id.tv_done_empty).visibility = View.VISIBLE
    }

    override fun onResume() {
        super.onResume()
        view?.let { loadPending(it) }
    }

    private fun loadPending(root: View) {
        val workplaceId = TokenStore.workplaceId
        val container = root.findViewById<LinearLayout>(R.id.panel_pending)
        val empty = root.findViewById<TextView>(R.id.tv_pending_empty)
        val tabPending = root.findViewById<TextView>(R.id.tab_pending)
        if (workplaceId <= 0) return
        lifecycleScope.launch {
            try {
                val requests = Network.api.getSwapRequests(workplaceId, view = "pending")
                // 빈 상태 뷰(인덱스 0)만 남기고 이전 카드 제거
                if (container.childCount > 1) container.removeViews(1, container.childCount - 1)
                tabPending.text = "대기 중 ${requests.size}"
                if (requests.isEmpty()) {
                    empty.visibility = View.VISIBLE
                    return@launch
                }
                empty.visibility = View.GONE
                val inflater = LayoutInflater.from(requireContext())
                requests.forEach { req ->
                    val card = inflater.inflate(R.layout.item_swap_request, container, false)
                    card.findViewById<TextView>(R.id.tv_title).text = "대타 요청 #${req.id}"
                    card.findViewById<TextView>(R.id.tv_sub).text = req.reason ?: "사유 없음"
                    card.findViewById<TextView>(R.id.tv_badge).text = "지원자 확인 →"
                    card.setOnClickListener {
                        startActivity(
                            Intent(requireContext(), SubApprovalActivity::class.java)
                                .putExtra(Extras.SWAP_REQUEST_ID, req.id)
                        )
                    }
                    container.addView(card)
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), e.toUserMessage(), Toast.LENGTH_SHORT).show()
            }
        }
    }
}
