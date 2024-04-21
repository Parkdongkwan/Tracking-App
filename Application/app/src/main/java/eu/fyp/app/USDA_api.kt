import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

data class FoodSearchResponse(
    val foods: List<FoodResult>
)

data class FoodResult(
    val fdcId: String, // FDC ID instead of ndbno
    val description: String // Description instead of name
)

data class FoodNutritionResponse(
    val foodNutrients: List<FoodNutrient>
)

data class FoodNutrient(
    val nutrient: Nutrient,
    val type: String,
    val amount: Double
)

data class Nutrient(
    val nutrientId: String,
    val name: String,
    val unitName: String
)

interface USDAService {
    @GET("foods/search")
    fun searchFoods(
        @Query("query") query: String,
        @Query("api_key") apiKey: String,
        @Query("pageNumber") pageNumber: Int, // Specify the page number
        @Query("pageSize") pageSize: Int // Specify the number of results per page
    ): Call<FoodSearchResponse>

    @GET("food/{fdcId}")
    fun getFoodNutrition(
        @Path("fdcId") fdcId: String,
        @Query("api_key") apiKey: String
    ): Call<FoodNutritionResponse>
}