package uws.ac.uk.studymate.ui

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
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
    private val onDelete: (AssignmentsItem) -> Unit
) : RecyclerView.Adapter<AssignmentListAdapter.Row>() {

    class Row(view: View) : RecyclerView.ViewHolder(view) {
        val badge: View = view.findViewById(R.id.iconBadge)
        val icon: ImageView = view.findViewById(R.id.iconImage)
        val title: TextView = view.findViewById(R.id.assignmentTitleText)
        val subject: TextView = view.findViewById(R.id.assignmentSubjectText)
        val due: TextView = view.findViewById(R.id.assignmentDueText)
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
        holder.subject.text = item.subjectName
        holder.due.text = "Due: ${AssignmentDateTimeUtils.formatDueDate(item.dueAt)}"

        val color = ColorUtils.parseOrDefault(item.subjectColorHex)
        (holder.badge.background as? GradientDrawable)?.setColor(color)
        holder.icon.setImageResource(AssignmentIcons.drawableForKey(item.iconKey))
        holder.icon.setColorFilter(Color.WHITE)

        holder.editBtn.setOnClickListener { onEdit(item) }
        holder.deleteBtn.setOnClickListener { onDelete(item) }
    }

    override fun getItemCount(): Int = items.size
}
