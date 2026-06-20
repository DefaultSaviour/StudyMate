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
    private val onToggleDone: (AssignmentsItem) -> Unit
) : RecyclerView.Adapter<AssignmentListAdapter.Row>() {

    class Row(view: View) : RecyclerView.ViewHolder(view) {
        val badge: View = view.findViewById(R.id.iconBadge)
        val icon: ImageView = view.findViewById(R.id.iconImage)
        val title: TextView = view.findViewById(R.id.assignmentTitleText)
        val due: TextView = view.findViewById(R.id.assignmentDueText)
        val doneBtn: MaterialButton = view.findViewById(R.id.doneBtn)
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
        holder.due.text = "Due: ${AssignmentDateTimeUtils.formatDueDate(item.dueAt)}"

        // Just the icon in the assignment's colour — no badge background or outline.
        val color = ColorUtils.parseOrDefault(item.colorHex)
        holder.badge.background = null
        holder.icon.setImageResource(AssignmentIcons.drawableForKey(item.iconKey))
        holder.icon.setColorFilter(color)

        // Completed assignments: filled check, struck-through title, dimmed row.
        if (item.isCompleted) {
            holder.doneBtn.setIconResource(R.drawable.ic_check_circle)
            holder.title.paintFlags = holder.title.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            holder.itemView.alpha = 0.55f
        } else {
            holder.doneBtn.setIconResource(R.drawable.ic_circle_outline)
            holder.title.paintFlags = holder.title.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            holder.itemView.alpha = 1f
        }

        holder.doneBtn.setOnClickListener { onToggleDone(item) }
        holder.editBtn.setOnClickListener { onEdit(item) }
        holder.deleteBtn.setOnClickListener { onDelete(item) }
    }

    override fun getItemCount(): Int = items.size
}
