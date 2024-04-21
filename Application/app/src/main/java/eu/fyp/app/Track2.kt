package eu.fyp.app

import FoodNutritionResponse
import FoodSearchResponse
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import eu.fyp.app.databinding.ActivityTrack2Binding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class Track2 : AppCompatActivity(), FoodAdapter.OnFoodClickListener {
    private lateinit var binding: ActivityTrack2Binding
    private val apiKey = "1dff5mOKwq4Ns3PJ7lG73VtEoaWl6t156ViOLLW6"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTrack2Binding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set click listener for the search button
        binding.buttonSearch.setOnClickListener {
            // Get the entered food name
            val foodName = binding.editTextFoodName.text.toString().trim()

            // Check if the entered food name is not empty
            if (foodName.isNotEmpty()) {
                // Display the food name in a toast message for testing
                Toast.makeText(this, "Food Name: $foodName", Toast.LENGTH_SHORT).show()

                // Fetch food details and nutrition information
                fetchFoodDetails(foodName, apiKey)
            } else {
                // If the entered food name is empty, show a toast indicating so
                Toast.makeText(this, "Please enter a food name", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fetchFoodDetails(foodName: String, apiKey: String) {
        USDARetrofitClient.instance.searchFoods(foodName, apiKey, 3, 5)
            .enqueue(object : Callback<FoodSearchResponse> {
                override fun onResponse(call: Call<FoodSearchResponse>, response: Response<FoodSearchResponse>) {
                    if (response.isSuccessful) {
                        val foods = response.body()?.foods
                        foods?.let {
                            val foodList = mutableListOf<Food>()
                            for (food in it) {
                                Log.d("Food", "Food Name: ${food.description}, NDBNO: ${food.fdcId}")
                                foodList.add(Food(food.fdcId, food.description))
                            }
                            // Initialize and set up the RecyclerView adapter
                            val adapter = FoodAdapter(foodList, this@Track2)
                            binding.recipesRecyclerView.adapter = adapter
                            binding.recipesRecyclerView.layoutManager = LinearLayoutManager(this@Track2)
                        }
                    } else {
                        Log.e("API Error", "Response Code: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<FoodSearchResponse>, t: Throwable) {
                    // Log or handle API call failure
                    Log.e("API Failure", t.message ?: "Unknown Error")
                }
            })
    }

    override fun onItemClick(food: Food) {
        // Handle item click here
        showFoodNutritionalInfoDialog(food.ndbno, apiKey, food.name)
    }

    private fun showFoodNutritionalInfoDialog(ndbno: String, apiKey: String, foodName: String) {
        val inputLayout = LinearLayout(this@Track2)
        inputLayout.orientation = LinearLayout.VERTICAL

        val portionEditText = EditText(this@Track2)
        portionEditText.hint = "Enter portion value"
        portionEditText.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        inputLayout.addView(portionEditText)

        val dialogBuilder = AlertDialog.Builder(this@Track2)
        dialogBuilder.setTitle("Nutritional Information")
        dialogBuilder.setView(inputLayout)

        USDARetrofitClient.instance.getFoodNutrition(ndbno, apiKey)
            .enqueue(object : Callback<FoodNutritionResponse> {
                override fun onResponse(call: Call<FoodNutritionResponse>, response: Response<FoodNutritionResponse>) {
                    if (response.isSuccessful) {
                        val foodNutritionResponse = response.body()
                        foodNutritionResponse?.let { nutritionResponse ->
                            val nutrients = nutritionResponse.foodNutrients

                            // Filter out the relevant nutrients
                            val relevantNutrients = nutrients.filter {
                                it.type == "FoodNutrient" && it.nutrient.name.toLowerCase() in listOf(
                                    "energy",
                                    "total lipid (fat)",
                                    "carbohydrate, by difference",
                                    "protein"
                                )
                            }

                            val details = StringBuilder()
                            details.append("Food Name: $foodName\n")

                            // Construct details string with relevant nutrients
                            relevantNutrients.forEach { nutrient ->
                                details.append("${nutrient.nutrient.name}: ${nutrient.amount} ${nutrient.nutrient.unitName}\n")
                            }

                            dialogBuilder.setMessage(details.toString().trim())

                            // Set positive button
                            dialogBuilder.setPositiveButton("Confirm") { dialog, _ ->
                                // Get portion value from EditText
                                val portionValue = portionEditText.text.toString().toDoubleOrNull() ?: 1.0
                                // Update database with food intake
                                updateDatabaseWithFoodIntake(nutritionResponse, foodName, portionValue)
                                dialog.dismiss()
                            }

                            // Set negative button for canceling
                            dialogBuilder.setNegativeButton("Cancel") { dialog, _ ->
                                dialog.cancel()
                            }

                            // Create and show the dialog
                            val dialog = dialogBuilder.create()
                            dialog.show()
                        }
                    } else {
                        Log.e("API Error", "Response Code: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<FoodNutritionResponse>, t: Throwable) {
                    // Handle API call failure
                    Log.e("API Failure", t.message ?: "Unknown Error")
                }
            })
    }

    private fun updateDatabaseWithFoodIntake(nutrition: FoodNutritionResponse, foodName: String, portionValue: Double) {
        val database = FirebaseDatabase.getInstance().reference
        val currentTimeMillis = System.currentTimeMillis()

        val nutrients = nutrition.foodNutrients

        var calories = 0.0
        var fat = 0.0
        var carbohydrates = 0.0
        var protein = 0.0

        // Extract the relevant nutrients
        for (nutrient in nutrients) {
            when (nutrient.nutrient.name.toLowerCase()) {
                "energy" -> {
                    // Check if the unit is kcal
                    if (nutrient.nutrient.unitName.equals("kcal", ignoreCase = true)) {
                        // Convert to kcal and assign to calories
                        calories = nutrient.amount.toDouble()
                    }
                }
                "total lipid (fat)" -> fat = nutrient.amount.toDouble()
                "carbohydrate, by difference" -> carbohydrates = nutrient.amount.toDouble()
                "protein" -> protein = nutrient.amount.toDouble()
            }
        }

        val foodIntake = FoodIntake(
            foodName = foodName,
            calories = calories,
            fat = fat,
            carbohydrates = carbohydrates,
            protein = protein,
            confirmTime = currentTimeMillis,
            portion = portionValue // Add portion value
        )

        val weeklyIntake = WeeklyIntake(
            foodName = foodName,
            calories = calories,
            fat = fat,
            carbohydrates = carbohydrates,
            protein = protein,
            confirmTime = currentTimeMillis,
            portion = portionValue // Add portion value
        )

        val dailyIntake = DailyIntakes(
            foodName = foodName,
            calories = calories,
            fat = fat,
            carbohydrates = carbohydrates,
            protein = protein,
            confirmTime = currentTimeMillis,
            portion = portionValue // Add portion value
        )

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        userId?.let { uid ->
            val userReference = FirebaseDatabase.getInstance().getReference("users").child(uid)
            userReference.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val existingUserData = dataSnapshot.getValue(UserData::class.java)
                    existingUserData?.let { userData ->
                        userData.foodIntakes.add(foodIntake)
                        userData.weeklyIntake.add(weeklyIntake)
                        userData.dailyIntakes.add(dailyIntake)

                        userReference.setValue(userData)
                            .addOnSuccessListener {
                                Toast.makeText(this@Track2, "Food intake recorded successfully!", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener { exception ->
                                Toast.makeText(this@Track2, "Failed to record food intake!", Toast.LENGTH_SHORT).show()
                            }
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.e("Firebase Error", "Listener cancelled: ${databaseError.message}")
                }
            })
        }
    }

}







