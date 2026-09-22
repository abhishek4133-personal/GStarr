package com.bidding.gstar.ui.slideshow

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.bidding.gstar.R
import com.bidding.gstar.database.DataFetchListener
import com.bidding.gstar.database.DataStoreTable
import com.bidding.gstar.database.EntryJDO
import com.bidding.gstar.database.EntryRepository
import com.bidding.gstar.database.Helper
import com.bidding.gstar.ui.adaptar.ChartTheme
import com.bidding.gstar.ui.adminlogin.ChangePassword
import com.bidding.gstar.ui.adminlogin.ManageDatesActivity
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Date
import java.util.Locale

class InsertValueActivity : AppCompatActivity(), DataFetchListener {

    private enum class PendingOp { NONE, INSERT, DELETE_SLOT, DELETE_ALL }

    private val helper = Helper()
    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.ENGLISH)
    private var mEntry = arrayListOf<Int>()
    private var mDateEntry = arrayListOf<Long>()
    private var todayDateLong = 0L
    private var pendingOp = PendingOp.NONE
    private var pendingValues: ArrayList<Int>? = null
    private var pendingTimes: ArrayList<Long>? = null
    private var hasResumed = false

    private lateinit var pattiEdit: EditText
    private lateinit var submit: View
    private lateinit var previewRow: View
    private lateinit var pattiPreview: TextView
    private lateinit var singlePreview: TextView
    private lateinit var slotCount: TextView
    private lateinit var progress: View
    private lateinit var overlay: View
    private lateinit var editSlotsContainer: LinearLayout
    private lateinit var editEmpty: View
    private lateinit var editDivider: View
    private lateinit var clearTodayButton: View
    private lateinit var repository: EntryRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.insert_value_layout)

        repository = DataStoreTable(this)
        pattiEdit = findViewById(R.id.patti_edit)
        submit = findViewById(R.id.submit)
        previewRow = findViewById(R.id.preview_row)
        pattiPreview = findViewById(R.id.patti)
        singlePreview = findViewById(R.id.pattiValue)
        slotCount = findViewById(R.id.slot_count)
        progress = findViewById(R.id.progress)
        overlay = findViewById(R.id.insert_overlay)
        editSlotsContainer = findViewById(R.id.edit_slots_container)
        editEmpty = findViewById(R.id.edit_empty)
        editDivider = findViewById(R.id.edit_divider)
        clearTodayButton = findViewById(R.id.clear_today_button)

        findViewById<View>(R.id.back).setOnClickListener { finish() }
        findViewById<View>(R.id.changePassword).setOnClickListener {
            startActivity(Intent(this, ChangePassword::class.java))
        }
        findViewById<View>(R.id.manageDates).setOnClickListener {
            startActivity(Intent(this, ManageDatesActivity::class.java))
        }
        clearTodayButton.setOnClickListener { confirmClearToday() }
        submit.setOnClickListener { saveEntry() }
        pattiEdit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                updatePreview(s.toString())
            }
        })

        bindTodayCard(mEntry)
        showLoading(true)
        DataStoreTable(this).fetchCurrentDateEntry(todayDateString())
    }

    override fun onResume() {
        super.onResume()
        if (hasResumed && pendingOp == PendingOp.NONE) {
            DataStoreTable(this).fetchCurrentDateEntry(todayDateString())
        }
        hasResumed = true
    }

    private fun saveEntry() {
        if (pendingOp != PendingOp.NONE) {
            return
        }
        if (mEntry.size >= 8) {
            Toast.makeText(this, R.string.khela_insert_max_today, Toast.LENGTH_SHORT).show()
            return
        }
        val pattiValue = pattiEdit.text.toString().trim()
        if (pattiValue.isEmpty()) {
            Toast.makeText(this, R.string.khela_insert_empty_value, Toast.LENGTH_SHORT).show()
            return
        }
        if (pattiValue.length != 3) {
            Toast.makeText(this, R.string.khela_insert_invalid_value, Toast.LENGTH_SHORT).show()
            return
        }

        pendingOp = PendingOp.INSERT
        showLoading(true)
        submit.isEnabled = false
        mEntry.add(pattiValue.toInt())
        mDateEntry.add(Date().time)

        repository.saveDay(todayEntry(mEntry, mDateEntry))
    }

    private fun confirmSlotDelete(index: Int) {
        if (pendingOp != PendingOp.NONE || index !in mEntry.indices) {
            return
        }
        val patti = mEntry[index].toString()
        showConfirmDialog(
            title = getString(R.string.khela_insert_delete_slot_title, index + 1),
            message = getString(R.string.khela_insert_delete_slot_message, patti)
        ) {
            deleteSlot(index)
        }
    }

    private fun confirmClearToday() {
        if (pendingOp != PendingOp.NONE || mEntry.isEmpty()) {
            return
        }
        showConfirmDialog(
            title = getString(R.string.khela_insert_clear_all_title),
            message = getString(R.string.khela_insert_clear_all_message, todayDateString())
        ) {
            clearToday()
        }
    }

    private fun deleteSlot(index: Int) {
        if (!ensureOnline() || index !in mEntry.indices) {
            return
        }
        val values = ArrayList(mEntry)
        val times = ArrayList(mDateEntry)
        values.removeAt(index)
        if (index in times.indices) {
            times.removeAt(index)
        }
        pendingValues = values
        pendingTimes = times
        showLoading(true)
        if (values.isEmpty()) {
            pendingOp = PendingOp.DELETE_ALL
            repository.deleteDay(todayDateString())
        } else {
            pendingOp = PendingOp.DELETE_SLOT
            repository.saveDay(todayEntry(values, times))
        }
    }

    private fun clearToday() {
        if (!ensureOnline()) {
            return
        }
        pendingOp = PendingOp.DELETE_ALL
        pendingValues = arrayListOf()
        pendingTimes = arrayListOf()
        showLoading(true)
        repository.deleteDay(todayDateString())
    }

    private fun showConfirmDialog(title: String, message: String, onConfirm: () -> Unit) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_khela_confirm, null)
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(dialogView)
        dialog.setCancelable(true)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.86f).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialogView.findViewById<TextView>(R.id.confirm_title).text = title
        dialogView.findViewById<TextView>(R.id.confirm_message).text = message
        dialogView.findViewById<View>(R.id.confirm_cancel).setOnClickListener { dialog.dismiss() }
        dialogView.findViewById<View>(R.id.confirm_delete).setOnClickListener {
            dialog.dismiss()
            onConfirm()
        }
        dialog.show()
    }

    private fun ensureOnline(): Boolean {
        if (helper.isOnline(this)) {
            return true
        }
        Toast.makeText(this, R.string.khela_insert_delete_offline, Toast.LENGTH_SHORT).show()
        return false
    }

    private fun updatePreview(value: String) {
        val ready = value.length == 3 && mEntry.size < 8 && pendingOp == PendingOp.NONE
        submit.isEnabled = ready
        if (value.length == 3) {
            previewRow.visibility = View.VISIBLE
            pattiPreview.text = getString(R.string.khela_insert_patti_preview, value)
            singlePreview.text = getString(R.string.khela_insert_single_preview, toSingle(value))
        } else {
            previewRow.visibility = View.GONE
        }
    }

    private fun bindTodayCard(values: List<Int>) {
        val theme = ChartTheme.at(0)
        val card = findViewById<CardView>(R.id.today_chart_card)
        val headerBar = findViewById<View>(R.id.dateHeaderBar)
        val numbersArea = findViewById<View>(R.id.numbersArea)
        val dateView = findViewById<TextView>(R.id.todayDate)
        val weekdayView = findViewById<TextView>(R.id.weekday)
        val today = Date()

        dateView.text = dateFormat.format(today)
        weekdayView.text = SimpleDateFormat("EEEE", Locale.ENGLISH).format(today)
        card.setCardBackgroundColor(theme.body)
        numbersArea.setBackgroundColor(theme.body)
        headerBar.background = theme.headerDrawable()
        slotCount.text = getString(R.string.khela_insert_slot_count, values.size)

        val pattiViews = arrayOf(
            findViewById<TextView>(R.id.ref_head_1),
            findViewById(R.id.ref_head_2),
            findViewById(R.id.ref_head_3),
            findViewById(R.id.ref_head_4),
            findViewById(R.id.ref_head_5),
            findViewById(R.id.ref_head_6),
            findViewById(R.id.ref_head_7),
            findViewById(R.id.ref_head_8)
        )
        val singleViews = arrayOf(
            findViewById<TextView>(R.id.ref_1),
            findViewById(R.id.ref_2),
            findViewById(R.id.ref_3),
            findViewById(R.id.ref_4),
            findViewById(R.id.ref_5),
            findViewById(R.id.ref_6),
            findViewById(R.id.ref_7),
            findViewById(R.id.ref_8)
        )
        val chipViews = arrayOf(
            findViewById<View>(R.id.chip_1),
            findViewById(R.id.chip_2),
            findViewById(R.id.chip_3),
            findViewById(R.id.chip_4),
            findViewById(R.id.chip_5),
            findViewById(R.id.chip_6),
            findViewById(R.id.chip_7),
            findViewById(R.id.chip_8)
        )
        val empty = getString(R.string.khela_chart_empty_slot)
        for (slot in pattiViews.indices) {
            chipViews[slot].background = theme.roundedBox(theme.chip, dp(14f))
            if (values.size > slot) {
                val value = values[slot].toString()
                pattiViews[slot].text = value
                pattiViews[slot].setTextColor(theme.pattiFilled)
                singleViews[slot].text = toSingle(value)
                singleViews[slot].setTextColor(0xFFFFFFFF.toInt())
                singleViews[slot].background = theme.roundedBox(theme.badge, dp(10f))
            } else {
                pattiViews[slot].text = empty
                pattiViews[slot].setTextColor(theme.pattiEmpty)
                singleViews[slot].text = empty
                singleViews[slot].setTextColor(theme.pattiEmpty)
                singleViews[slot].background = theme.roundedBox(theme.emptyBadge, dp(10f))
            }
        }
        pattiEdit.isEnabled = values.size < 8
        updatePreview(pattiEdit.text.toString())
        bindEditRows(theme)
    }

    private fun bindEditRows(theme: ChartTheme) {
        editSlotsContainer.removeAllViews()
        val hasResults = mEntry.isNotEmpty()
        editEmpty.visibility = if (hasResults) View.GONE else View.VISIBLE
        editDivider.visibility = if (hasResults) View.VISIBLE else View.GONE
        clearTodayButton.isEnabled = hasResults && pendingOp == PendingOp.NONE
        clearTodayButton.alpha = if (hasResults) 1f else 0.4f

        if (!hasResults) {
            return
        }
        mEntry.forEachIndexed { index, value ->
            val row = layoutInflater.inflate(R.layout.item_insert_edit_slot, editSlotsContainer, false)
            val indexView = row.findViewById<TextView>(R.id.edit_slot_index)
            val pattiView = row.findViewById<TextView>(R.id.edit_slot_patti)
            val metaView = row.findViewById<TextView>(R.id.edit_slot_meta)
            val deleteView = row.findViewById<View>(R.id.edit_slot_delete)
            val patti = value.toString()
            val time = slotTime(index)

            indexView.text = (index + 1).toString()
            indexView.background = theme.roundedBox(theme.badge, dp(14f))
            pattiView.text = patti
            metaView.text = getString(
                R.string.khela_insert_edit_meta,
                toSingle(patti),
                time
            )
            deleteView.contentDescription = getString(R.string.khela_insert_delete_slot, index + 1)
            deleteView.setOnClickListener { confirmSlotDelete(index) }
            editSlotsContainer.addView(row)
        }
    }

    private fun slotTime(index: Int): String {
        if (index !in mDateEntry.indices || mDateEntry[index] <= 0L) {
            return getString(R.string.khela_detail_empty_time)
        }
        return timeFormat.format(Date(mDateEntry[index]))
    }

    private fun toSingle(raw: String): String {
        val digits = raw.filter { it.isDigit() }
        if (digits.isEmpty()) {
            return getString(R.string.khela_chart_empty_slot)
        }
        val padded = String.format(Locale.US, "%03d", digits.toInt() % 1000)
        return helper.convertToPattiValue(padded)
    }

    private fun todayDateString(): String {
        return dateFormat.format(Date())
    }

    private fun todayEntry(values: List<Int>, times: List<Long>): EntryJDO {
        return EntryJDO(
            dateLong = if (todayDateLong > 0L) todayDateLong else Date().time,
            dateString = todayDateString(),
            dateEntry = times,
            entry = values
        )
    }

    private fun showLoading(loading: Boolean) {
        overlay.visibility = if (loading) View.VISIBLE else View.GONE
        progress.visibility = if (loading) View.VISIBLE else View.GONE
        clearTodayButton.isEnabled = !loading && mEntry.isNotEmpty()
    }

    private fun applyPendingIfNeeded() {
        pendingValues?.let { mEntry = it }
        pendingTimes?.let { mDateEntry = it }
        pendingValues = null
        pendingTimes = null
    }

    private fun rollbackInsert() {
        if (mEntry.isNotEmpty()) {
            mEntry.removeAt(mEntry.lastIndex)
        }
        if (mDateEntry.isNotEmpty()) {
            mDateEntry.removeAt(mDateEntry.lastIndex)
        }
    }

    private fun dp(value: Float): Float {
        return value * resources.displayMetrics.density
    }

    override fun onDataFetchSuccess(jdo: Any) {
        val fetched = jdo as EntryJDO
        mEntry = ArrayList(fetched.entry)
        mDateEntry = ArrayList(fetched.dateEntry)
        todayDateLong = fetched.dateLong
        pendingOp = PendingOp.NONE
        pendingValues = null
        pendingTimes = null
        bindTodayCard(mEntry)
        showLoading(false)
    }

    override fun onLoginDataFetchSuccess(success: Boolean) {}

    override fun onDataFetchFailure(error: Exception) {
        pendingOp = PendingOp.NONE
        showLoading(false)
        bindTodayCard(mEntry)
    }

    override fun onDataInsertSuccess(success: Boolean) {
        val operation = pendingOp
        if (!success) {
            if (operation == PendingOp.INSERT) {
                rollbackInsert()
            }
            pendingOp = PendingOp.NONE
            pendingValues = null
            pendingTimes = null
            showLoading(false)
            bindTodayCard(mEntry)
            val message = if (operation == PendingOp.INSERT) {
                R.string.khela_insert_failed
            } else {
                R.string.khela_insert_delete_failed
            }
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            return
        }
        if (operation == PendingOp.DELETE_SLOT) {
            applyPendingIfNeeded()
            pendingOp = PendingOp.NONE
            Toast.makeText(this, R.string.khela_insert_delete_success, Toast.LENGTH_SHORT).show()
            pattiEdit.text = null
            DataStoreTable(this).fetchCurrentDateEntry(todayDateString())
            return
        }
        pendingOp = PendingOp.NONE
        Toast.makeText(this, R.string.khela_insert_saved, Toast.LENGTH_SHORT).show()
        pattiEdit.text = null
        DataStoreTable(this).fetchCurrentDateEntry(todayDateString())
    }

    override fun onDataDeleteSuccess(success: Boolean) {
        if (!success) {
            pendingOp = PendingOp.NONE
            pendingValues = null
            pendingTimes = null
            showLoading(false)
            Toast.makeText(this, R.string.khela_insert_delete_failed, Toast.LENGTH_SHORT).show()
            return
        }
        mEntry = arrayListOf()
        mDateEntry = arrayListOf()
        pendingOp = PendingOp.NONE
        pendingValues = null
        pendingTimes = null
        todayDateLong = 0L
        Toast.makeText(this, R.string.khela_insert_clear_success, Toast.LENGTH_SHORT).show()
        bindTodayCard(mEntry)
        showLoading(false)
    }
}
