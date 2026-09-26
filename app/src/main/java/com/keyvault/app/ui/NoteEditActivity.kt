package com.keyvault.app.ui

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.keyvault
import com.keyvault.app.R
import com.keyvault.app.Session
import com.keyvault.app.data.NoteItem
import com.keyvault.app.data.TagParser
import com.keyvault.app.data.VaultRepository

class NoteEditActivity : BaseActivity() {
    private lateinit var repo: VaultRepository
    private var existing: NoteItem? = null

    companion object {
        const val EXTRA_ID = "note_id"
        fun intent(context: Context, noteId: String?) =
            Intent(context, NoteEditActivity::class.java).putExtra(EXTRA_ID, noteId)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_note_edit)
        repo = VaultRepository(filesDir)
        existing = Session.vault?.notes?.firstOrNull { it.id == intent.getStringExtra(EXTRA_ID) }

        val title = findViewById<EditText>(R.id.title)
        val body = findViewById<EditText>(R.id.body)
        val tags = findViewById<EditText>(R.id.tags)
        val delete = findViewById<Button>(R.id.delete)

        existing?.let {
            title.setText(it.title); body.setText(it.body)
            tags.setText(it.tags.joinToString(",")); delete.visibility = View.VISIBLE
        }

        findViewById<Button>(R.id.save).setOnClickListener {
            val v = Session.vault ?: return@setOnClickListener
            val key = Session.key ?: return@setOnClickListener
            val t = title.text.toString().trim()
            if (t.isEmpty()) { title.error = getString(R.string.title); return@setOnClickListener }
            val tagList = TagParser.parse(tags.text.toString())
            val cur = existing
            if (cur == null) {
                v.notes.add(NoteItem(title = t, body = body.text.toString(), tags = tagList))
            } else {
                cur.title = t; cur.body = body.text.toString(); cur.tags = tagList
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
                    existing?.let { v.notes.remove(it) }
                    repo.save(key, v)
                    finish()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
    }
}
