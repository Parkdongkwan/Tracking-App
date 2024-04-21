package eu.fyp.app

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.CalendarView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GestureDetectorCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Locale

class Stats : AppCompatActivity() {

    private lateinit var gestureDetector: GestureDetectorCompat

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stats)

        // Initialize views
        val calendarView = findViewById<CalendarView>(R.id.calendarView)
        val textSelectedDate = findViewById<TextView>(R.id.textSelectedDate)
        val textFoodList = findViewById<TextView>(R.id.textFoodList)

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
        gestureDetector = GestureDetectorCompat(this, SwipeGestureListener())
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event)
    }

    inner class SwipeGestureListener : GestureDetector.SimpleOnGestureListener() {
        private val SWIPE_THRESHOLD = 100
        private val SWIPE_VELOCITY_THRESHOLD = 100

        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            e1 ?: return false
            e2 ?: return false

            val diffY = e2.y - e1.y
            val diffX = e2.x - e1.x

            if (Math.abs(diffX) > Math.abs(diffY)) {
                if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffX > 0) {
                        return true
                    } else {
                        // Swipe left
                        startActivity(Intent(this@Stats, Stats2::class.java))
                        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                        return true
                    }
                }
            }
            return super.onFling(e1, e2, velocityX, velocityY)
        }
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

