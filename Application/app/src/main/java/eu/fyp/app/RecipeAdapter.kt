package eu.fyp.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class Food(val ndbno: String, val name: String)

class FoodAdapter(private val foods: List<Food>, private val listener: OnFoodClickListener) : RecyclerView.Adapter<FoodAdapter.FoodViewHolder>() {

    // Interface for item click listener
    interface OnFoodClickListener {
        fun onItemClick(food: Food)
    }

    // Create view holder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FoodViewHolder {
        // Inflate the layout for the item view
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.food_item_layout, parent, false)
        return FoodViewHolder(itemView, listener)
    }

    // Bind data to view holder
    override fun onBindViewHolder(holder: FoodViewHolder, position: Int) {
        val food = foods[position]
        holder.bind(food)
    }

    // Get item count
    override fun getItemCount(): Int {
        return foods.size
    }

    // View holder class
    inner class FoodViewHolder(itemView: View, private val listener: OnFoodClickListener) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        init {
            // Set click listener for the item view
            itemView.setOnClickListener(this)
        }

        // Bind data to views
        fun bind(food: Food) {
            itemView.findViewById<TextView>(R.id.foodNameTextView).text = food.name
        }

        // Handle item click event
        override fun onClick(v: View) {
            val position = adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                val food = foods[position]
                listener.onItemClick(food)
            }
        }
    }
}




