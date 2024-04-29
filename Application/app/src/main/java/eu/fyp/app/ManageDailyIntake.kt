package eu.fyp.app

import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import eu.fyp.app.databinding.ActivityManageDailyIntakeBinding

class ManageDailyIntake : AppCompatActivity() {
    private lateinit var binding: ActivityManageDailyIntakeBinding
    private lateinit var dailyIntakeAdapter: DailyIntakeAdapter
    private lateinit var database: DatabaseReference
    private var currentUser: FirebaseUser? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageDailyIntakeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase components
        database = FirebaseDatabase.getInstance().reference
        currentUser = FirebaseAuth.getInstance().currentUser

        // Setup RecyclerView
        setupRecyclerView()

        // Fetch daily intakes from Firebase
        fetchDailyIntakes()
    }

    // Setup RecyclerView
    private fun setupRecyclerView() {
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        dailyIntakeAdapter = DailyIntakeAdapter(emptyList(), object : DailyIntakeAdapter.OnItemClickListener {
            override fun onItemClick(position: Int) {
                showEditDeleteDialog(position)
            }
        })
        binding.recyclerView.adapter = dailyIntakeAdapter
    }

    // Fetch daily intakes from Firebase
// Fetch daily intakes from Firebase
    private fun fetchDailyIntakes() {
        currentUser?.uid?.let { userId ->
            val userDailyIntakeRef: DatabaseReference = database.child("users").child(userId).child("dailyIntakes")

            userDailyIntakeRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val dailyIntakesList = mutableListOf<DailyIntakes>()
                    for (snapshot in dataSnapshot.children) {
                        val dailyIntake = snapshot.getValue(DailyIntakes::class.java)
                        dailyIntake?.let { dailyIntakesList.add(it) }
                    }
                    if (dailyIntakesList.isEmpty()) {
                        // If no daily intakes, set adapter with a single item
                        dailyIntakeAdapter.setData(listOf(DailyIntakes("No Intake Today")))
                    } else {
                        // Set adapter with fetched daily intakes
                        dailyIntakeAdapter.setData(dailyIntakesList)
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.e("Firebase Error", "Fetching daily intakes cancelled: ${databaseError.message}")
                    Toast.makeText(this@ManageDailyIntake, "Error fetching daily intakes", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }


    // Show edit and delete options dialog
    private fun showEditDeleteDialog(position: Int) {
        val dailyIntake = dailyIntakeAdapter.getDailyIntakeAtPosition(position)

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Options")
        builder.setItems(arrayOf("Edit Portion", "Delete")) { dialog, which ->
            when (which) {
                0 -> showEditPortionDialog(dailyIntake)
                1 -> deleteDailyIntakeFromDatabase(dailyIntake)
            }
        }
        builder.create().show()
    }

    // Show dialog for editing portion
    private fun showEditPortionDialog(dailyIntake: DailyIntakes) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Edit Portion")

        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        input.hint = "Enter new portion value"
        input.setText(dailyIntake.portion.toString()) // Set the current portion value as default
        input.setSelection(input.text.length) // Move cursor to the end
        builder.setView(input)

        builder.setPositiveButton("OK") { _, _ ->
            val newPortion = input.text.toString().toDoubleOrNull()
            if (newPortion != null && newPortion > 0) {
                // Update the portion value in the database
                updatePortionValueInDatabase(dailyIntake, newPortion)
            } else {
                // Show error message for invalid portion value
                Toast.makeText(this, "Invalid portion value. Please enter a positive number.", Toast.LENGTH_SHORT).show()
            }
        }

        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }

        builder.show()
    }

    // Delete daily intake from database
    private fun deleteDailyIntakeFromDatabase(dailyIntake: DailyIntakes) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        userId?.let { uid ->
            val userReference = FirebaseDatabase.getInstance().getReference("users").child(uid)
            userReference.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val existingUserData = dataSnapshot.getValue(UserData::class.java)
                    existingUserData?.let { userData ->
                        // Find and remove the DailyIntake from dailyIntakes list
                        val dailyIntakeIndex = userData.dailyIntakes.indexOfFirst { it.confirmTime == dailyIntake.confirmTime }
                        if (dailyIntakeIndex != -1) {
                            userData.dailyIntakes.removeAt(dailyIntakeIndex)

                            // Find and remove the corresponding FoodIntake from foodIntakes list
                            val foodIntakeIndex = userData.foodIntakes.indexOfFirst { it.confirmTime == dailyIntake.confirmTime }
                            if (foodIntakeIndex != -1) {
                                userData.foodIntakes.removeAt(foodIntakeIndex)
                            }

                            // Find and remove the corresponding WeeklyIntake from weeklyIntakes list
                            val weeklyIntakeIndex = userData.weeklyIntake.indexOfFirst { it.confirmTime == dailyIntake.confirmTime }
                            if (weeklyIntakeIndex != -1) {
                                userData.weeklyIntake.removeAt(weeklyIntakeIndex)
                            }

                            // Update the database with the modified UserData
                            userReference.setValue(userData)
                                .addOnSuccessListener {
                                    Toast.makeText(this@ManageDailyIntake, "Daily intake deleted successfully", Toast.LENGTH_SHORT).show()
                                }
                                .addOnFailureListener { exception ->
                                    Log.e("Firebase Error", "Deleting daily intake failed: ${exception.message}")
                                    Toast.makeText(this@ManageDailyIntake, "Error deleting daily intake", Toast.LENGTH_SHORT).show()
                                }
                        }
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.e("Firebase Error", "Listener cancelled: ${databaseError.message}")
                    Toast.makeText(this@ManageDailyIntake, "Error deleting daily intake", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    // Update portion value in database
    private fun updatePortionValueInDatabase(dailyIntake: DailyIntakes, newPortion: Double) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        userId?.let { uid ->
            val userReference = FirebaseDatabase.getInstance().getReference("users").child(uid)
            userReference.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val existingUserData = dataSnapshot.getValue(UserData::class.java)
                    existingUserData?.let { userData ->
                        // Update portion value for daily intake
                        val updatedDailyIntakes = userData.dailyIntakes.map {
                            if (it.confirmTime == dailyIntake.confirmTime) {
                                it.copy(portion = newPortion)
                            } else {
                                it
                            }
                        }

                        // Update portion value for food intake
                        val updatedFoodIntakes = userData.foodIntakes.map {
                            if (it.confirmTime == dailyIntake.confirmTime) {
                                it.copy(portion = newPortion)
                            } else {
                                it
                            }
                        }

                        // Update portion value for weekly intake
                        val updatedWeeklyIntakes = userData.weeklyIntake.map {
                            if (it.confirmTime == dailyIntake.confirmTime) {
                                it.copy(portion = newPortion)
                            } else {
                                it
                            }
                        }

                        // Update userData with the new lists
                        val updatedUserData = userData.copy(
                            dailyIntakes = updatedDailyIntakes.toMutableList(),
                            foodIntakes = updatedFoodIntakes.toMutableList(),
                            weeklyIntake = updatedWeeklyIntakes.toMutableList()
                        )

                        // Save the updated data to the database
                        userReference.setValue(updatedUserData)
                            .addOnSuccessListener {
                                Toast.makeText(this@ManageDailyIntake, "Portion value updated successfully", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener { exception ->
                                Log.e("Firebase Error", "Updating portion value failed: ${exception.message}")
                                Toast.makeText(this@ManageDailyIntake, "Error updating portion value", Toast.LENGTH_SHORT).show()
                            }
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.e("Firebase Error", "Listener cancelled: ${databaseError.message}")
                    Toast.makeText(this@ManageDailyIntake, "Error updating portion value", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }
}
