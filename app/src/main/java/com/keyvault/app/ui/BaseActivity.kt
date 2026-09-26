package com.keyvault.app.ui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import com.keyvault.app.Session

open class BaseActivity : AppCompatActivity() {
    override fun onResume() {
        super.onResume()
        if (Session.key == null) {
            startActivity(Intent(this, UnlockActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK))
            finish()
        }
    }
}
