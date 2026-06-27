package com.example.ptmanageremployer

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.ptmanageremployer.data.CreateShiftRequest
import com.example.ptmanageremployer.data.Network
import com.example.ptmanageremployer.data.TokenStore
import com.example.ptmanageremployer.data.toUserMessage
import kotlinx.coroutines.launch
import java.time.LocalDate

class ScheduleEditFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_schedule_edit, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<View>(R.id.btn_add_shift).setOnClickListener { addShiftForFirstMember() }
        view.findViewById<View>(R.id.btn_attendance)?.setOnClickListener {
            startActivity(Intent(requireContext(), ShiftAttendanceActivity::class.java))
        }
    }

    /**
     * 프로토타입 단계 — 별도 편성 폼이 없어, 첫 알바를 오늘 18:00–22:00 근무로 편성한다.
     * 실제 API(POST /api/shifts)를 호출해 백엔드 연동을 검증한다.
     */
    private fun addShiftForFirstMember() {
        val workplaceId = TokenStore.workplaceId
        if (workplaceId <= 0) {
            Toast.makeText(requireContext(), "소속 매장이 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }
        lifecycleScope.launch {
            try {
                val members = Network.api.getMembers(workplaceId, role = "EMPLOYEE")
                val employee = members.firstOrNull()
                if (employee == null) {
                    Toast.makeText(requireContext(), "편성할 알바가 없습니다.", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                val today = LocalDate.now().toString()
                Network.api.createShift(
                    CreateShiftRequest(
                        workplaceId = workplaceId,
                        employeeId = employee.id,
                        workDate = today,
                        startTime = "18:00:00",
                        endTime = "22:00:00",
                    )
                )
                Toast.makeText(
                    requireContext(),
                    "${employee.name ?: "알바"}님 근무를 편성했어요 (오늘 18:00–22:00)",
                    Toast.LENGTH_SHORT,
                ).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), e.toUserMessage(), Toast.LENGTH_SHORT).show()
            }
        }
    }
}
