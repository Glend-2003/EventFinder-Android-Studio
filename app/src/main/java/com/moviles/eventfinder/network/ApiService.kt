package com.moviles.eventfinder.network

import com.moviles.eventfinder.models.Event
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    @GET("api/event")
    suspend fun getEvents(): List<Event>

    @Multipart
    @POST("api/event")
    suspend fun addEvent(
        @Part("Name") name: RequestBody,
        @Part("Description") description: RequestBody,
        @Part("Location") location: RequestBody,
        @Part("Date") date: RequestBody,
        @Part file: MultipartBody.Part
    ): Event

    @PUT("api/event/{id}")
    suspend fun updateEvent(@Path("id") id: Int?, @Body eventDto: Event): Event

    @DELETE("api/event/{id}")
    suspend fun deleteEvent(@Path("id") id: Int?): Response<Unit>
}