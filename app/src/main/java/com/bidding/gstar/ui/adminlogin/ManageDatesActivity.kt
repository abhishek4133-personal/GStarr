package com.bidding.gstar.ui.adminlogin

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bidding.gstar.R
import com.bidding.gstar.database.DataFetchListener
import com.bidding.gstar.database.DataStoreTable
import com.bidding.gstar.database.EntryJDO
import com.bidding.gstar.database.EntryRepository
import com.bidding.gstar.database.Helper

class ManageDatesActivity : AppCompatActivity(), DataFetchListener {

    private val helper = Helper()
    private val entries = mutableListOf<EntryJDO>()
    private lateinit var repository: EntryRepository
    private lateinit var listView: ListView
    private lateinit var emptyState: View
    private lateinit var overlay: View
    private lateinit var adapter: AdminDateAdapter
    private var pendingDate: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_dates)

        repository = DataStoreTable(this)
        listView = findViewById(R.id.dates_list)
        emptyState = findViewById(R.id.empty_state)
        overlay = findViewById(R.id.manage_overlay)
        adapter = AdminDateAdapter(this, entries) { entry ->
            confirmDelete(entry)
        }
        listView.adapter = adapter

        findViewById<View>(R.id.back).setOnClickListener { finish() }
        loadDates()
    }

    private fun loadDates() {
        showLoading(true)
        repository.fetchAllDays(this)
    }

    private fun confirmDelete(entry: EntryJDO) {
        if (pendingDate != null) {
            return
        }
        showConfirmDialog(
            title = getString(R.string.khela_manage_dates_delete_title, entry.dateString),
            message = getString(R.string.khela_manage_dates_delete_message)
        ) {
            deleteDate(entry)
        }
    }

    private fun deleteDate(entry: EntryJDO) {
        if (!helper.isOnline(this)) {
            Toast.makeText(this, R.string.khela_insert_delete_offline, Toast.LENGTH_SHORT).show()
            return
        }
        pendingDate = entry.dateString
        showLoading(true)
        repository.deleteDay(entry.dateString)
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

    private fun bindList(days: List<EntryJDO>) {
        entries.clear()
        entries.addAll(days.sortedByDescending { it.dateLong })
        adapter.notifyDataSetChanged()
        val empty = entries.isEmpty()
        emptyState.visibility = if (empty) View.VISIBLE else View.GONE
        listView.visibility = if (empty) View.GONE else View.VISIBLE
    }

    private fun showLoading(loading: Boolean) {
        overlay.visibility = if (loading) View.VISIBLE else View.GONE
    }

    override fun onDataFetchSuccess(jdo: Any) {
        pendingDate = null
        showLoading(false)
        @Suppress("UNCHECKED_CAST")
        bindList(jdo as? List<EntryJDO> ?: emptyList())
    }

    override fun onLoginDataFetchSuccess(success: Boolean) {}

    override fun onDataInsertSuccess(success: Boolean) {}

    override fun onDataFetchFailure(error: Exception) {
        pendingDate = null
        showLoading(false)
        bindList(entries)
        Toast.makeText(this, R.string.khela_chart_error_title, Toast.LENGTH_SHORT).show()
    }

    override fun onDataDeleteSuccess(success: Boolean) {
        val date = pendingDate
        pendingDate = null
        if (!success || date == null) {
            showLoading(false)
            Toast.makeText(this, R.string.khela_insert_delete_failed, Toast.LENGTH_SHORT).show()
            return
        }
        entries.removeAll { it.dateString == date }
        adapter.notifyDataSetChanged()
        val empty = entries.isEmpty()
        emptyState.visibility = if (empty) View.VISIBLE else View.GONE
        listView.visibility = if (empty) View.GONE else View.VISIBLE
        showLoading(false)
        Toast.makeText(this, getString(R.string.khela_manage_dates_deleted, date), Toast.LENGTH_SHORT).show()
    }
}
