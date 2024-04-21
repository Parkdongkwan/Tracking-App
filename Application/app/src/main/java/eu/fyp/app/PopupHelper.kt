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


object PopupHelper {

    fun showPopupWindow(activity: Activity, anchorView: View, maxIdx: Int) {
        val inflater = activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupView = inflater.inflate(R.layout.popup_layout, null)

        val width = LinearLayout.LayoutParams.MATCH_PARENT
        val height = LinearLayout.LayoutParams.WRAP_CONTENT
        val focusable = true
        val popupWindow = PopupWindow(popupView, width, height, focusable)

        val recyclerView = popupView.findViewById<RecyclerView>(R.id.foodsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(activity)

        val labels = activity.assets.open("labels.txt").bufferedReader().readLines()
        val foodName = labels[maxIdx]
        fetchFoodDetails(activity, foodName, "1dff5mOKwq4Ns3PJ7lG73VtEoaWl6t156ViOLLW6", recyclerView)

        popupWindow.showAtLocation(anchorView, Gravity.CENTER, 0, 0)
    }

    private fun fetchFoodDetails(activity: Activity, foodName: String, apiKey: String, recyclerView: RecyclerView) {
        USDARetrofitClient.instance.searchFoods(foodName, apiKey, 5, 5)
            .enqueue(object : Callback<FoodSearchResponse> {
                override fun onResponse(call: Call<FoodSearchResponse>, response: Response<FoodSearchResponse>) {
                    if (response.isSuccessful) {
                        val foods = response.body()?.foods
                        foods?.let {
                            val foodList = mutableListOf<Food>()
                            for (food in it) {
                                foodList.add(Food(food.fdcId, food.description))
                            }
                            val adapter = FoodAdapter(foodList, object : FoodAdapter.OnFoodClickListener {
                                override fun onItemClick(food: Food) {
                                    showFoodNutritionalInfoDialog(activity, food.ndbno, apiKey, food.name)
                                }
                            })
                            recyclerView.adapter = adapter
                        }
                    } else {
                        Log.e("API Error", "Response Code: ${response.code()}")
                        Toast.makeText(activity, "Failed to fetch food details!", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<FoodSearchResponse>, t: Throwable) {
                    Log.e("API Failure", t.message ?: "Unknown Error")
                    Toast.makeText(activity, "Failed to fetch food details!", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun showFoodNutritionalInfoDialog(activity: Activity, fdcId: String, apiKey: String, foodName: String) {
        USDARetrofitClient.instance.getFoodNutrition(fdcId, apiKey)
            .enqueue(object : Callback<FoodNutritionResponse> {
                override fun onResponse(call: Call<FoodNutritionResponse>, response: Response<FoodNutritionResponse>) {
                    if (response.isSuccessful) {
                        val foodNutrientResponse = response.body()
                        foodNutrientResponse?.let { nutrientResponse ->
                            val nutrients = nutrientResponse.foodNutrients
                            val relevantNutrients = nutrients.filter { it.type == "FoodNutrient" }

                            val dialogBuilder = AlertDialog.Builder(activity)
                            dialogBuilder.setTitle("Nutritional Information")

                            val details = StringBuilder()
                            details.append("Food Name: $foodName\n")

                            val desiredNutrients = setOf("Carbohydrate, by difference", "Total lipid (fat)", "Protein", "Energy")

                            relevantNutrients.forEach { nutrient ->
                                if (desiredNutrients.contains(nutrient.nutrient.name)) {
                                    details.append("${nutrient.nutrient.name}: ${nutrient.amount} ${nutrient.nutrient.unitName}\n")
                                }
                            }

                            val input = EditText(activity)
                            input.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                            input.hint = "Enter portion value"
                            dialogBuilder.setView(input)

                            dialogBuilder.setMessage(details.toString())

                            dialogBuilder.setPositiveButton("Confirm") { dialog, _ ->
                                val portionValue = input.text.toString().toDoubleOrNull() ?: 1.0
                                updateDatabaseWithFoodIntake(activity, nutrientResponse, foodName, portionValue)
                                dialog.dismiss()
                            }

                            dialogBuilder.setNegativeButton("Cancel") { dialog, _ ->
                                dialog.cancel()
                            }

                            val dialog = dialogBuilder.create()
                            dialog.show()
                        }
                    } else {
                        Log.e("API Error", "Response Code: ${response.code()}")
                        Toast.makeText(activity, "Failed to fetch nutritional information!", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<FoodNutritionResponse>, t: Throwable) {
                    Log.e("API Failure", t.message ?: "Unknown Error")
                    Toast.makeText(activity, "Failed to fetch nutritional information!", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun updateDatabaseWithFoodIntake(activity: Activity, nutrition: FoodNutritionResponse, foodName: String, portionValue: Double) {
        val database = FirebaseDatabase.getInstance().reference
        val currentTimeMillis = System.currentTimeMillis()

        val foodNutrients = nutrition.foodNutrients

        var calories = 0.0
        var fat = 0.0
        var carbohydrates = 0.0
        var protein = 0.0

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

        val foodIntake = FoodIntake(
            foodName = foodName,
            calories = calories,
            fat = fat,
            carbohydrates = carbohydrates,
            protein = protein,
            confirmTime = currentTimeMillis,
            portion = portionValue
        )

        val weeklyIntake = WeeklyIntake(
            foodName = foodName,
            calories = calories,
            fat = fat,
            carbohydrates = carbohydrates,
            protein = protein,
            confirmTime = currentTimeMillis,
            portion = portionValue
        )

        val dailyIntake = DailyIntakes(
            foodName = foodName,
            calories = calories,
            fat = fat,
            carbohydrates = carbohydrates,
            protein = protein,
            confirmTime = currentTimeMillis,
            portion = portionValue
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
                                Toast.makeText(activity, "Food intake recorded successfully!", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener { exception ->
                                Log.e("Firebase Error", "Error updating database: ${exception.message}")
                                Toast.makeText(activity, "Failed to record food intake!", Toast.LENGTH_SHORT).show()
                            }
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.e("Firebase Error", "Listener cancelled: ${databaseError.message}")
                    Toast.makeText(activity, "Failed to record food intake!", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }
}


