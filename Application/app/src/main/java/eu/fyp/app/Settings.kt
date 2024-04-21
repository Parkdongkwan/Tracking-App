package eu.fyp.app

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import eu.fyp.app.databinding.ActivitySettingsBinding

class Settings : AppCompatActivity() {

    private lateinit var settingsBinding: ActivitySettingsBinding
    private lateinit var database: DatabaseReference
    private lateinit var userId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase Realtime Database reference
        database = FirebaseDatabase.getInstance().reference

        // Retrieve the current user's ID from Firebase Authentication
        userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        // Inflate and bind settings.xml layout
        settingsBinding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(settingsBinding.root)

        // Initialize views and set up listeners
        setupUI()
    }

    private fun setupUI() {
        // Access UI components from settings.xml layout
        val goalCalorieEditText = settingsBinding.editGoalCalorie
        val btnSaveGoalCalorie = settingsBinding.btnSaveGoalCalorie
        val textGoalCalorie = settingsBinding.textGoalCalorie
        val btnLogout = settingsBinding.btnLogout

        // Retrieve and display user information
        retrieveUserInfoAndUpdateUI()

        // Retrieve user's calorie goal from database and display it
        database.child("users").child(userId).child("userCalorieGoal")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val userCalorieGoal = snapshot.getValue(Int::class.java) ?: 2500
                    textGoalCalorie.text = "Daily Goal Calorie: $userCalorieGoal"
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle database error
                    Log.e("Firebase Error", "Database read failed: ${error.message}")
                    Toast.makeText(this@Settings, "Failed to retrieve user calorie goal", Toast.LENGTH_SHORT).show()
                }
            })

        // Set up listener for save button
        btnSaveGoalCalorie.setOnClickListener {
            val newGoalCalorie = goalCalorieEditText.text.toString().toIntOrNull() ?: return@setOnClickListener
            updateGoalCalorie(newGoalCalorie)
        }

        // Set up Listener for logout button
        btnLogout.setOnClickListener{
            logout()
        }
    }

    private fun updateGoalCalorie(newGoalCalorie: Int) {
        // Update user's calorie goal in the database
        database.child("users").child(userId).child("userCalorieGoal").setValue(newGoalCalorie)
            .addOnSuccessListener {
                // Notify user and navigate to MainActivity upon successful update
                Toast.makeText(this@Settings, "Calorie goal updated successfully", Toast.LENGTH_SHORT).show()
                val intent = Intent(this@Settings, MainActivity::class.java)
                intent.putExtra("newGoalCalorie", newGoalCalorie)
                startActivity(intent)
                finish()
            }
            .addOnFailureListener {
                // Notify user if update fails
                Toast.makeText(this@Settings, "Failed to update calorie goal", Toast.LENGTH_SHORT).show()
                Log.e("Firebase Error", "Failed to update calorie goal: ${it.message}")
            }
    }

    private fun retrieveUserInfoAndUpdateUI() {
        // Retrieve user's information from database and display it
        database.child("users").child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val userData = snapshot.getValue(UserData::class.java)
                val username = userData?.username ?: "Unknown"
                settingsBinding.textUserInformation.text = "User: $username"
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle database error
                Log.e("Firebase Error", "Database read failed: ${error.message}")
                Toast.makeText(this@Settings, "Failed to retrieve user information", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun logout() {
        // Perform logout action
        FirebaseAuth.getInstance().signOut()
        // Navigate to the login screen or any other desired destination
        startActivity(Intent(this, Login::class.java))
        finish()
    }
}







