package com.bidding.khela.ui.home

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.ImageView
import android.widget.ListView
import android.widget.ProgressBar
import androidx.core.app.ActivityOptionsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.fragment.NavHostFragment.findNavController
import com.bidding.khela.R
import com.bidding.khela.database.DataFetchListener
import com.bidding.khela.database.DataStoreTable
import com.bidding.khela.database.EntryJDO
import com.bidding.khela.database.Helper
import com.bidding.khela.ui.adaptar.ChartAdapter
import com.bidding.khela.ui.adaptar.ChartTheme
import com.bidding.khela.ui.slideshow.InsertValueActivity
import com.bidding.khela.utils.SessionManager
import java.util.Collections

class HomeFragment : Fragment() {

    private lateinit var lListView: ListView
    private lateinit var mProgress: ProgressBar
    private lateinit var emptyState: View
    private lateinit var refreshButton: ImageView
    private lateinit var adminButton: ImageView
    private lateinit var sessionManager: SessionManager
    private var homeViewModel: HomeViewModel? = null
    private var chartAdapter: ChartAdapter? = null
    private var showSingles = true
    private var mEntryList: MutableList<EntryJDO> = ArrayList()
    private var allEntries: List<EntryJDO> = ArrayList()
    private var currentOffset = 0
    private val pageSize = 20
    private var isLoading = false
    private var hasMoreData = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_home, container, false)
        lListView = root.findViewById(R.id.list_item)
        mProgress = root.findViewById(R.id.progress)
        emptyState = root.findViewById(R.id.empty_state)
        refreshButton = root.findViewById(R.id.refresh_button)
        adminButton = root.findViewById(R.id.admin_button)
        
        // Initialize session manager
        sessionManager = SessionManager(requireContext())

        // Refresh button click listener
        refreshButton.setOnClickListener {
            reloadChart()
        }

        // Admin button click listener - Check session before navigation
        adminButton.setOnClickListener {
            if (sessionManager.isSessionValid()) {
                // Session is valid, go directly to InsertValueActivity
                val intent = Intent(activity, InsertValueActivity::class.java)
                startActivity(intent)
            } else {
                // Session expired or not logged in, go to login
                findNavController(this).navigate(R.id.nav_gallery)
            }
        }

        // Setup pagination scroll listener
        lListView.setOnScrollListener(object : AbsListView.OnScrollListener {
            override fun onScrollStateChanged(view: AbsListView?, scrollState: Int) {}

            override fun onScroll(
                view: AbsListView?,
                firstVisibleItem: Int,
                visibleItemCount: Int,
                totalItemCount: Int
            ) {
                if (!isLoading && hasMoreData && totalItemCount > 0) {
                    val lastVisibleItem = firstVisibleItem + visibleItemCount
                    if (lastVisibleItem >= totalItemCount - 2) {
                        // User is near the end, load more
                        loadMoreItems()
                    }
                }
            }
        })

        lListView.onItemClickListener = android.widget.AdapterView.OnItemClickListener { _, view, position, _ ->
            val header = view.findViewById<View>(R.id.dateHeaderBar)
            header?.transitionName = ChartTheme.HEADER_TRANSITION
            val lIntent = Intent(activity, DetailViewActivity::class.java)
            lIntent.putExtra(DetailViewActivity.EXTRA_ENTRY, mEntryList[position])
            lIntent.putExtra(DetailViewActivity.EXTRA_THEME_INDEX, position)
            val host = requireActivity()
            if (header != null) {
                val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                    host,
                    header,
                    ChartTheme.HEADER_TRANSITION
                )
                host.startActivity(lIntent, options.toBundle())
            } else {
                startActivity(lIntent)
                host.overridePendingTransition(R.anim.khela_detail_enter, R.anim.khela_detail_exit)
            }
        }

        LocalBroadcastManager.getInstance(requireContext())
            .registerReceiver(mUpdateStatus, IntentFilter("com.bidding.khela.fetch"))
        return root
    }

    private fun reloadChart() {
        val fragmentContext = context ?: return
        if (!isAdded) {
            return
        }
        if (Helper().isOnline(fragmentContext)) {
            // Animate the refresh button
            refreshButton.animate()
                .rotation(360f)
                .setDuration(500)
                .withEndAction {
                    refreshButton.rotation = 0f
                }
                .start()
            
            // Reset pagination
            currentOffset = 0
            hasMoreData = true
            mEntryList.clear()
            
            mProgress.visibility = View.VISIBLE
            emptyState.visibility = View.GONE
            isLoading = true
            DataStoreTable(lister).fetchEntryList(fragmentContext)
        }
    }

    private fun loadMoreItems() {
        if (isLoading || !hasMoreData) {
            return
        }

        isLoading = true
        val endIndex = minOf(currentOffset + pageSize, allEntries.size)
        
        if (currentOffset < allEntries.size) {
            val newItems = allEntries.subList(currentOffset, endIndex)
            mEntryList.addAll(newItems)
            currentOffset = endIndex
            hasMoreData = currentOffset < allEntries.size
            
            updateAdapter()
        } else {
            hasMoreData = false
        }
        
        isLoading = false
    }

    private fun updateAdapter() {
        val fragmentContext = context
        if (!isAdded || view == null || fragmentContext == null) {
            return
        }
        
        if (chartAdapter == null) {
            chartAdapter = ChartAdapter(fragmentContext, mEntryList)
            chartAdapter?.setShowSingles(showSingles)
            lListView.adapter = chartAdapter
        } else {
            chartAdapter?.notifyDataSetChanged()
        }
    }

    var lister: DataFetchListener = object : DataFetchListener {
        override fun onDataFetchSuccess(jdo: Any) {
            val fragmentContext = context
            if (!isAdded || view == null || fragmentContext == null) {
                return
            }
            
            isLoading = false
            allEntries = jdo as List<EntryJDO>
            allEntries = sortByStartTime(allEntries, true) as List<EntryJDO>
            
            // Clear and load first page
            mEntryList.clear()
            currentOffset = 0
            hasMoreData = allEntries.isNotEmpty()
            
            // Load first 20 items
            val endIndex = minOf(pageSize, allEntries.size)
            if (endIndex > 0) {
                mEntryList.addAll(allEntries.subList(0, endIndex))
                currentOffset = endIndex
                hasMoreData = currentOffset < allEntries.size
            }
            
            chartAdapter = ChartAdapter(fragmentContext, mEntryList)
            chartAdapter?.setShowSingles(showSingles)
            lListView.adapter = chartAdapter
            
            emptyState.visibility = if (mEntryList.isEmpty()) View.VISIBLE else View.GONE
            lListView.visibility = if (mEntryList.isEmpty()) View.GONE else View.VISIBLE
            mProgress.visibility = View.GONE
        }

        override fun onLoginDataFetchSuccess(success: Boolean) {}
        override fun onDataInsertSuccess(success: Boolean) {}
    }

    override fun onResume() {
        super.onResume()
        val fragmentContext = context ?: return
        if (!isAdded) {
            return
        }
        if (Helper().isOnline(fragmentContext)) {
            mProgress.visibility = View.VISIBLE
            isLoading = true
            DataStoreTable(lister).fetchEntryList(fragmentContext)
        }
    }

    override fun onDestroyView() {
        context?.let { hostContext ->
            LocalBroadcastManager.getInstance(hostContext).unregisterReceiver(mUpdateStatus)
        }
        super.onDestroyView()
    }

    var mUpdateStatus: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {}
    }

    fun sortByStartTime(pCollection: List<EntryJDO>, isSortAscending: Boolean): List<EntryJDO>? {
        Collections.sort(pCollection) { one, two ->
            if (isSortAscending) {
                two.dateLong.compareTo(one.dateLong)
            } else {
                one.dateLong.compareTo(two.dateLong)
            }
        }
        return pCollection
    }
}
