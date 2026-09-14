package com.bidding.khela.ui.adminlogin

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.fragment.NavHostFragment.findNavController
import com.bidding.khela.R
import com.bidding.khela.database.DataFetchListener
import com.bidding.khela.database.DataStoreTable
import com.bidding.khela.database.Helper
import com.bidding.khela.ui.slideshow.InsertValueActivity
import com.bidding.khela.utils.SessionManager
import java.lang.Exception

class AdminLoginFragment : Fragment(), DataFetchListener {

    lateinit var mProgress : ProgressBar
    private lateinit var loginOverlay: View
    private lateinit var sessionManager: SessionManager
    private var hasHandledLogin = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        sessionManager = SessionManager(requireContext())
        val root = inflater.inflate(R.layout.fragment_admin_login, container, false)
        val lLogin = root.findViewById<View>(R.id.login)
        val lUserName = root.findViewById<EditText>(R.id.username)
        val lPassword = root.findViewById<EditText>(R.id.password)
        val backButton = root.findViewById<ImageView>(R.id.back_button)
        mProgress = root.findViewById(R.id.progress)
        loginOverlay = root.findViewById(R.id.login_overlay)

        // Back button click listener
        backButton.setOnClickListener {
            findNavController(this).navigateUp()
        }

        lLogin.setOnClickListener {
            attemptLogin(lUserName, lPassword)
        }
        lPassword.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                attemptLogin(lUserName, lPassword)
                true
            } else {
                false
            }
        }

        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(mUpdateStatus, IntentFilter("com.bidding.khela.login"))
        return root
    }

    private fun attemptLogin(userName: EditText, password: EditText) {
        val fragmentContext = context ?: return
        DataStoreTable(this@AdminLoginFragment).fetchAllLoginData()
        val user = userName.text.toString().trim()
        val pass = password.text.toString()
        if (user.isNotEmpty() && pass.isNotEmpty()) {
            showLoading(true)
            DataStoreTable(this@AdminLoginFragment).login(user, pass, fragmentContext)
        } else {
            Toast.makeText(activity, R.string.khela_login_empty_credentials, Toast.LENGTH_SHORT).show()
        }
    }

    private fun onLoginSuccess() {
        if (hasHandledLogin) {
            return
        }
        hasHandledLogin = true
        sessionManager.saveLoginSession()
        showLoading(false)
        openInsertValueScreen()
        if (isAdded) {
            findNavController(this).navigateUp()
        }
    }

    private fun openInsertValueScreen() {
        startActivity(Intent(activity, InsertValueActivity::class.java))
    }

    private fun showLoading(loading: Boolean) {
        if (view == null) {
            return
        }
        loginOverlay.visibility = if (loading) View.VISIBLE else View.GONE
        mProgress.visibility = if (loading) View.VISIBLE else View.GONE
    }

    override fun onLoginDataFetchSuccess(success: Boolean) {
        if (!isAdded) {
            return
        }
        Log.e("AdminFragment", "success - $success")
        if (success) {
            onLoginSuccess()
        } else {
            showLoading(false)
            Toast.makeText(activity, R.string.khela_login_invalid_password, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDataFetchSuccess(jdo: Any) {}
    override fun onDataInsertSuccess(success: Boolean) {}

    override fun onDestroyView() {
        context?.let { hostContext ->
            LocalBroadcastManager.getInstance(hostContext).unregisterReceiver(mUpdateStatus)
        }
        super.onDestroyView()
    }

    var mUpdateStatus: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (!isAdded) {
                return
            }
            if (Helper().isOnline(context)) {
                try {
                    if(intent.getBooleanExtra("login", false)) {
                        onLoginSuccess()
                    } else {
                        Toast.makeText(activity, R.string.khela_login_invalid_password, Toast.LENGTH_SHORT).show()
                    }
                    showLoading(false)
                } catch (e : Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}
