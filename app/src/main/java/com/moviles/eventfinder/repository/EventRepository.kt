package com.moviles.eventfinder.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.moviles.eventfinder.common.NetworkUtils
import com.moviles.eventfinder.data.local.EventDao
import com.moviles.eventfinder.models.Event
import com.moviles.eventfinder.network.ApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream

class EventRepository(
    private val eventDao: EventDao,
    private val apiService: ApiService,
    private val context: Context
) {
    private val TAG = "EventRepository"

    // Estado de carga
    private var isLoadingFromNetwork = false
    private val _isLoadingState = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoadingState

    // Estado de origen de datos
    private val _dataFromCache = MutableStateFlow(false)
    val dataFromCache: StateFlow<Boolean> = _dataFromCache

    // Obtener todos los eventos (desde local)
    fun getAllEvents(): Flow<List<Event>> {
        return eventDao.getAllEvents()
    }

    // Sincronizar eventos con la API
    suspend fun syncEvents() {
        if (isLoadingFromNetwork) return

        isLoadingFromNetwork = true
        _isLoadingState.value = true

        try {
            if (NetworkUtils.isNetworkAvailable(context)) {
                // Marcar eventos existentes como de caché
                eventDao.updateCacheStatus(true)

                // Obtener eventos desde la API
                val remoteEvents = apiService.getEvents()

                // Actualizar eventos en la base de datos local
                val currentTime = System.currentTimeMillis()
                val localEvents = remoteEvents.map {
                    it.copy(isFromCache = false, lastSyncTimestamp = currentTime)
                }

                eventDao.insertEvents(localEvents)
                _dataFromCache.value = false

                Log.d(TAG, "Eventos actualizados desde la API: ${remoteEvents.size}")
            } else {
                _dataFromCache.value = true
                Log.d(TAG, "Sin conexión, usando datos de caché")
            }
        } catch (e: Exception) {
            _dataFromCache.value = true
            Log.e(TAG, "Error actualizando eventos", e)
        } finally {
            isLoadingFromNetwork = false
            _isLoadingState.value = false
        }
    }

    // Añadir un nuevo evento
    suspend fun addEvent(event: Event, imageUri: Uri?): Result<Event> {
        return try {
            if (NetworkUtils.isNetworkAvailable(context) && imageUri != null) {
                // Si hay conexión, primero subir a la API
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
                val newEvent = apiService.addEvent(namePart, descriptionPart, locationPart, datePart, filePart)

                // Guardar también en la base de datos local
                eventDao.insertEvent(newEvent.copy(isFromCache = false, lastSyncTimestamp = System.currentTimeMillis()))

                Result.success(newEvent)
            } else {
                // Si no hay conexión o imagen, guardar solo localmente (pendiente de sincronización)
                val localEvent = event.copy(isFromCache = true)
                val id = eventDao.insertEvent(localEvent)
                Result.success(localEvent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error adding event", e)
            Result.failure(e)
        }
    }

    // Actualizar un evento existente
    suspend fun updateEvent(event: Event): Result<Event> {
        return try {
            if (NetworkUtils.isNetworkAvailable(context)) {
                // Si hay conexión, actualizar en la API
                val updatedEvent = apiService.updateEvent(event.id, event)

                // Actualizar también en la base de datos local
                eventDao.updateEvent(updatedEvent.copy(isFromCache = false, lastSyncTimestamp = System.currentTimeMillis()))

                Result.success(updatedEvent)
            } else {
                // Si no hay conexión, actualizar solo localmente
                val localEvent = event.copy(isFromCache = true)
                eventDao.updateEvent(localEvent)
                Result.success(localEvent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating event", e)
            Result.failure(e)
        }
    }

    // Eliminar un evento
    suspend fun deleteEvent(id: Int?): Result<Unit> {
        return try {
            if (NetworkUtils.isNetworkAvailable(context)) {
                // Si hay conexión, eliminar de la API
                apiService.deleteEvent(id)
            }

            // Siempre eliminar de la base de datos local
            eventDao.deleteEvent(id)

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting event", e)
            Result.failure(e)
        }
    }

    // Obtener detalles de un evento
    suspend fun getEventById(eventId: Int?): Event? {
        return eventDao.getEventById(eventId)
    }

    // Función auxiliar para convertir URI a File
    private suspend fun uriToFile(context: Context, uri: Uri): File = withContext(Dispatchers.IO) {
        val inputStream = context.contentResolver.openInputStream(uri)
        val fileName = "image_${System.currentTimeMillis()}.jpg"
        val tempFile = File(context.cacheDir, fileName)

        inputStream?.use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        }

        tempFile
    }
}