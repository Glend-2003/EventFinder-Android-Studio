package com.moviles.eventfinder.models

data class Event(
    val id: Int?,
    val name: String,
    val date: String,
    val location: String,
    val description: String,
    val image: String?
)
