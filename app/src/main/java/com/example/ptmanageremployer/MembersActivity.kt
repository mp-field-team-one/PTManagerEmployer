package com.example.ptmanageremployer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.ptmanageremployer.data.DecisionRequest
import com.example.ptmanageremployer.data.JoinRequestDto
import com.example.ptmanageremployer.data.Network
import com.example.ptmanageremployer.data.TokenStore
import com.example.ptmanageremployer.data.UserDto
import com.example.ptmanageremployer.data.toUserMessage
import kotlinx.coroutines.launch

class MembersActivity : AppCompatActivity() {

    private val workplaceId by lazy { TokenStore.workplaceId }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_members)
        findViewById<View>(R.id.members_root).applySystemBarInsets()
        findViewById<View>(R.id.btn_back).setOnClickListener { finish() }

        if (workplaceId <= 0) {
            Toast.makeText(this, "소속 매장이 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }
        loadPending()
        loadMembers()
    }

    private fun loadPending() {
        val container = findViewById<LinearLayout>(R.id.pending_container)
        val countLabel = findViewById<TextView>(R.id.tv_pending_count)
        lifecycleScope.launch {
            try {
                val requests = Network.api.getJoinRequests(workplaceId, status = "PENDING")
                countLabel.text = "가입 승인 대기 ${requests.size}"
                container.removeAllViews()
                val inflater = LayoutInflater.from(this@MembersActivity)
                requests.forEach { req ->
                    val card = inflater.inflate(R.layout.item_join_request, container, false)
                    card.findViewById<TextView>(R.id.tv_name).text = req.userName ?: "신청자 #${req.userId}"
                    card.findViewById<TextView>(R.id.tv_sub).text = "매장 참여 신청"
                    card.findViewById<View>(R.id.btn_approve).setOnClickListener {
                        decide(req, "APPROVE", card, container, countLabel)
                    }
                    card.findViewById<View>(R.id.btn_reject).setOnClickListener {
                        decide(req, "REJECT", card, container, countLabel)
                    }
                    container.addView(card)
                }
            } catch (e: Exception) {
                Toast.makeText(this@MembersActivity, e.toUserMessage(), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun decide(
        req: JoinRequestDto,
        decision: String,
        card: View,
        container: LinearLayout,
        countLabel: TextView,
    ) {
        card.isEnabled = false
        lifecycleScope.launch {
            try {
                Network.api.decideJoinRequest(req.id, DecisionRequest(decision))
                container.removeView(card)
                countLabel.text = "가입 승인 대기 ${container.childCount}"
                Toast.makeText(
                    this@MembersActivity,
                    if (decision == "APPROVE") "가입을 승인했어요" else "가입을 거절했어요",
                    Toast.LENGTH_SHORT,
                ).show()
                if (decision == "APPROVE") loadMembers()
            } catch (e: Exception) {
                Toast.makeText(this@MembersActivity, e.toUserMessage(), Toast.LENGTH_SHORT).show()
                card.isEnabled = true
            }
        }
    }

    private fun loadMembers() {
        val container = findViewById<LinearLayout>(R.id.members_container)
        val countLabel = findViewById<TextView>(R.id.tv_members_count)
        lifecycleScope.launch {
            try {
                val members = Network.api.getMembers(workplaceId)
                countLabel.text = "멤버 ${members.size}명"
                container.removeAllViews()
                val inflater = LayoutInflater.from(this@MembersActivity)
                members.forEach { member ->
                    val row = inflater.inflate(R.layout.item_member, container, false)
                    row.findViewById<TextView>(R.id.tv_name).text = member.name ?: "이름 없음"
                    row.findViewById<TextView>(R.id.tv_sub).text = roleLabel(member)
                    container.addView(row)
                }
            } catch (e: Exception) {
                Toast.makeText(this@MembersActivity, e.toUserMessage(), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun roleLabel(user: UserDto): String {
        val base = when (user.role) {
            "EMPLOYER" -> "사장님"
            "EMPLOYEE" -> "알바"
            else -> user.email ?: ""
        }
        return if (user.id == TokenStore.userId) "$base · 나" else base
    }
}
