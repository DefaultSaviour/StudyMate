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

// The list can hold plain section headers (e.g. "Completed assignments") as well
// as deck rows, so the completed decks sit in their own labelled subsection inside
// the same card.
sealed interface DeckRow {
    data class Header(val title: String) : DeckRow
    data class Deck(val item: DeckListItem) : DeckRow
}

class DeckListAdapter(
    private var rows: List<DeckRow>,
    private val onTap: (DeckListItem) -> Unit,
    private val onEdit: (DeckListItem) -> Unit,
    private val onDelete: (DeckListItem) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    class DeckVH(view: View) : RecyclerView.ViewHolder(view) {
        val root: View = view
        val colorDot: View = view.findViewById(R.id.colorDot)
        val name: TextView = view.findViewById(R.id.deckNameText)
        val subtitle: TextView = view.findViewById(R.id.deckSubtitleText)
        val editBtn: MaterialButton = view.findViewById(R.id.editBtn)
        val deleteBtn: MaterialButton = view.findViewById(R.id.deleteBtn)
    }

    class HeaderVH(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.sectionHeaderText)
    }

    fun submitRows(newRows: List<DeckRow>) {
        rows = newRows
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int =
        if (rows[position] is DeckRow.Header) TYPE_HEADER else TYPE_DECK

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_HEADER) {
            HeaderVH(inflater.inflate(R.layout.item_deck_header, parent, false))
        } else {
            DeckVH(inflater.inflate(R.layout.item_deck, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = rows[position]) {
            is DeckRow.Header -> (holder as HeaderVH).title.text = row.title
            is DeckRow.Deck -> bindDeck(holder as DeckVH, row.item)
        }
    }

    private fun bindDeck(holder: DeckVH, item: DeckListItem) {
        holder.name.text = item.deck.name
        val cardLabel = when (item.cardCount) {
            0 -> "No cards yet"
            1 -> "1 card"
            else -> "${item.cardCount} cards"
        }
        val base = "${item.assignmentName} • $cardLabel"
        holder.subtitle.text = if (item.dueText.isNotEmpty()) "$base • ${item.dueText}" else base

        val color = ColorUtils.parseOrDefault(item.assignmentColorHex)
        (holder.colorDot.background as? GradientDrawable)?.setColor(color)

        // Completed-assignment decks read as muted so the active ones stand out.
        holder.root.alpha = if (item.isCompleted) 0.55f else 1f

        holder.root.setOnClickListener { onTap(item) }
        holder.editBtn.setOnClickListener { onEdit(item) }
        holder.deleteBtn.setOnClickListener { onDelete(item) }
    }

    override fun getItemCount(): Int = rows.size

    companion object {
        private const val TYPE_DECK = 0
        private const val TYPE_HEADER = 1
    }
}
