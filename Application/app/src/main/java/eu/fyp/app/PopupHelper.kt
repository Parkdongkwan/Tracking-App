package eu.fyp.app

import FoodNutritionResponse
import FoodSearchResponse
import android.app.Activity
import android.content.Context
import android.text.InputType
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


// Helper class for displaying a popup window to show food details
object PopupHelper {

    // Function to show the popup window
    fun showPopupWindow(activity: Activity, anchorView: View, maxIdx: Int) {
        // Inflating the popup layout
        val inflater = activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupView = inflater.inflate(R.layout.popup_layout, null)

        // Setting up the popup window
        val width = LinearLayout.LayoutParams.MATCH_PARENT
        val height = LinearLayout.LayoutParams.WRAP_CONTENT
        val focusable = true
        val popupWindow = PopupWindow(popupView, width, height, focusable)

        // Setting up the RecyclerView for displaying food details
        val recyclerView = popupView.findViewById<RecyclerView>(R.id.foodsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(activity)

        // Reading labels from assets to get food name
        val labels = activity.assets.open("labels.txt").bufferedReader().readLines()
        val foodName = labels[maxIdx].replace("_", " ")

        // Fetching food details from the API
        fetchFoodDetails(
            activity,
            foodName,
            "1dff5mOKwq4Ns3PJ7lG73VtEoaWl6t156ViOLLW6",
            recyclerView
        )

        // Showing the popup window
        popupWindow.showAtLocation(anchorView, Gravity.CENTER, 0, 0)
    }

    // Function to fetch food details from the API
    private fun fetchFoodDetails(
        activity: Activity,
        foodName: String,
        apiKey: String,
        recyclerView: RecyclerView
    ) {
        // Making API call to fetch food details
        USDARetrofitClient.instance.searchFoods(foodName, apiKey, 1, 20)
            .enqueue(object : Callback<FoodSearchResponse> {
                override fun onResponse(
                    call: Call<FoodSearchResponse>,
                    response: Response<FoodSearchResponse>
                ) {
                    if (response.isSuccessful) {
                        val foods = response.body()?.foods
                        if (foods.isNullOrEmpty()) {
                            // Showing a toast if no data available for the food
                            Toast.makeText(
                                activity,
                                "No $foodName data available",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            // Displaying the fetched food details in RecyclerView
                            foods?.let {
                                val foodList = mutableListOf<Food>()
                                for (food in it) {
                                    foodList.add(Food(food.fdcId, food.description))
                                }
                                val adapter =
                                    FoodAdapter(foodList, object : FoodAdapter.OnFoodClickListener {
                                        override fun onItemClick(food: Food) {
                                            // Showing nutritional info dialog on food item click
                                            showFoodNutritionalInfoDialog(
                                                activity,
                                                food.ndbno,
                                                apiKey,
                                                food.name
                                            )
                                        }
                                    })
                                recyclerView.adapter = adapter
                                recyclerView.layoutManager = LinearLayoutManager(activity)
                            }
                        }
                    } else {
                        // Handling API error
                        Log.e("API Error", "Response Code: ${response.code()}")
                        Toast.makeText(
                            activity,
                            "Failed to fetch food details!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<FoodSearchResponse>, t: Throwable) {
                    // Handling API call failure
                    Log.e("API Failure", t.message ?: "Unknown Error")
                    Toast.makeText(activity, "Failed to fetch food details!", Toast.LENGTH_SHORT)
                        .show()
                }
            })
    }


    // Function to show a dialog with nutritional information and record food intake
    private fun showFoodNutritionalInfoDialog(
        activity: Activity,
        fdcId: String,
        apiKey: String,
        foodName: String
    ) {
        // Making API call to fetch food nutrition details
        USDARetrofitClient.instance.getFoodNutrition(fdcId, apiKey)
            .enqueue(object : Callback<FoodNutritionResponse> {
                override fun onResponse(
                    call: Call<FoodNutritionResponse>,
                    response: Response<FoodNutritionResponse>
                ) {
                    if (response.isSuccessful) {
                        val foodNutrientResponse = response.body()
                        foodNutrientResponse?.let { nutrientResponse ->
                            val nutrients = nutrientResponse.foodNutrients
                            // Filtering relevant nutrients
                            val relevantNutrients = nutrients.filter { it.type == "FoodNutrient" }

                            // Building AlertDialog to display nutritional information
                            val dialogBuilder = AlertDialog.Builder(activity)
                            dialogBuilder.setTitle("Nutritional Information")

                            val details = StringBuilder()
                            details.append("Food Name: $foodName\n")

                            // List of desired nutrients to display
                            val desiredNutrients = setOf(
                                "Carbohydrate, by difference",
                                "Total lipid (fat)",
                                "Protein",
                                "Energy"
                            )

                            // Appending relevant nutrient details to the dialog
                            relevantNutrients.forEach { nutrient ->
                                if (desiredNutrients.contains(nutrient.nutrient.name)) {
                                    details.append("${nutrient.nutrient.name}: ${nutrient.amount} ${nutrient.nutrient.unitName}\n")
                                }
                            }

                            // Adding an input field for portion value
                            val input = EditText(activity)
                            input.inputType =
                                InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                            input.hint = "Enter portion value (1 portion is 100g)"
                            dialogBuilder.setView(input)

                            dialogBuilder.setMessage(details.toString())

                            // Confirm button click listener
                            dialogBuilder.setPositiveButton("Confirm") { dialog, _ ->
                                val portionValue = input.text.toString().toDoubleOrNull() ?: 1.0
                                // Updating database with food intake
                                updateDatabaseWithFoodIntake(
                                    activity,
                                    nutrientResponse,
                                    foodName,
                                    portionValue
                                )
                                dialog.dismiss()
                            }

                            // Cancel button click listener
                            dialogBuilder.setNegativeButton("Cancel") { dialog, _ ->
                                dialog.cancel()
                            }

                            // Creating and displaying the dialog
                            val dialog = dialogBuilder.create()
                            dialog.show()
                        }
                    } else {
                        // Handling API error
                        Log.e("API Error", "Response Code: ${response.code()}")
                        Toast.makeText(
                            activity,
                            "Failed to fetch nutritional information!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<FoodNutritionResponse>, t: Throwable) {
                    // Handling API call failure
                    Log.e("API Failure", t.message ?: "Unknown Error")
                    Toast.makeText(
                        activity,
                        "Failed to fetch nutritional information!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    // Function to update database with recorded food intake
    private fun updateDatabaseWithFoodIntake(
        activity: Activity,
        nutrition: FoodNutritionResponse,
        foodName: String,
        portionValue: Double
    ) {
        // Getting reference to Firebase Database
        val database = FirebaseDatabase.getInstance().reference
        // Getting current time in milliseconds
        val currentTimeMillis = System.currentTimeMillis()

        val foodNutrients = nutrition.foodNutrients

        var calories = 0.0
        var fat = 0.0
        var carbohydrates = 0.0
        var protein = 0.0

        // Extracting relevant nutrient values
        for (nutrient in foodNutrients) {
            when (nutrient.nutrient.name.toLowerCase()) {
                "energy" -> {
                    if (nutrient.nutrient.unitName.equals("kcal", ignoreCase = true)) {
                        calories = nutrient.amount.toDouble()
                    }
                }

                "total lipid (fat)" -> fat = nutrient.amount.toDouble()
                "carbohydrate, by difference" -> carbohydrates = nutrient.amount.toDouble()
                "protein" -> protein = nutrient.amount.toDouble()
            }
        }

        // Creating FoodIntake object
        val foodIntake = FoodIntake(
            foodName = foodName,
            calories = calories,
            fat = fat,
            carbohydrates = carbohydrates,
            protein = protein,
            confirmTime = currentTimeMillis,
            portion = portionValue
        )

        // Creating WeeklyIntake object
        val weeklyIntake = WeeklyIntake(
            foodName = foodName,
            calories = calories,
            fat = fat,
            carbohydrates = carbohydrates,
            protein = protein,
            confirmTime = currentTimeMillis,
            portion = portionValue
        )

        // Creating DailyIntakes object
        val dailyIntake = DailyIntakes(
            foodName = foodName,
            calories = calories,
            fat = fat,
            carbohydrates = carbohydrates,
            protein = protein,
            confirmTime = currentTimeMillis,
            portion = portionValue
        )

        // Getting user ID from Firebase Authentication
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        userId?.let { uid ->
            // Getting reference to user's data in Firebase Database
            val userReference = FirebaseDatabase.getInstance().getReference("users").child(uid)
            // Adding listener to fetch existing user data
            userReference.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val existingUserData = dataSnapshot.getValue(UserData::class.java)
                    existingUserData?.let { userData ->
                        // Adding food intake to user's data
                        userData.foodIntakes.add(foodIntake)
                        userData.weeklyIntake.add(weeklyIntake)
                        userData.dailyIntakes.add(dailyIntake)

                        // Updating user's data in Firebase Database
                        userReference.setValue(userData)
                            .addOnSuccessListener {
                                Toast.makeText(
                                    activity,
                                    "Food intake recorded successfully!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            .addOnFailureListener { exception ->
                                Log.e(
                                    "Firebase Error",
                                    "Error updating database: ${exception.message}"
                                )
                                Toast.makeText(
                                    activity,
                                    "Failed to record food intake!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.e("Firebase Error", "Listener cancelled: ${databaseError.message}")
                    Toast.makeText(activity, "Failed to record food intake!", Toast.LENGTH_SHORT)
                        .show()
                }
            })
        }
    }
}

