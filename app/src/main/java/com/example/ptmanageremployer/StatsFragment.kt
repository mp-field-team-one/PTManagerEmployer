package com.example.ptmanageremployer

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.ptmanageremployer.data.Network
import com.example.ptmanageremployer.data.TokenStore
import com.example.ptmanageremployer.data.won
import kotlinx.coroutines.launch
import java.time.LocalDate

class StatsFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_stats, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // 인건비 상세는 별도 화면에서 제공한다.
        view.findViewById<View>(R.id.tv_month_total).setOnClickListener {
            startActivity(Intent(requireContext(), LaborCostActivity::class.java))
        }
        loadStats(view)
    }

    private fun loadStats(view: View) {
        val workplaceId = TokenStore.workplaceId
        if (workplaceId <= 0) return
        val yearMonth = LocalDate.now().toString().substring(0, 7)
        view.findViewById<TextView>(R.id.tv_month_label).text = "$yearMonth 실근태 기준"
        lifecycleScope.launch {
            runCatching { Network.api.getPayroll(workplaceId, yearMonth) }.getOrNull()?.let {
                view.findViewById<TextView>(R.id.tv_month_total).text = won(it.totalAmount)
            }
        }
    }
}
