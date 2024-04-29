package eu.fyp.app

data class UserData(
    val id: String? = null,
    val username: String? = null,
    val password: String? = null,
    val foodIntakes: MutableList<FoodIntake> = mutableListOf(),
    val weeklyIntake: MutableList<WeeklyIntake> = mutableListOf(),
    var dailyIntakes: MutableList<DailyIntakes> = mutableListOf(),
    var userCalorieGoal: Int = 0, // Default value for user's calorie goal
    val gender: String? = null,
)

data class DailyIntakes(
    val foodName: String = "",
    val calories: Double = 0.0,
    val fat: Double = 0.0,
    val carbohydrates: Double = 0.0,
    val protein: Double = 0.0,
    val confirmTime: Long = 0,
    val portion: Double = 0.0
)

data class WeeklyIntake(
    val foodName: String = "",
    val calories: Double = 0.0,
    val fat: Double = 0.0,
    val carbohydrates: Double = 0.0,
    val protein: Double = 0.0,
    val confirmTime: Long = 0,
    val portion: Double = 0.0
)


data class FoodIntake(
    val foodName: String,
    val calories: Double,
    val fat: Double,
    val carbohydrates: Double,
    val protein: Double,
    val confirmTime: Long,
    val portion: Double // New field for portion value
) {
    // No-argument constructor required by Firebase for deserialization
    constructor() : this("", 0.0, 0.0, 0.0, 0.0, 0,0.0)
}