package com.bidding.khela.ui.slideshow

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.bidding.khela.R
import com.bidding.khela.database.DataFetchListener
import com.bidding.khela.database.DataStoreTable
import com.bidding.khela.database.EntryJDO
import com.bidding.khela.database.Helper
import com.bidding.khela.ui.adaptar.ChartTheme
import com.bidding.khela.ui.adminlogin.ChangePassword
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Date
import java.util.Locale

class InsertValueActivity : AppCompatActivity(), DataFetchListener {

    private val helper = Helper()
    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
    private var mEntry = arrayListOf<Int>()
    private var mDateEntry = arrayListOf<Long>()

    private lateinit var pattiEdit: EditText
    private lateinit var submit: View
    private lateinit var previewRow: View
    private lateinit var pattiPreview: TextView
    private lateinit var singlePreview: TextView
    private lateinit var slotCount: TextView
    private lateinit var progress: View
    private lateinit var overlay: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.insert_value_layout)

        pattiEdit = findViewById(R.id.patti_edit)
        submit = findViewById(R.id.submit)
        previewRow = findViewById(R.id.preview_row)
        pattiPreview = findViewById(R.id.patti)
        singlePreview = findViewById(R.id.pattiValue)
        slotCount = findViewById(R.id.slot_count)
        progress = findViewById(R.id.progress)
        overlay = findViewById(R.id.insert_overlay)

        findViewById<View>(R.id.back).setOnClickListener { finish() }
        findViewById<View>(R.id.changePassword).setOnClickListener {
            startActivity(Intent(this, ChangePassword::class.java))
        }
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
        DataStoreTable(this).fetchCurrentDateEntry(dateFormat.format(Date()))
    }

    private fun saveEntry() {
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

        showLoading(true)
        submit.isEnabled = false
        mEntry.add(pattiValue.toInt())
        mDateEntry.add(Date().time)

        val entry = EntryJDO()
        entry.dateLong = Date().time
        entry.dateString = dateFormat.format(Date())
        entry.entry = mEntry
        entry.dateEntry = mDateEntry
        DataStoreTable(this).insertEntryData(entry)
    }

    private fun updatePreview(value: String) {
        val ready = value.length == 3 && mEntry.size < 8
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
    }

    private fun toSingle(raw: String): String {
        val digits = raw.filter { it.isDigit() }
        if (digits.isEmpty()) {
            return getString(R.string.khela_chart_empty_slot)
        }
        val padded = String.format(Locale.US, "%03d", digits.toInt() % 1000)
        return helper.convertToPattiValue(padded)
    }

    private fun showLoading(loading: Boolean) {
        overlay.visibility = if (loading) View.VISIBLE else View.GONE
        progress.visibility = if (loading) View.VISIBLE else View.GONE
    }

    private fun dp(value: Float): Float {
        return value * resources.displayMetrics.density
    }

    override fun onDataFetchSuccess(jdo: Any) {
        val fetched = jdo as EntryJDO
        mEntry = ArrayList(fetched.entry)
        mDateEntry = ArrayList(fetched.dateEntry)
        bindTodayCard(mEntry)
        showLoading(false)
    }

    override fun onLoginDataFetchSuccess(success: Boolean) {}

    override fun onDataInsertSuccess(success: Boolean) {
        if (success) {
            Toast.makeText(this, R.string.khela_insert_saved, Toast.LENGTH_SHORT).show()
            pattiEdit.text = null
            DataStoreTable(this).fetchCurrentDateEntry(dateFormat.format(Date()))
        } else {
            if (mEntry.isNotEmpty()) {
                mEntry.removeAt(mEntry.lastIndex)
            }
            if (mDateEntry.isNotEmpty()) {
                mDateEntry.removeAt(mDateEntry.lastIndex)
            }
            showLoading(false)
            bindTodayCard(mEntry)
            Toast.makeText(this, R.string.khela_insert_failed, Toast.LENGTH_SHORT).show()
        }
    }
}
