package eu.fyp.app

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import eu.fyp.app.databinding.ActivitySignupBinding

class Signup : AppCompatActivity() {
    private lateinit var binding: ActivitySignupBinding
    private lateinit var firebaseDatabase: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase components
        firebaseDatabase = FirebaseDatabase.getInstance()
        databaseReference = firebaseDatabase.reference.child("users")
        auth = FirebaseAuth.getInstance()

        // Set click listener for sign up button
        binding.signUpButton.setOnClickListener{
            val signupUsername = binding.signupUsername.text.toString()
            val signupPassword = binding.signupPassword.text.toString()
            val signupConfirmPassword = binding.signupPassword2.text.toString()

            // Check if username and password are not empty
            if (signupUsername.isNotEmpty() && signupPassword.isNotEmpty()){
                signupUser(signupUsername, signupPassword, signupConfirmPassword)
            } else {
                Toast.makeText(this@Signup, "All fields are mandatory", Toast.LENGTH_SHORT).show()
            }
        }

        // Set click listener for login redirection
        binding.loginRedirect.setOnClickListener{
            startActivity(Intent(this@Signup, Login::class.java))
            finish()
        }
    }

    // Function to sign up a new user
    private fun signupUser(email: String, password: String, confirmPassword: String) {
        // Check if passwords match
        if (password != confirmPassword) {
            Toast.makeText(this@Signup, "Passwords do not match", Toast.LENGTH_SHORT).show()
            return
        }

        // Create user with email and password
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign up success, add user data to database
                    val user = auth.currentUser
                    val userData = UserData(user?.uid, email, password)
                    databaseReference.child(user?.uid ?: "").setValue(userData)
                    Toast.makeText(this@Signup, "Signup Successfully", Toast.LENGTH_SHORT).show()
                    // Show gender selection dialog after successful sign up
                    showGenderSelectionDialog()
                } else {
                    // If sign up fails, display a message to the user.
                    Toast.makeText(this@Signup, "Signup failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    // Function to show the gender selection dialog
    private fun showGenderSelectionDialog() {
        val options = arrayOf("Woman", "Man")

        AlertDialog.Builder(this)
            .setTitle("Select Your Gender")
            .setItems(options) { _, which ->
                val selectedGender = options[which]
                val calorieGoal = if (selectedGender == "Woman") 2000 else 2700
                // Update user's calorie goal in Firebase
                updateUserCalorieGoal(calorieGoal, selectedGender)
            }
            .setCancelable(false)
            .show()
    }

    // Function to update the user's calorie goal in Firebase
    private fun updateUserCalorieGoal(calorieGoal: Int, selectedGender: String) {
        val currentUser: FirebaseUser? = auth.currentUser
        currentUser?.uid?.let { userId ->
            val updates = mapOf(
                "userCalorieGoal" to calorieGoal,
                "gender" to selectedGender
            )
            databaseReference.child(userId).updateChildren(updates)
                .addOnSuccessListener {
                    // Calorie goal and gender updated successfully
                    Toast.makeText(this, "Calorie goal and gender updated successfully", Toast.LENGTH_SHORT).show()
                    // Redirect user to login screen
                    startActivity(Intent(this@Signup, Login::class.java))
                    finish()
                }
                .addOnFailureListener { e ->
                    // Failed to update calorie goal and gender
                    Log.e("Firebase Error", "Failed to update calorie goal and gender: ${e.message}")
                    Toast.makeText(this, "Failed to update calorie goal and gender", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
