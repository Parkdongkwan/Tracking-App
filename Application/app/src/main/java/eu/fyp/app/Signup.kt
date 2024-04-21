package eu.fyp.app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
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
                    startActivity(Intent(this@Signup, Login::class.java))
                    finish()
                } else {
                    // If sign up fails, display a message to the user.
                    Toast.makeText(this@Signup, "Signup failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
}


