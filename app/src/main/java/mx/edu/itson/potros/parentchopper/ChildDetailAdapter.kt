package mx.edu.itson.potros.parentchopper

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import mx.edu.itson.potros.parentchopper.models.ChildDetail

class ChildDetailAdapter(
    private val details: List<ChildDetail>
) : RecyclerView.Adapter<ChildDetailAdapter.DetailViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DetailViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_detail_card, parent, false)
        return DetailViewHolder(view)
    }

    override fun onBindViewHolder(holder: DetailViewHolder, position: Int) {
        val detail = details[position]
        holder.bind(detail)
    }

    override fun getItemCount(): Int = details.size

    class DetailViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val typeText: TextView = itemView.findViewById(R.id.detailTypeText)
        private val subjectText: TextView = itemView.findViewById(R.id.detailSubjectText)
        private val descriptionText: TextView = itemView.findViewById(R.id.detailDescription)

        fun bind(detail: ChildDetail) {
            typeText.text = detail.type
            subjectText.text = detail.subject
            descriptionText.text = detail.detailedInfo  // <-- cambiar aquí

            itemView.setOnClickListener {
                val context = itemView.context
                val intent = Intent(context, DetailItemActivity::class.java).apply {
                    putExtra("type", detail.type)
                    putExtra("subject", detail.subject)
                    putExtra("detailedInfo", detail.detailedInfo)  // <-- cambiar aquí
                    putExtra("detailId", detail.id)  // si quieres pasar el id también
                }
                context.startActivity(intent)
            }
        }
    }
}
