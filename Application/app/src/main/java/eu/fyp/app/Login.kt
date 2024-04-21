package eu.fyp.app

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import eu.fyp.app.databinding.ActivityLoginBinding

class Login : AppCompatActivity() {
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var binding: ActivityLoginBinding
    private lateinit var firebaseDatabase: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("MyAppPreference", Context.MODE_PRIVATE)
        firebaseDatabase = FirebaseDatabase.getInstance()
        databaseReference = firebaseDatabase.reference.child("users")

        // Set click listener for login button
        binding.loginButton.setOnClickListener {
            val loginUsername = binding.loginUsername.text.toString()
            val loginPassword = binding.loginPassword.text.toString()

            // Check if username and password are not empty
            if (loginUsername.isNotEmpty() && loginPassword.isNotEmpty()) {
                loginUser(loginUsername, loginPassword)
            } else {
                Toast.makeText(this@Login, "All Fields are mandatory", Toast.LENGTH_SHORT).show()
            }
        }

        // Set click listener for signup redirection
        binding.signupRedirect.setOnClickListener {
            startActivity(Intent(this@Login, Signup::class.java))
            finish()
        }
    }

    // Function to authenticate user login
    private fun loginUser(username: String, password: String) {
        val auth = FirebaseAuth.getInstance()

        auth.signInWithEmailAndPassword(username, password)
            .addOnCompleteListener(this@Login) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    val user = auth.currentUser
                    Toast.makeText(this@Login, "Login Successfully", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@Login, MainActivity::class.java))
                    finish()
                } else {
                    // If sign in fails, display a message to the user.
                    Toast.makeText(this@Login, "Login Failed", Toast.LENGTH_SHORT).show()
                }
            }
    }
}
