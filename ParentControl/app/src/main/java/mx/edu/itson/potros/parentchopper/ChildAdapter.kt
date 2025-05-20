package mx.edu.itson.potros.parentchopper

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import mx.edu.itson.potros.parentchopper.models.Child

class ChildAdapter(
    private val children: List<Child>,
    private val onClick: (Child) -> Unit
) : RecyclerView.Adapter<ChildAdapter.ChildViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChildViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_child_card, parent, false)
        return ChildViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChildViewHolder, position: Int) {
        val child = children[position]
        holder.bind(child)
        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, ChildDetailActivity::class.java).apply {
                putExtra("name", child.name)
                putExtra("photoUrl", child.photoUrl)
                putExtra("childId", child.id)
            }
            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = children.size

    class ChildViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val childNameTextView: TextView = itemView.findViewById(R.id.childNameTextView)
        private val childPhotoImageView: ImageView = itemView.findViewById(R.id.childPhotoImageView)

        fun bind(child: Child) {
            childNameTextView.text = child.name
            Glide.with(itemView.context)
                .load(child.photoUrl)
                .circleCrop()
                .into(childPhotoImageView)
        }
    }
}