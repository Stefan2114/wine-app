package com.example.wine_app.data
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface ApiService {

    @GET("wines")
    suspend fun getWines(): Response<List<Wine>>

    @GET("wines/{id}")
    suspend fun getWineById(@Path("id") id: Int): Response<Wine>

    @POST("wines")
    suspend fun createWine(@Body wine: Wine): Response<Wine>

    @PUT("wines/{id}")
    suspend fun updateWine(@Path("id") id: Int, @Body wine: Wine): Response<Wine>

    @DELETE("wines/{id}")
    suspend fun deleteWine(@Path("id") id: Int): Response<Unit>
}