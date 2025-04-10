package com.moviles.eventfinder.viewmodel
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moviles.eventfinder.models.Event
import com.moviles.eventfinder.network.RetrofitInstance
import com.moviles.eventfinder.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import android.content.Context
import android.net.Uri
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream

class EventViewModel : ViewModel() {
    private val apiService = RetrofitClient.apiService
    private val _events = MutableStateFlow<List<Event>>(emptyList())
    val events: StateFlow<List<Event>> = _events

    fun fetchEvents() {
        viewModelScope.launch {
            try {
                val response = apiService.getEvents()
                _events.value = response
            } catch (e: Exception) {
                Log.e("EventViewModel", "Error fetching events", e)
            }
        }
    }

    fun addEvent(event: Event, context: Context, imageUri: Uri?) {
        viewModelScope.launch {
            try {
                if (imageUri != null) {
                    // Convertir la URI a un archivo
                    val imageFile = uriToFile(context, imageUri)

                    // Crear los RequestBody para los campos de texto
                    val namePart = event.name.toRequestBody("text/plain".toMediaTypeOrNull())
                    val descriptionPart = event.description.toRequestBody("text/plain".toMediaTypeOrNull())
                    val locationPart = event.location.toRequestBody("text/plain".toMediaTypeOrNull())
                    val datePart = event.date.toRequestBody("text/plain".toMediaTypeOrNull())

                    // Crear el MultipartBody.Part para el archivo
                    val requestFile = imageFile.asRequestBody("image/*".toMediaTypeOrNull())
                    val filePart = MultipartBody.Part.createFormData("File", imageFile.name, requestFile)

                    // Enviar la solicitud
                    val response = apiService.addEvent(namePart, descriptionPart, locationPart, datePart, filePart)

                    // Actualizar la lista de eventos
                    fetchEvents()
                } else {
                    Log.e("EventViewModel", "No se proporcionó una imagen")
                }
            } catch (e: Exception) {
                Log.e("EventViewModel", "Error adding event", e)
            }
        }
    }

    fun updateEvent(event: Event) {
        viewModelScope.launch {
            try {
                apiService.updateEvent(event.id, event)
                fetchEvents()
            } catch (e: Exception) {
                Log.e("EventViewModel", "Error updating event", e)
            }
        }
    }

    fun deleteEvent(id: Int?) {
        viewModelScope.launch {
            try {
                apiService.deleteEvent(id)
                fetchEvents()
            } catch (e: Exception) {
                Log.e("EventViewModel", "Error deleting event", e)
            }
        }
    }

    // Función auxiliar para convertir URI a File
    private fun uriToFile(context: Context, uri: Uri): File {
        val inputStream = context.contentResolver.openInputStream(uri)
        val fileName = "image_${System.currentTimeMillis()}.jpg"
        val tempFile = File(context.cacheDir, fileName)

        inputStream?.use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        }

        return tempFile
    }
}