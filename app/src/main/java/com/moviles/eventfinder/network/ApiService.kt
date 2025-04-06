package com.moviles.eventfinder.network
import com.moviles.eventfinder.models.Event
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface ApiService {
    @GET("api/event")
    suspend fun getEvents(): List<Event>

    @POST("api/event")
    suspend fun addEvent(@Body eventDto: Event): Event

    @PUT("api/event/{id}")
    suspend fun updateEvent(@Path("id") id: Int?, @Body eventDto: Event): Event

    @DELETE("api/event/{id}")
    suspend fun deleteEvent(@Path("id") id: Int?): Response<Unit>


}