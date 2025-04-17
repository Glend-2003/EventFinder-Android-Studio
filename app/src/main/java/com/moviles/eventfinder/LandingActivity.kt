package com.moviles.eventfinder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.google.firebase.messaging.FirebaseMessaging
import com.moviles.eventfinder.common.Constants.IMAGES_BASE_URL
import com.moviles.eventfinder.models.Event
import com.moviles.eventfinder.ui.theme.EventFinderTheme
import com.moviles.eventfinder.viewmodel.EventViewModel
import kotlinx.coroutines.launch

class LandingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannel(this)
        subscribeToTopic()
        enableEdgeToEdge()
        setContent {
            EventFinderTheme {
                val viewModel: EventViewModel = viewModel()
                val intent = Intent(this, MainActivity::class.java)
                LandingPage(viewModel) { startActivity(intent) }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LandingPage(viewModel: EventViewModel, onNavigate: () -> Unit) {
    val events by viewModel.events.collectAsState()
    val pagerState = rememberPagerState(pageCount = { events.size })
    val coroutineScope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    // Use OnLifecycleEvent to observe lifecycle state
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.fetchEvents()  // Call the fetchEvents method when the activity is resumed
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Events") }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {

            HorizontalPager(state = pagerState) { page ->
                EventCard(event = events[page])
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(events.size) { index ->
                    IconButton(onClick = {
                        coroutineScope.launch { pagerState.animateScrollToPage(index) }
                    }) {
                        Icon(
                            painter = painterResource(id = if (pagerState.currentPage == index) android.R.drawable.presence_online else android.R.drawable.presence_invisible),
                            contentDescription = null
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { onNavigate() },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(text = "Ver todos los eventos")
            }
            Spacer(modifier = Modifier.height(16.dp))

            Text("Eventos destacados", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp))
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp)) {
                items(events.size) { index ->
                    EventCard(event = events[index], modifier = Modifier.padding(end = 8.dp))
                }
            }
        }
    }
}

@Composable
fun EventCard(event: Event, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth().padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            RemoteImage(IMAGES_BASE_URL + event.image)
            Spacer(modifier = Modifier.height(8.dp))
            Text(event.name, style = MaterialTheme.typography.titleLarge)
            Text(text = "📅 ${event.date}", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun RemoteImage(imageUrl: String) {
    AsyncImage(
        model = imageUrl,
        contentDescription = null,
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp), // Set fixed height
        contentScale = ContentScale.Fit // Crop to fit the box
    )
}

fun createNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channelId = "event_reminder_channel"
        val channelName = "Event Reminders"
        val importance = NotificationManager.IMPORTANCE_DEFAULT

        val channel = NotificationChannel(channelId, channelName, importance).apply {
            description = "Notifies users about upcoming events"
        }

        val notificationManager =
            context.getSystemService(NotificationManager::class.java)
        notificationManager?.createNotificationChannel(channel)
    }
}

fun subscribeToTopic() {
    FirebaseMessaging.getInstance().subscribeToTopic("event_notifications")
        .addOnCompleteListener { task ->
            var msg = "Subscription successful"
            if (!task.isSuccessful) {
                msg = "Subscription failed"
            }
            Log.d("FCM", msg)
        }
}