package com.bidding.gstar.ui.adminlogin

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.bidding.gstar.R
import com.bidding.gstar.database.EntryJDO
import com.bidding.gstar.ui.adaptar.ChartTheme

class AdminDateAdapter(
    context: Context,
    private val entries: List<EntryJDO>,
    private val onDelete: (EntryJDO) -> Unit
) : BaseAdapter() {

    private val inflater = LayoutInflater.from(context)

    override fun getCount(): Int {
        return entries.size
    }

    override fun getItem(position: Int): EntryJDO {
        return entries[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: inflater.inflate(R.layout.item_manage_date, parent, false)
        val holder = (view.tag as? Holder) ?: Holder(
            dateView = view.findViewById(R.id.manage_date),
            metaView = view.findViewById(R.id.manage_meta),
            pattiView = view.findViewById(R.id.manage_patti),
            deleteView = view.findViewById(R.id.manage_delete)
        ).also { view.tag = it }

        val entry = entries[position]
        val weekday = ChartTheme.weekdayName(entry)
        holder.dateView.text = entry.dateString
        holder.metaView.text = view.context.getString(
            R.string.khela_manage_dates_meta,
            weekday,
            entry.filledCount()
        )
        val preview = entry.pattiPreview()
        holder.pattiView.text = if (preview.isBlank()) {
            view.context.getString(R.string.khela_manage_dates_empty_patti)
        } else {
            preview
        }
        holder.deleteView.contentDescription = view.context.getString(
            R.string.khela_manage_dates_delete,
            entry.dateString
        )
        holder.deleteView.setOnClickListener { onDelete(entry) }
        return view
    }

    private class Holder(
        val dateView: TextView,
        val metaView: TextView,
        val pattiView: TextView,
        val deleteView: View
    )
}
