package com.keyvault.app.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.keyvault.app.R
import com.keyvault.app.data.NoteItem

class NoteAdapter(
    private var items: List<NoteItem>,
    private val onClick: (NoteItem) -> Unit,
    private val onLongClick: (NoteItem) -> Unit,
) : RecyclerView.Adapter<NoteAdapter.VH>() {
    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val title: TextView = v.findViewById(R.id.title)
        val snippet: TextView = v.findViewById(R.id.snippet)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_note, parent, false))
    override fun getItemCount() = items.size
    override fun onBindViewHolder(holder: VH, position: Int) {
        val n = items[position]
        holder.title.text = n.title
        holder.snippet.text = n.body.take(120)
        holder.itemView.setOnClickListener { onClick(n) }
        holder.itemView.setOnLongClickListener { onLongClick(n); true }
    }
    fun submit(newItems: List<NoteItem>) { items = newItems; notifyDataSetChanged() }
}
