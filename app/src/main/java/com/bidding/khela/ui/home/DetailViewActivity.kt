package com.bidding.khela.ui.home

import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.bidding.khela.R
import com.bidding.khela.database.EntryJDO
import com.bidding.khela.database.Helper
import com.bidding.khela.ui.adaptar.ChartTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DetailViewActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ENTRY = "entry"
        const val EXTRA_THEME_INDEX = "theme_index"
        private const val SLOT_COUNT = 8
    }

    private val helper = Helper()
    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.ENGLISH)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        postponeEnterTransition()
        setContentView(R.layout.detail_layout)

        val entry = intent.getSerializableExtra(EXTRA_ENTRY) as? EntryJDO
        if (entry == null) {
            finish()
            return
        }

        val theme = ChartTheme.at(intent.getIntExtra(EXTRA_THEME_INDEX, 0))
        val header = findViewById<View>(R.id.detail_header)
        val slotsContainer = findViewById<LinearLayout>(R.id.slots_container)

        window.statusBarColor = theme.headerEnd
        findViewById<View>(R.id.detail_root).setBackgroundColor(theme.body)
        header.transitionName = ChartTheme.HEADER_TRANSITION
        header.background = theme.headerDrawable()

        findViewById<ImageView>(R.id.back).setOnClickListener {
            finishAfterTransition()
        }
        findViewById<TextView>(R.id.dateHeader).text = entry.dateString
        findViewById<TextView>(R.id.weekday).text = ChartTheme.weekdayName(entry)

        bindSlots(slotsContainer, entry, theme)
        slotsContainer.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                slotsContainer.viewTreeObserver.removeOnPreDrawListener(this)
                startPostponedEnterTransition()
                return true
            }
        })
    }

    override fun onBackPressed() {
        finishAfterTransition()
    }

    private fun bindSlots(container: LinearLayout, entry: EntryJDO, theme: ChartTheme) {
        container.removeAllViews()
        val values = entry.entry
        val times = entry.dateEntry
        val empty = getString(R.string.khela_chart_empty_slot)
        val radius = dp(10f)

        for (slot in 0 until SLOT_COUNT) {
            val slotView = layoutInflater.inflate(R.layout.item_detail_slot, container, false)
            val indexView = slotView.findViewById<TextView>(R.id.slot_index)
            val pattiView = slotView.findViewById<TextView>(R.id.slot_patti)
            val singleView = slotView.findViewById<TextView>(R.id.slot_single)
            val timeView = slotView.findViewById<TextView>(R.id.slot_time)
            val hasValue = values.size > slot
            val patti = if (hasValue) values[slot].toString() else empty
            val single = if (hasValue) toSingleDigit(patti) else empty

            indexView.text = (slot + 1).toString()
            indexView.background = theme.roundedBox(
                if (hasValue) theme.badge else theme.emptyBadge,
                dp(18f)
            )
            indexView.setTextColor(if (hasValue) 0xFFFFFFFF.toInt() else theme.pattiEmpty)

            pattiView.text = patti
            pattiView.setTextColor(if (hasValue) theme.pattiFilled else theme.pattiEmpty)

            singleView.text = single
            singleView.background = theme.roundedBox(
                if (hasValue) theme.badge else theme.emptyBadge,
                radius
            )
            singleView.setTextColor(if (hasValue) 0xFFFFFFFF.toInt() else theme.pattiEmpty)

            timeView.text = slotTime(times, slot)
            timeView.setTextColor(if (hasValue) theme.pattiFilled else theme.pattiEmpty)

            slotView.contentDescription = getString(
                R.string.khela_detail_slot_content_description,
                slot + 1,
                patti,
                single
            )
            (slotView as CardView).setCardBackgroundColor(theme.chip)
            animateSlot(slotView, slot)
            container.addView(slotView)
        }
    }

    private fun slotTime(times: List<Long>, slot: Int): String {
        if (times.size <= slot || times[slot] <= 0L) {
            return getString(R.string.khela_detail_empty_time)
        }
        return timeFormat.format(Date(times[slot]))
    }

    private fun toSingleDigit(raw: String): String {
        val digits = raw.filter { it.isDigit() }
        if (digits.isEmpty()) {
            return getString(R.string.khela_chart_empty_slot)
        }
        val padded = String.format(Locale.US, "%03d", digits.toInt() % 1000)
        return helper.convertToPattiValue(padded)
    }

    private fun animateSlot(view: View, index: Int) {
        view.alpha = 0f
        view.translationY = dp(18f)
        view.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay(60L + (index * 35L))
            .setDuration(280L)
            .start()
    }

    private fun dp(value: Float): Float {
        return value * resources.displayMetrics.density
    }
}
