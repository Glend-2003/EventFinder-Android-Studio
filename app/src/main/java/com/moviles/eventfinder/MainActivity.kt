package com.moviles.eventfinder

import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.moviles.eventfinder.models.Event
import com.moviles.eventfinder.ui.theme.EventFinderTheme
import com.moviles.eventfinder.viewmodel.EventViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            EventFinderTheme {
                val viewModel: EventViewModel = viewModel()
                EventScreen(viewModel)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EventScreenPreview(){
    EventFinderTheme {
        var viewModel: EventViewModel = viewModel()
        EventScreen(viewModel)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventScreen(viewModel: EventViewModel) {
    val events by viewModel.events.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var selectedEvent by remember { mutableStateOf<Event?>(null) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        Log.i("Activity", "Coming here???")
        viewModel.fetchEvents()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Events") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                selectedEvent = null
                showDialog = true
            },
                containerColor = MaterialTheme.colorScheme.secondary) {
                Icon(Icons.Default.Add, contentDescription = "Add Event")
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            Button(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                onClick = { viewModel.fetchEvents() }
            ) {
                Text("Refresh Events")
            }

            Spacer(modifier = Modifier.height(8.dp))

            EventList(events,
                onEdit = { event ->
                    selectedEvent = event
                    showDialog = true
                }, onDelete =
                { event -> viewModel.deleteEvent(event.id) }
            )
        }
    }

    if (showDialog) {
        EventDialog(
            event = selectedEvent,
            onDismiss = { showDialog = false },
            onSave = { event, imageUri ->
                if (event.id == null) {
                    viewModel.addEvent(event, context, imageUri)
                } else {
                    viewModel.updateEvent(event)
                }
                showDialog = false
            },
            context = context
        )
    }
}

@Composable
fun EventList(events: List<Event>, modifier: Modifier = Modifier, onEdit: (Event) -> Unit, onDelete: (Event) -> Unit) {
    LazyColumn(modifier = modifier.padding(16.dp)) {
        items(events) { event ->
            EventItem(event, onEdit, onDelete)
        }
    }
}

@Composable
fun EventItem(event: Event, onEdit: (Event) -> Unit, onDelete: (Event) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        elevation = CardDefaults.elevatedCardElevation(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = event.name, style = MaterialTheme.typography.titleLarge)
            Text(text = event.description, style = MaterialTheme.typography.bodyMedium)
            Text(text = "📍 ${event.location}", style = MaterialTheme.typography.bodySmall)
            Text(text = "📅 ${event.date}", style = MaterialTheme.typography.bodySmall)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = { onEdit(event) }) {
                    Text("Edit", color = MaterialTheme.colorScheme.primary)
                }
                TextButton(onClick = { onDelete(event) }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun EventDialog(
                event: Event?,
                onDismiss: () -> Unit,
                onSave: (Event, Uri?) -> Unit,
                context: android.content.Context
) {
    var name by remember { mutableStateOf(event?.name ?: "") }
    var description by remember { mutableStateOf(event?.description ?: "") }
    var location by remember { mutableStateOf(event?.location ?: "") }
    var selectedDate by remember { mutableStateOf<Long?>(event?.date?.let {
        try {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(it)?.time
        } catch (e: Exception) {
            null
        }
    }) }
    var showDatePicker by remember { mutableStateOf(false) }
    var imageUri by remember { mutableStateOf<Uri?>(event?.image?.let { Uri.parse(it) }) }
    var showImagePicker by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (event == null) "Add Event" else "Edit Event") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Sección de fecha
                Spacer(modifier = Modifier.height(16.dp))

                // Mostrar fecha seleccionada
                if (selectedDate != null) {
                    val formattedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        .format(Date(selectedDate!!))
                    Text("Selected date: $formattedDate")
                } else {
                    Text("No date selected")
                }

                // Botón para fecha
                Button(
                    onClick = { showDatePicker = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text("Select Date")
                }

                // Sección de imagen
                Spacer(modifier = Modifier.height(8.dp))
                Text("Imagen del evento:")

                // Mostrar info de imagen seleccionada
                if (imageUri != null) {
                    AsyncImage(
                        model = imageUri,
                        contentDescription = "Imagen seleccionada",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text("No hay imagen seleccionada")
                }

                // Botón para imagen
                Button(
                    onClick = { showImagePicker = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text("Select Image")
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                // Verificar campos requeridos
                if (name.isBlank() || location.isBlank() || description.isBlank()) {
                    Log.e("EventDialog", "Campos requeridos vacíos")
                    return@Button
                }

                val formattedDate = selectedDate?.let {
                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it))
                } ?: ""

                val newEvent = Event(
                    id = event?.id,
                    name = name.trim(),
                    date = formattedDate,
                    location = location.trim(),
                    description = description.trim(),
                    image = null // El image no lo usaremos en este objeto, lo pasaremos por separado
                )

                onSave(newEvent, imageUri)
            }) {
                Text("Guardar")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )

    // Modales
    if (showDatePicker) {
        DatePickerModal(
            onDateSelected = {
                selectedDate = it
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }

    if (showImagePicker) {
        ImagePickerModal(
            onImageSelected = { uri ->
                imageUri = uri
                showImagePicker = false
            },
            onDismiss = { showImagePicker = false }
        )
    }
   }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerModal(
    onDateSelected: (Long?) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState()

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onDateSelected(datePickerState.selectedDateMillis)
                onDismiss()
            }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

@Composable
fun ImagePickerModal(
    onImageSelected: (Uri?) -> Unit,
    onDismiss: () -> Unit
) {
    val pickMedia = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            Log.d("PhotoPicker", "Selected URI: $uri")
            onImageSelected(uri)
        } else {
            Log.d("PhotoPicker", "No media selected")
            onImageSelected(null)
        }
    }

    // Lanzamos el picker inmediatamente
    LaunchedEffect(Unit) {
        pickMedia.launch(
            PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly)
                .build()
        )
    }
}

