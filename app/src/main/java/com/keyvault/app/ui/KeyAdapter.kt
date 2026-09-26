package com.keyvault.app.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.keyvault.app.R
import com.keyvault.app.data.KeyItem

class KeyAdapter(
    private var items: List<KeyItem>,
    private val onClick: (KeyItem) -> Unit,
    private val onLongClick: (KeyItem) -> Unit,
) : RecyclerView.Adapter<KeyAdapter.VH>() {
    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val name: TextView = v.findViewById(R.id.name)
        val valuePreview: TextView = v.findViewById(R.id.valuePreview)
        val tags: TextView = v.findViewById(R.id.tags)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_key, parent, false))
    override fun getItemCount() = items.size
    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.name.text = item.name
        holder.valuePreview.text = item.value.take(4) + "••••••"
        holder.tags.text = item.tags.joinToString(" ")
        holder.itemView.setOnClickListener { onClick(item) }
        holder.itemView.setOnLongClickListener { onLongClick(item); true }
    }
    fun submit(newItems: List<KeyItem>) { items = newItems; notifyDataSetChanged() }
}
