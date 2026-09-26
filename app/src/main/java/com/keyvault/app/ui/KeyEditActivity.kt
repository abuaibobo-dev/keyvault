package com.keyvault.app.ui

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.keyvault.app.R
import com.keyvault.app.Session
import com.keyvault.app.data.KeyItem
import com.keyvault.app.data.TagParser
import com.keyvault.app.data.VaultRepository
import com.keyvault.app.util.KeyGenerator

class KeyEditActivity : BaseActivity() {
    private lateinit var repo: VaultRepository
    private var existing: KeyItem? = null

    companion object {
        const val EXTRA_ID = "key_id"
        fun intent(context: Context, keyId: String?) =
            Intent(context, KeyEditActivity::class.java).putExtra(EXTRA_ID, keyId)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_key_edit)
        repo = VaultRepository(filesDir)
        val id = intent.getStringExtra(EXTRA_ID)
        existing = Session.vault?.keys?.firstOrNull { it.id == id }

        val name = findViewById<EditText>(R.id.name)
        val value = findViewById<EditText>(R.id.value)
        val note = findViewById<EditText>(R.id.note)
        val tags = findViewById<EditText>(R.id.tags)
        val len = findViewById<EditText>(R.id.valueLength)
        val delete = findViewById<Button>(R.id.delete)

        existing?.let {
            name.setText(it.name); value.setText(it.value)
            note.setText(it.note); tags.setText(it.tags.joinToString(","))
            delete.visibility = View.VISIBLE
        }

        findViewById<Button>(R.id.generate).setOnClickListener {
            val n = len.text.toString().toIntOrNull() ?: 32
            value.setText(KeyGenerator.generate(n.coerceIn(8, 128)))
        }

        findViewById<Button>(R.id.save).setOnClickListener {
            val v = Session.vault ?: return@setOnClickListener
            val key = Session.key ?: return@setOnClickListener
            val nm = name.text.toString().trim()
            if (nm.isEmpty()) { name.error = getString(R.string.name); return@setOnClickListener }
            val tagList = TagParser.parse(tags.text.toString())
            val cur = existing
            if (cur == null) {
                v.keys.add(KeyItem(name = nm, value = value.text.toString(),
                    note = note.text.toString(), tags = tagList))
            } else {
                cur.name = nm; cur.value = value.text.toString()
                cur.note = note.text.toString(); cur.tags = tagList
                cur.updatedAt = System.currentTimeMillis()
            }
            repo.save(key, v)
            finish()
        }

        delete.setOnClickListener {
            val v = Session.vault ?: return@setOnClickListener
            val key = Session.key ?: return@setOnClickListener
            AlertDialog.Builder(this)
                .setMessage(R.string.confirm_delete)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    existing?.let { v.keys.remove(it) }
                    repo.save(key, v)
                    finish()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
    }
}
