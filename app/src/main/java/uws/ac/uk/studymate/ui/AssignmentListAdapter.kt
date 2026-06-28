package uws.ac.uk.studymate.ui

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import uws.ac.uk.studymate.R
import uws.ac.uk.studymate.ui.viewmodels.AssignmentsItem
import uws.ac.uk.studymate.util.AssignmentDateTimeUtils
import uws.ac.uk.studymate.util.AssignmentIcons
import uws.ac.uk.studymate.util.ColorUtils

class AssignmentListAdapter(
    private var items: List<AssignmentsItem>,
    private val onEdit: (AssignmentsItem) -> Unit,
    private val onDelete: (AssignmentsItem) -> Unit,
    private val onToggleDone: (AssignmentsItem) -> Unit,
    private val onOpenChecklist: (AssignmentsItem) -> Unit,
    private val onOpenDecks: (AssignmentsItem) -> Unit
) : RecyclerView.Adapter<AssignmentListAdapter.Row>() {

    class Row(view: View) : RecyclerView.ViewHolder(view) {
        val badge: View = view.findViewById(R.id.iconBadge)
        val icon: ImageView = view.findViewById(R.id.iconImage)
        val title: TextView = view.findViewById(R.id.assignmentTitleText)
        val due: TextView = view.findViewById(R.id.assignmentDueText)
        val doneBtn: MaterialButton = view.findViewById(R.id.doneBtn)
        val checklistBtn: MaterialButton = view.findViewById(R.id.checklistBtn)
        val editBtn: MaterialButton = view.findViewById(R.id.editBtn)
        val deleteBtn: MaterialButton = view.findViewById(R.id.deleteBtn)
    }

    fun submit(newItems: List<AssignmentsItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Row {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_assignment, parent, false)
        return Row(view)
    }

    override fun onBindViewHolder(holder: Row, position: Int) {
        val item = items[position]
        holder.title.text = item.assignment.title

        // Due text, with a subtle "✓ done/total" checklist hint appended only when
        // the assignment actually has checklist items (keeps clutter-free rows clean).
        val dueText = "Due: ${AssignmentDateTimeUtils.formatDueDate(item.dueAt)}"
        holder.due.text = if (item.taskTotal > 0) {
            "$dueText  ·  ✓ ${item.taskDone}/${item.taskTotal}"
        } else {
            dueText
        }

        // Just the icon in the assignment's colour — no badge background or outline.
        // It's decorative for TalkBack (the title carries the meaning).
        val color = ColorUtils.parseOrDefault(item.colorHex)
        holder.badge.background = null
        holder.icon.setImageResource(AssignmentIcons.drawableForKey(item.iconKey))
        holder.icon.setColorFilter(color)
        holder.icon.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO

        val ctx = holder.itemView.context
        val title = item.assignment.title
        holder.editBtn.contentDescription = ctx.getString(R.string.cd_edit_assignment, title)
        holder.deleteBtn.contentDescription = ctx.getString(R.string.cd_delete_assignment, title)

        // Completed assignments: filled check, struck-through title, dimmed row.
        if (item.isCompleted) {
            holder.doneBtn.setIconResource(R.drawable.ic_check_circle)
            holder.doneBtn.contentDescription = ctx.getString(R.string.cd_mark_not_done, title)
            holder.title.paintFlags = holder.title.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            holder.itemView.alpha = 0.55f
        } else {
            holder.doneBtn.setIconResource(R.drawable.ic_circle_outline)
            holder.doneBtn.contentDescription = ctx.getString(R.string.cd_mark_done, title)
            holder.title.paintFlags = holder.title.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            holder.itemView.alpha = 1f
        }

        holder.doneBtn.setOnClickListener { onToggleDone(item) }
        holder.checklistBtn.setOnClickListener { onOpenChecklist(item) }
        holder.editBtn.setOnClickListener { onEdit(item) }
        holder.deleteBtn.setOnClickListener { onDelete(item) }

        // Tapping the row body opens the flashcard decks screen for this assignment.
        holder.itemView.setOnClickListener { onOpenDecks(item) }
        holder.itemView.contentDescription = "Open flashcard decks for $title"
    }

    override fun getItemCount(): Int = items.size
}
