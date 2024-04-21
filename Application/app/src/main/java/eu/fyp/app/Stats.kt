package eu.fyp.app

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.CalendarView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Locale

class Stats : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stats)

        // Initialize views
        val calendarView = findViewById<CalendarView>(R.id.calendarView)
        val textSelectedDate = findViewById<TextView>(R.id.textSelectedDate)
        val textFoodList = findViewById<TextView>(R.id.textFoodList)
        val redirectButton = findViewById<Button>(R.id.buttonRedirectToStats2)

        redirectButton.setOnClickListener{
            val intent = Intent(this, Stats2::class.java)
            startActivity(intent)
        }

        // Set the CalendarView to the current date
        calendarView.date = System.currentTimeMillis()

        // Set listener for calendar selection
        calendarView.setOnDateChangeListener { view, year, month, dayOfMonth ->
            // Format the selected date
            val selectedDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
            // Update the text view
            textSelectedDate.text = "Selected Date: $selectedDate"
            // Update the food list
            updateFoodList(selectedDate, textFoodList)
        }
    }

    fun redirectToHome(view: View) {
        // Start the HomeActivity
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }

    private fun updateFoodList(selectedDate: String, textFoodList: TextView) {
        // Get a reference to your Firebase database
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        userId?.let { uid ->
            val databaseRef = FirebaseDatabase.getInstance().getReference("users").child(uid).child("foodIntakes")

            // Convert the selected date to Unix timestamp
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val selectedTimestamp = dateFormat.parse(selectedDate)?.time ?: 0

            // Query the database for food entries with the selected date
            databaseRef.orderByChild("confirmTime").startAt(selectedTimestamp.toDouble()).endAt(selectedTimestamp.toDouble() + 86400000)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        val foodList = mutableListOf<String>()

                        for (snapshot in dataSnapshot.children) {
                            // Retrieve food entry details and format them
                            val foodName = snapshot.child("foodName").getValue(String::class.java)
                            val calories = snapshot.child("calories").getValue(Double::class.java)
                            val portion = snapshot.child("portion").getValue(Double::class.java)

                            if (foodName != null && calories != null && portion != null) {
                                val totalCalories = calories * portion
                                val foodEntry = "$foodName - ${totalCalories.toInt()} calories"
                                foodList.add(foodEntry)
                            }
                        }

                        // Display the food list in the TextView
                        val formattedFoodList = foodList.joinToString("\n")
                        textFoodList.text = if (formattedFoodList.isNotEmpty()) formattedFoodList else "No food entries for this date"
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        // Handle database error
                        Log.e("Firebase Error", "Database read failed: ${databaseError.message}")
                        textFoodList.text = "Error fetching food entries"
                    }
                })
        }
    }
}

