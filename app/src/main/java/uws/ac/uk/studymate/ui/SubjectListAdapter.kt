package uws.ac.uk.studymate.ui

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import uws.ac.uk.studymate.R
import uws.ac.uk.studymate.data.relations.SubjectWithAssignments

class SubjectListAdapter(
    private var items: List<SubjectWithAssignments>,
    private val onEdit: (SubjectWithAssignments) -> Unit,
    private val onDelete: (SubjectWithAssignments) -> Unit
) : RecyclerView.Adapter<SubjectListAdapter.SubjectRow>() {

    class SubjectRow(view: android.view.View) : RecyclerView.ViewHolder(view) {
        val colorDot: android.view.View = view.findViewById(R.id.colorDot)
        val nameText: TextView = view.findViewById(R.id.subjectNameText)
        val countText: TextView = view.findViewById(R.id.assignmentCountText)
        val editBtn: MaterialButton = view.findViewById(R.id.editBtn)
        val deleteBtn: MaterialButton = view.findViewById(R.id.deleteBtn)
    }

    fun submit(newItems: List<SubjectWithAssignments>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubjectRow {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_subject, parent, false)
        return SubjectRow(view)
    }

    override fun onBindViewHolder(holder: SubjectRow, position: Int) {
        val item = items[position]
        holder.nameText.text = item.subject.name

        val count = item.assignments.size
        holder.countText.text = when (count) {
            0 -> "No assignments"
            1 -> "1 assignment"
            else -> "$count assignments"
        }

        val colorInt = parseColorOrDefault(item.subject.color)
        (holder.colorDot.background as? GradientDrawable)?.setColor(colorInt)

        holder.editBtn.setOnClickListener { onEdit(item) }
        holder.deleteBtn.setOnClickListener { onDelete(item) }
    }

    override fun getItemCount(): Int = items.size

    private fun parseColorOrDefault(hex: String?): Int {
        if (hex.isNullOrBlank()) return Color.parseColor("#C4A24A")
        return try {
            Color.parseColor(hex)
        } catch (_: IllegalArgumentException) {
            Color.parseColor("#C4A24A")
        }
    }
}
