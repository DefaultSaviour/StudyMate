package uws.ac.uk.studymate.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import uws.ac.uk.studymate.R
import uws.ac.uk.studymate.data.entities.FlashCard

class CardListAdapter(
    private var items: List<FlashCard>,
    private val onEdit: (FlashCard) -> Unit,
    private val onDelete: (FlashCard) -> Unit
) : RecyclerView.Adapter<CardListAdapter.Row>() {

    class Row(view: View) : RecyclerView.ViewHolder(view) {
        val front: TextView = view.findViewById(R.id.cardFrontText)
        val editBtn: MaterialButton = view.findViewById(R.id.editBtn)
        val deleteBtn: MaterialButton = view.findViewById(R.id.deleteBtn)
    }

    fun submit(newItems: List<FlashCard>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Row {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_card, parent, false)
        return Row(view)
    }

    override fun onBindViewHolder(holder: Row, position: Int) {
        val item = items[position]
        holder.front.text = item.front
        holder.editBtn.setOnClickListener { onEdit(item) }
        holder.deleteBtn.setOnClickListener { onDelete(item) }
    }

    override fun getItemCount(): Int = items.size
}
