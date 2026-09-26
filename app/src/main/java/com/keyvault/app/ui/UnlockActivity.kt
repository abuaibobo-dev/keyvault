package com.keyvault.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.keyvault.app.R
import com.keyvault.app.Session
import com.keyvault.app.data.VaultRepository
import com.keyvault.app.data.Vault

class UnlockActivity : AppCompatActivity() {
    private lateinit var repo: VaultRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_unlock)
        repo = VaultRepository(filesDir)

        val firstRun = !repo.exists()
        val pw = findViewById<EditText>(R.id.password)
        val confirm = findViewById<EditText>(R.id.passwordConfirm)
        val action = findViewById<Button>(R.id.action)
        val error = findViewById<TextView>(R.id.error)
        val title = findViewById<TextView>(R.id.title)

        confirm.visibility = if (firstRun) View.VISIBLE else View.GONE
        (confirm.parent as? android.view.View)?.visibility = confirm.visibility
        action.text = getString(if (firstRun) R.string.action_save else R.string.unlock)
        title.text = getString(if (firstRun) R.string.set_password else R.string.unlock)

        action.setOnClickListener {
            error.text = ""
            val p = pw.text.toString()
            if (firstRun) {
                if (p.length < 8) { error.text = getString(R.string.error_password_short); return@setOnClickListener }
                if (p != confirm.text.toString()) { error.text = getString(R.string.error_password_mismatch); return@setOnClickListener }
                val key = repo.initVault(p.toCharArray())
                Session.key = key
                Session.vault = repo.load(key)
            } else {
                if (p.isEmpty()) return@setOnClickListener
                try {
                    val (key, vault) = repo.unlock(p.toCharArray())
                    Session.key = key
                    Session.vault = vault
                } catch (e: Exception) {
                    error.text = getString(R.string.error_unlock_failed)
                    return@setOnClickListener
                }
            }
            pw.setText(""); confirm.setText("")
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}