package eu.fyp.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class DailyIntakeAdapter(private var dailyIntakes: List<DailyIntakes>, private val listener: OnItemClickListener) :
    RecyclerView.Adapter<DailyIntakeAdapter.DailyIntakeViewHolder>() {

    // Interface for item click listener
    interface OnItemClickListener {
        fun onItemClick(position: Int)
    }

    inner class DailyIntakeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val foodNameTextView: TextView = itemView.findViewById(R.id.foodNameTextView)
        private val caloriesTextView: TextView = itemView.findViewById(R.id.caloriesTextView)
        private val portionTextView: TextView = itemView.findViewById(R.id.portionTextView)

        // Bind data to views and set click listener
        fun bind(dailyIntake: DailyIntakes) {
            foodNameTextView.text = "Food Name: ${dailyIntake.foodName}"
            caloriesTextView.text = "Calories: ${dailyIntake.calories}"
            portionTextView.text = "Portion: ${dailyIntake.portion}"
            itemView.setOnClickListener {
                listener.onItemClick(adapterPosition)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DailyIntakeViewHolder {
        // Inflate layout for each item view
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_daily_intake, parent, false)
        return DailyIntakeViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: DailyIntakeViewHolder, position: Int) {
        // Bind data to views for each item
        holder.bind(dailyIntakes[position])
    }

    override fun getItemCount(): Int {
        // Return the total number of items
        return dailyIntakes.size
    }

    // Update data and notify adapter
    fun setData(data: List<DailyIntakes>) {
        dailyIntakes = data
        notifyDataSetChanged()
    }

    // Get daily intake at specific position
    fun getDailyIntakeAtPosition(position: Int): DailyIntakes {
        return dailyIntakes[position]
    }
}
