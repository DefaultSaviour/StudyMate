package uws.ac.uk.studymate.ui

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import uws.ac.uk.studymate.R
import uws.ac.uk.studymate.ui.viewmodels.DeckListItem
import uws.ac.uk.studymate.util.ColorUtils

class DeckListAdapter(
    private var items: List<DeckListItem>,
    private val onTap: (DeckListItem) -> Unit,
    private val onEdit: (DeckListItem) -> Unit,
    private val onDelete: (DeckListItem) -> Unit
) : RecyclerView.Adapter<DeckListAdapter.Row>() {

    class Row(view: View) : RecyclerView.ViewHolder(view) {
        val root: View = view
        val colorDot: View = view.findViewById(R.id.colorDot)
        val name: TextView = view.findViewById(R.id.deckNameText)
        val subtitle: TextView = view.findViewById(R.id.deckSubtitleText)
        val editBtn: MaterialButton = view.findViewById(R.id.editBtn)
        val deleteBtn: MaterialButton = view.findViewById(R.id.deleteBtn)
    }

    fun submit(newItems: List<DeckListItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Row {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_deck, parent, false)
        return Row(view)
    }

    override fun onBindViewHolder(holder: Row, position: Int) {
        val item = items[position]
        holder.name.text = item.deck.name
        val cardLabel = when (item.cardCount) {
            0 -> "No cards yet"
            1 -> "1 card"
            else -> "${item.cardCount} cards"
        }
        holder.subtitle.text = "${item.subjectName} • $cardLabel"

        val color = ColorUtils.parseOrDefault(item.subjectColorHex)
        (holder.colorDot.background as? GradientDrawable)?.setColor(color)

        holder.root.setOnClickListener { onTap(item) }
        holder.editBtn.setOnClickListener { onEdit(item) }
        holder.deleteBtn.setOnClickListener { onDelete(item) }
    }

    override fun getItemCount(): Int = items.size
}
