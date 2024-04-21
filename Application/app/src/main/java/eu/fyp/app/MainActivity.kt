package eu.fyp.app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MainActivity : AppCompatActivity() {
    private lateinit var circularGauge: ProgressBar
    private lateinit var calorieText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize views
        circularGauge = findViewById(R.id.circularGauge)
        calorieText = findViewById(R.id.tvEatenCalorie)

        // Initialize buttons
        val trackButton = findViewById<Button>(R.id.trackButton)
        val statsButton = findViewById<Button>(R.id.statsButton)
        val settingsButton = findViewById<Button>(R.id.settingsButton)
        val manageDailyIntakeText = findViewById<TextView>(R.id.tvManageDailyIntake)

        // Retrieve newGoalCalorie data from intent and update circular gauge if available
        val newGoalCalorie = intent.getIntExtra("newGoalCalorie", -1)
        if (newGoalCalorie != -1) {
            circularGauge.max = newGoalCalorie
        }

        // Set click listeners for buttons
        trackButton.setOnClickListener {
            navigateToActivity(Track::class.java)
        }

        statsButton.setOnClickListener {
            navigateToActivity(Stats::class.java)
        }

        settingsButton.setOnClickListener {
            navigateToActivity(Settings::class.java)
        }

        manageDailyIntakeText.setOnClickListener {
            navigateToActivity(ManageDailyIntake::class.java)
        }
    }

    override fun onResume() {
        super.onResume()
        // Fetch user data from Firebase database and update UI
        fetchUserDataFromDatabase { userData ->
            val totalCaloriesForToday = calculateTotalCaloriesForToday(userData.dailyIntakes)
            val userCalorieGoal = userData.userCalorieGoal
            updateCircularGauge(totalCaloriesForToday.toInt(), userCalorieGoal)
            calorieText.text = "$totalCaloriesForToday kcal"
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun updateCircularGauge(caloriesIntake: Int, calorieGoal: Int) {
        // Update circular gauge with calorie intake and goal
        circularGauge.progress = caloriesIntake
        circularGauge.max = calorieGoal
    }

    private fun fetchUserDataFromDatabase(callback: (UserData) -> Unit) {
        // Fetch user data from Firebase database
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        userId?.let { uid ->
            val databaseRef = FirebaseDatabase.getInstance().getReference("users").child(uid)
            databaseRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    // Retrieve user data from database and invoke callback
                    val userData = dataSnapshot.getValue(UserData::class.java)
                    userData?.let {
                        callback(it)
                    } ?: run {
                        showToast("Failed to retrieve user data")
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Handle database error
                    showToast("Database error: ${databaseError.message}")
                }
            })
        } ?: run {
            // Handle case where user is not authenticated
            showToast("User not authenticated")
        }
    }

    private fun calculateTotalCaloriesForToday(dailyIntakes: List<DailyIntakes>): Double {
        // Calculate total calories consumed for today based on daily intakes
        var totalCalories = 0.0
        for (intake in dailyIntakes) {
            val caloriesWithPortion = intake.calories * intake.portion
            totalCalories += caloriesWithPortion
        }
        return totalCalories
    }

    private fun navigateToActivity(activityClass: Class<*>) {
        // Navigate to specified activity
        val intent = Intent(this, activityClass)
        startActivity(intent)
    }
}




