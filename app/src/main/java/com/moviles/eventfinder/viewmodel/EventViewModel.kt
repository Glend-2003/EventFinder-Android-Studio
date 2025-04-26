package com.moviles.eventfinder.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.moviles.eventfinder.data.local.EventDatabase
import com.moviles.eventfinder.models.Event
import com.moviles.eventfinder.network.RetrofitClient
import com.moviles.eventfinder.repository.EventRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class EventViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: EventRepository

    private val _events = MutableStateFlow<List<Event>>(emptyList())
    val events: StateFlow<List<Event>> = _events

    // Estado de carga y origen de datos
    val isLoading: StateFlow<Boolean>
    val dataFromCache: StateFlow<Boolean>

    init {
        val database = EventDatabase.getDatabase(application)
        val eventDao = database.eventDao()
        val apiService = RetrofitClient.apiService
        repository = EventRepository(eventDao, apiService, application)

        // Observar los eventos del repositorio
        viewModelScope.launch {
            repository.getAllEvents().collect { eventList ->
                _events.value = eventList
            }
        }

        isLoading = repository.isLoading
        dataFromCache = repository.dataFromCache
    }

    fun fetchEvents() {
        viewModelScope.launch {
            repository.syncEvents()
        }
    }

    fun addEvent(event: Event, context: Context, imageUri: Uri?) {
        viewModelScope.launch {
            try {
                repository.addEvent(event, imageUri)
            } catch (e: Exception) {
                Log.e("EventViewModel", "Error adding event", e)
            }
        }
    }

    fun updateEvent(event: Event) {
        viewModelScope.launch {
            try {
                repository.updateEvent(event)
            } catch (e: Exception) {
                Log.e("EventViewModel", "Error updating event", e)
            }
        }
    }

    fun deleteEvent(id: Int?) {
        viewModelScope.launch {
            try {
                repository.deleteEvent(id)
            } catch (e: Exception) {
                Log.e("EventViewModel", "Error deleting event", e)
            }
        }
    }

    // Factory para crear el ViewModel con Application
    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(EventViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return EventViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}