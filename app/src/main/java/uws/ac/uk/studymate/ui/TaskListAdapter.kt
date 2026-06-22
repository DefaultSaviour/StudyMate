package uws.ac.uk.studymate.ui

import android.graphics.Color
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import uws.ac.uk.studymate.R
import uws.ac.uk.studymate.data.entities.AssignmentTask

/*//////////////////////
Checklist rows (0.9J) — shared by the Assignments checklist panel and the focus
timer. Tapping the checkbox toggles done; the bin deletes (management panel only,
so it's hidden when [onDelete] is null, e.g. in the focus timer).
 *//////////////////////
class TaskListAdapter(
    private var items: List<AssignmentTask>,
    private val onToggle: (AssignmentTask) -> Unit,
    private val onDelete: ((AssignmentTask) -> Unit)? = null,
    private val onClick: ((AssignmentTask) -> Unit)? = null
) : RecyclerView.Adapter<TaskListAdapter.Row>() {

    class Row(view: View) : RecyclerView.ViewHolder(view) {
        val checkBtn: MaterialButton = view.findViewById(R.id.taskCheckBtn)
        val text: TextView = view.findViewById(R.id.taskText)
        val deleteBtn: MaterialButton = view.findViewById(R.id.taskDeleteBtn)
    }

    fun submit(newItems: List<AssignmentTask>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Row {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task, parent, false)
        return Row(view)
    }

    override fun onBindViewHolder(holder: Row, position: Int) {
        val item = items[position]
        val ctx = holder.itemView.context
        holder.text.text = item.text

        // Done = filled check, struck-through + dimmed text. Open = empty circle, cream.
        if (item.isDone) {
            holder.checkBtn.setIconResource(R.drawable.ic_check_circle)
            holder.checkBtn.contentDescription = ctx.getString(R.string.cd_task_done, item.text)
            holder.text.paintFlags = holder.text.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            holder.text.setTextColor(Color.parseColor("#7D8693"))
        } else {
            holder.checkBtn.setIconResource(R.drawable.ic_circle_outline)
            holder.checkBtn.contentDescription = ctx.getString(R.string.cd_task_not_done, item.text)
            holder.text.paintFlags = holder.text.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            holder.text.setTextColor(ctx.getColor(R.color.surface))
        }
        holder.checkBtn.setOnClickListener { onToggle(item) }

        val delete = onDelete
        if (delete != null) {
            holder.deleteBtn.visibility = View.VISIBLE
            holder.deleteBtn.contentDescription = ctx.getString(R.string.cd_delete_task, item.text)
            holder.deleteBtn.setOnClickListener { delete(item) }
        } else {
            holder.deleteBtn.visibility = View.GONE
            holder.deleteBtn.setOnClickListener(null)
        }

        // Tapping the row body (not the checkbox / bin) shows the full text — items
        // are truncated to two lines, so this is how a long one is read in full.
        val click = onClick
        if (click != null) {
            holder.itemView.isClickable = true
            holder.itemView.setOnClickListener { click(item) }
        } else {
            holder.itemView.isClickable = false
            holder.itemView.setOnClickListener(null)
        }
    }

    override fun getItemCount(): Int = items.size
}
