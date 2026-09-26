package com.keyvault.app.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.keyvault.app.R
import com.keyvault.app.Session
import com.keyvault.app.data.VaultRepository
import com.keyvault.app.util.SearchFilter

class MainActivity : BaseActivity() {
    private lateinit var repo: VaultRepository
    private lateinit var keyAdapter: KeyAdapter
    private lateinit var noteAdapter: NoteAdapter
    private var tab = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        repo = VaultRepository(filesDir)
        if (Session.vault == null) { finish(); return }

        val tabs = findViewById<TabLayout>(R.id.tabs)
        tabs.addTab(tabs.newTab().setText(R.string.tab_keys))
        tabs.addTab(tabs.newTab().setText(R.string.tab_notes))
        tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(t: TabLayout.Tab) { tab = t.position; render() }
            override fun onTabUnselected(t: TabLayout.Tab) {}
            override fun onTabReselected(t: TabLayout.Tab) {}
        })

        keyAdapter = KeyAdapter(emptyList(), onClick = { copy(it.value) }, onLongClick = { openKeyEditor(it.id) })
        findViewById<RecyclerView>(R.id.listKeys).apply {
            layoutManager = LinearLayoutManager(this@MainActivity); adapter = keyAdapter
        }

        noteAdapter = NoteAdapter(emptyList(), onClick = { openNoteEditor(it.id) }, onLongClick = { openNoteEditor(it.id) })
        findViewById<RecyclerView>(R.id.listNotes).apply {
            layoutManager = LinearLayoutManager(this@MainActivity); adapter = noteAdapter
        }

        findViewById<EditText>(R.id.search).addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) = render()
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        })
        findViewById<FloatingActionButton>(R.id.fab).setOnClickListener { onFab() }
        render()
    }

    private fun query(): String = findViewById<EditText>(R.id.search).text.toString()

    private fun render() {
        val v = Session.vault ?: return
        val empty = findViewById<TextView>(R.id.empty)
        if (tab == 0) {
            findViewById<RecyclerView>(R.id.listKeys).visibility = View.VISIBLE
            findViewById<RecyclerView>(R.id.listNotes).visibility = View.GONE
            val list = SearchFilter.keys(v.keys, query())
            keyAdapter.submit(list)
            empty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            empty.text = getString(R.string.empty_keys)
        } else {
            val list = SearchFilter.notes(v.notes, query())
            noteAdapter.submit(list)
            findViewById<RecyclerView>(R.id.listKeys).visibility = View.GONE
            findViewById<RecyclerView>(R.id.listNotes).visibility = View.VISIBLE
            empty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            empty.text = getString(R.string.empty_notes)
        }
    }

    private fun copy(value: String) {
        (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
            .setPrimaryClip(ClipData.newPlainText("key", value))
        Toast.makeText(this, R.string.copied, Toast.LENGTH_SHORT).show()
    }

    private fun onFab() {
        if (tab == 0) openKeyEditor(null) else openNoteEditor(null)
    }

    private fun openKeyEditor(id: String?) {
        startActivity(KeyEditActivity.intent(this, id))
    }

    private fun openNoteEditor(id: String?) {
        startActivity(NoteEditActivity.intent(this, id))
    }
}
