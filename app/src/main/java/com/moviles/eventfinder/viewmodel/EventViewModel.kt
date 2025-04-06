package com.moviles.eventfinder.viewmodel
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moviles.eventfinder.models.Event
import com.moviles.eventfinder.network.RetrofitInstance
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

class EventViewModel : ViewModel() {

    private val _events = MutableStateFlow<List<Event>>(emptyList())
    val events: StateFlow<List<Event>> get() = _events

    fun fetchEvents() {
        viewModelScope.launch {
            try {
                _events.value = RetrofitInstance.api.getEvents()
                Log.i("MyViewModel", "Fetching data from API... ${_events.value}")
            } catch (e: Exception) {
                Log.e("ViewmodelError", "Error: ${e}")
            }
        }
    }

    fun addEvent(event: Event){
        viewModelScope.launch {
            try {
                Log.i("ViewModelInfo", "Event: ${event}")
                val response = RetrofitInstance.api.addEvent(event)
                _events.value += response
                Log.i("ViewModelInfo", "Response: ${response}")
            } catch (e: HttpException) {
                val errorBody = e.response()?.errorBody()?.string()
                Log.e("ViewModelError", "HTTP Error: ${e.message()}, Response Body: $errorBody")
            } catch (e: Exception) {
                Log.e("ViewModelError", "Error: ${e.message}", e)
            }
        }
    }

    fun updateEvent(event: Event){
        viewModelScope.launch {
            try {
                Log.i("ViewModelInfo", "Event: ${event}")
                val response = RetrofitInstance.api.updateEvent(event.id, event)
                _events.value = _events.value.map { event ->
                    if (event.id == response.id) response else event
                }
                Log.i("ViewModelInfo", "Response: ${response}")
            } catch (e: HttpException) {
                val errorBody = e.response()?.errorBody()?.string()
                Log.e("ViewModelError", "HTTP Error: ${e.message()}, Response Body: $errorBody")
            } catch (e: Exception) {
                Log.e("ViewModelError", "Error: ${e.message}", e)
            }
        }
    }

    fun deleteEvent(eventId: Int?) {
        eventId?.let { id ->
            viewModelScope.launch {
                try {
                    RetrofitInstance.api.deleteEvent(id)
                    _events.value = _events.value.filter { it.id != eventId }
                } catch (e: Exception) {
                    Log.e("ViewModelError", "Error deleting event: ${e.message}")
                }
            }
        } ?: Log.e("ViewModelError", "Error: eventId is null")
    }
}