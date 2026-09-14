package com.bidding.khela.ui.adminlogin

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bidding.khela.R
import com.bidding.khela.database.DataFetchListener
import com.bidding.khela.database.DataStoreTable
import com.google.android.material.textfield.TextInputLayout

class ChangePassword : AppCompatActivity(), DataFetchListener {

    private lateinit var passwordInput: EditText
    private lateinit var confirmPasswordInput: EditText
    private lateinit var passwordLayout: TextInputLayout
    private lateinit var confirmPasswordLayout: TextInputLayout
    private lateinit var overlay: View
    private lateinit var progress: View
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.change_password)

        passwordInput = findViewById(R.id.password)
        confirmPasswordInput = findViewById(R.id.confirm_password)
        passwordLayout = findViewById(R.id.password_layout)
        confirmPasswordLayout = findViewById(R.id.confirm_password_layout)
        overlay = findViewById(R.id.password_overlay)
        progress = findViewById(R.id.progress)

        findViewById<View>(R.id.back).setOnClickListener { finish() }
        findViewById<View>(R.id.submit).setOnClickListener { submitPassword() }
        confirmPasswordInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                submitPassword()
                true
            } else {
                false
            }
        }
    }

    private fun submitPassword() {
        val password = passwordInput.text.toString().trim()
        val confirmPassword = confirmPasswordInput.text.toString().trim()
        passwordLayout.error = null
        confirmPasswordLayout.error = null

        if (password.isEmpty() || confirmPassword.isEmpty()) {
            if (password.isEmpty()) {
                passwordLayout.error = getString(R.string.khela_password_empty)
            }
            if (confirmPassword.isEmpty()) {
                confirmPasswordLayout.error = getString(R.string.khela_password_empty)
            }
            Toast.makeText(this, R.string.khela_password_empty, Toast.LENGTH_SHORT).show()
            return
        }

        if (password != confirmPassword) {
            confirmPasswordLayout.error = getString(R.string.khela_password_mismatch)
            Toast.makeText(this, R.string.khela_password_mismatch, Toast.LENGTH_SHORT).show()
            return
        }

        showLoading(true)
        DataStoreTable(this).updatePassword(password)
        handler.postDelayed({
            showLoading(false)
            Toast.makeText(this, R.string.khela_password_success, Toast.LENGTH_SHORT).show()
            finish()
        }, 2000)
    }

    private fun showLoading(loading: Boolean) {
        overlay.visibility = if (loading) View.VISIBLE else View.GONE
        progress.visibility = if (loading) View.VISIBLE else View.GONE
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    override fun onDataFetchSuccess(jdo: Any) {}
    override fun onLoginDataFetchSuccess(success: Boolean) {}
    override fun onDataInsertSuccess(success: Boolean) {}
}
