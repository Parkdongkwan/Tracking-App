package eu.fyp.app

import USDAService
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object USDARetrofitClient {
    private const val BASE_URL = "https://api.nal.usda.gov/fdc/v1/"

    val instance: USDAService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        retrofit.create(USDAService::class.java)
    }
}