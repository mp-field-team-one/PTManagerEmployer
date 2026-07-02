package com.example.ptmanageremployer

import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.ptmanageremployer.data.CreateShiftRequest
import com.example.ptmanageremployer.data.Network
import com.example.ptmanageremployer.data.ShiftDto
import com.example.ptmanageremployer.data.TokenStore
import com.example.ptmanageremployer.data.UpdateShiftRequest
import com.example.ptmanageremployer.data.shiftTimeRange
import com.example.ptmanageremployer.data.toUserMessage
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * 스케줄 편성 탭. 현재 주(월~일)를 실제 날짜로 표시하고, 선택한 날짜의 근무를
 * GET /api/shifts 로 불러와 렌더링한다. 시프트 추가(POST)·삭제(DELETE)를 지원한다.
 */
class ScheduleEditFragment : Fragment() {

    private val cellIds = intArrayOf(
        R.id.day_cell_0, R.id.day_cell_1, R.id.day_cell_2, R.id.day_cell_3,
        R.id.day_cell_4, R.id.day_cell_5, R.id.day_cell_6,
    )
    private val numIds = intArrayOf(
        R.id.day_num_0, R.id.day_num_1, R.id.day_num_2, R.id.day_num_3,
        R.id.day_num_4, R.id.day_num_5, R.id.day_num_6,
    )

    private val weekDates = ArrayList<LocalDate>(7)
    private var selectedDate: LocalDate = LocalDate.now()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_schedule_edit, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        buildWeek(view)
        view.findViewById<View>(R.id.btn_add_shift).setOnClickListener { showAddShiftDialog() }
        view.findViewById<View>(R.id.btn_attendance)?.setOnClickListener {
            startActivity(Intent(requireContext(), ShiftAttendanceActivity::class.java))
        }
        selectDate(view, selectedDate)
    }

    override fun onResume() {
        super.onResume()
        // 추가/삭제 후 돌아왔을 때 최신 근무로 갱신한다.
        view?.let { loadShifts(it) }
    }

    /** 이번 주(월요일 시작)의 7일을 계산해 날짜 숫자와 클릭 리스너를 세팅한다. */
    private fun buildWeek(view: View) {
        val today = LocalDate.now()
        val monday = today.minusDays((today.dayOfWeek.value - DayOfWeek.MONDAY.value).toLong())
        weekDates.clear()
        for (i in 0..6) weekDates.add(monday.plusDays(i.toLong()))

        view.findViewById<TextView>(R.id.tv_month).text = "${today.year}년 ${today.monthValue}월"
        for (i in 0..6) {
            val date = weekDates[i]
            view.findViewById<TextView>(numIds[i]).text = date.dayOfMonth.toString()
            view.findViewById<View>(cellIds[i]).setOnClickListener { selectDate(view, date) }
        }
    }

    private fun selectDate(view: View, date: LocalDate) {
        selectedDate = date
        for (i in 0..6) {
            val selected = weekDates[i] == date
            val cell = view.findViewById<View>(cellIds[i])
            val num = view.findViewById<TextView>(numIds[i])
            if (selected) {
                cell.setBackgroundResource(R.drawable.bg_day_selected)
                num.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            } else {
                cell.setBackgroundResource(0)
                num.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
            }
        }
        view.findViewById<TextView>(R.id.tv_selected_date).text = "$date 근무"
        loadShifts(view)
    }

    private fun loadShifts(view: View) {
        val workplaceId = TokenStore.workplaceId
        val container = view.findViewById<LinearLayout>(R.id.shift_container)
        val empty = view.findViewById<TextView>(R.id.tv_shift_empty)
        // 기존 근무 행 제거(빈 상태 뷰는 유지).
        for (i in container.childCount - 1 downTo 0) {
            if (container.getChildAt(i).id != R.id.tv_shift_empty) container.removeViewAt(i)
        }
        if (workplaceId <= 0) {
            empty.visibility = View.VISIBLE
            return
        }
        val date = selectedDate.toString()
        lifecycleScope.launch {
            try {
                val shifts = Network.api.getShifts(workplaceId = workplaceId, from = date, to = date)
                    .sortedBy { it.startTime }
                if (shifts.isEmpty()) {
                    empty.visibility = View.VISIBLE
                    return@launch
                }
                empty.visibility = View.GONE
                val inflater = LayoutInflater.from(requireContext())
                shifts.forEach { shift ->
                    val row = inflater.inflate(R.layout.item_shift_edit, container, false)
                    row.findViewById<TextView>(R.id.tv_time).text =
                        shiftTimeRange(shift.startTime, shift.endTime)
                    row.findViewById<TextView>(R.id.tv_worker).text =
                        shift.employeeName ?: "직원 #${shift.employeeId}"
                    row.findViewById<View>(R.id.btn_edit).setOnClickListener { showEditShiftDialog(view, shift) }
                    row.findViewById<View>(R.id.btn_delete).setOnClickListener { confirmDelete(view, shift) }
                    container.addView(row)
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), e.toUserMessage(), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun confirmDelete(view: View, shift: ShiftDto) {
        AlertDialog.Builder(requireContext())
            .setTitle("근무 삭제")
            .setMessage("${shift.employeeName ?: "직원"}님의 ${shiftTimeRange(shift.startTime, shift.endTime)} 근무를 삭제할까요?")
            .setPositiveButton("삭제") { _, _ ->
                lifecycleScope.launch {
                    runCatching { Network.api.deleteShift(shift.id) }
                        .onSuccess {
                            Toast.makeText(requireContext(), "근무를 삭제했어요", Toast.LENGTH_SHORT).show()
                            loadShifts(view)
                        }
                        .onFailure {
                            Toast.makeText(requireContext(), it.toUserMessage(), Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    /** 알바 선택 → 시작/종료 시간 선택 → 선택한 날짜에 근무 편성(POST /api/shifts). */
    private fun showAddShiftDialog() {
        val workplaceId = TokenStore.workplaceId
        if (workplaceId <= 0) {
            Toast.makeText(requireContext(), "소속 매장이 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }
        lifecycleScope.launch {
            val members = runCatching {
                Network.api.getMembers(workplaceId, role = "EMPLOYEE")
            }.getOrNull()
            if (members.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "편성할 알바가 없습니다.", Toast.LENGTH_SHORT).show()
                return@launch
            }
            val names = members.map { it.name ?: "직원 #${it.id}" }.toTypedArray()
            AlertDialog.Builder(requireContext())
                .setTitle("$selectedDate · 근무 편성 — 알바 선택")
                .setItems(names) { _, which ->
                    pickTimeThenCreate(workplaceId, members[which].id, members[which].name ?: "알바")
                }
                .setNegativeButton("취소", null)
                .show()
        }
    }

    private fun pickTimeThenCreate(workplaceId: Long, employeeId: Long, employeeName: String) {
        // 시작 시간 → 종료 시간 순으로 선택받는다.
        TimePickerDialog(requireContext(), { _, startH, startM ->
            TimePickerDialog(requireContext(), { _, endH, endM ->
                val start = String.format("%02d:%02d:00", startH, startM)
                val end = String.format("%02d:%02d:00", endH, endM)
                createShift(workplaceId, employeeId, employeeName, start, end)
            }, 22, 0, true).apply { setTitle("종료 시간") }.show()
        }, 18, 0, true).apply { setTitle("시작 시간") }.show()
    }

    private fun createShift(
        workplaceId: Long, employeeId: Long, employeeName: String, start: String, end: String,
    ) {
        lifecycleScope.launch {
            try {
                Network.api.createShift(
                    CreateShiftRequest(
                        workplaceId = workplaceId,
                        employeeId = employeeId,
                        workDate = selectedDate.toString(),
                        startTime = start,
                        endTime = end,
                    )
                )
                Toast.makeText(
                    requireContext(),
                    "${employeeName}님 근무를 편성했어요 (${start.take(5)}–${end.take(5)})",
                    Toast.LENGTH_SHORT,
                ).show()
                view?.let { loadShifts(it) }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), e.toUserMessage(), Toast.LENGTH_SHORT).show()
            }
        }
    }

    /** 근무 수정: 배정 알바 변경(선택) → 시작/종료 시간 변경 → PATCH /api/shifts/{id}. */
    private fun showEditShiftDialog(view: View, shift: ShiftDto) {
        val workplaceId = TokenStore.workplaceId
        if (workplaceId <= 0) return
        lifecycleScope.launch {
            val members = runCatching {
                Network.api.getMembers(workplaceId, role = "EMPLOYEE")
            }.getOrNull()
            if (members.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "편성할 알바가 없습니다.", Toast.LENGTH_SHORT).show()
                return@launch
            }
            val names = members.map { it.name ?: "직원 #${it.id}" }.toTypedArray()
            val currentIdx = members.indexOfFirst { it.id == shift.employeeId }.coerceAtLeast(0)
            AlertDialog.Builder(requireContext())
                .setTitle("근무 수정 — 알바 선택")
                .setSingleChoiceItems(names, currentIdx) { dialog, which ->
                    dialog.dismiss()
                    editTimeThenUpdate(view, shift, members[which].id)
                }
                .setNegativeButton("취소", null)
                .show()
        }
    }

    private fun editTimeThenUpdate(view: View, shift: ShiftDto, employeeId: Long) {
        val (sh, sm) = hourMinuteOf(shift.startTime, 18, 0)
        val (eh, em) = hourMinuteOf(shift.endTime, 22, 0)
        TimePickerDialog(requireContext(), { _, startH, startM ->
            TimePickerDialog(requireContext(), { _, endH, endM ->
                val start = String.format("%02d:%02d:00", startH, startM)
                val end = String.format("%02d:%02d:00", endH, endM)
                lifecycleScope.launch {
                    try {
                        Network.api.updateShift(
                            shift.id,
                            UpdateShiftRequest(
                                employeeId = employeeId,
                                workDate = selectedDate.toString(),
                                startTime = start,
                                endTime = end,
                            ),
                        )
                        Toast.makeText(requireContext(), "근무를 수정했어요", Toast.LENGTH_SHORT).show()
                        loadShifts(view)
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), e.toUserMessage(), Toast.LENGTH_SHORT).show()
                    }
                }
            }, eh, em, true).apply { setTitle("종료 시간") }.show()
        }, sh, sm, true).apply { setTitle("시작 시간") }.show()
    }

    /** "HH:mm:ss" → (시, 분). 파싱 실패 시 기본값. */
    private fun hourMinuteOf(time: String?, defH: Int, defM: Int): Pair<Int, Int> {
        val parts = time?.split(":") ?: return defH to defM
        val h = parts.getOrNull(0)?.toIntOrNull() ?: defH
        val m = parts.getOrNull(1)?.toIntOrNull() ?: defM
        return h to m
    }
}
