package com.example.dramastory

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.ViewGroup
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

data class Episode(val id: String = "", val title: String = "", val description: String = "",
                   val videoUrl: String = "", val thumbnailUrl: String = "", val published: Boolean = false)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try { FirebaseApp.initializeApp(this) } catch (_: Exception) {}
        setContent { DramaApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DramaApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }
    var screen by remember { mutableStateOf("Home") }
    var episodes by remember { mutableStateOf(listOf<Episode>()) }
    var selected by remember { mutableStateOf<Episode?>(null) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var signedIn by remember { mutableStateOf(auth.currentUser?.email.orEmpty()) }
    var message by remember { mutableStateOf("Add licensed episodes in Firebase Firestore to get started.") }

    fun loadEpisodes() {
        db.collection("episodes").whereEqualTo("published", true).get()
            .addOnSuccessListener { s ->
                episodes = s.documents.mapNotNull { d -> d.toObject(Episode::class.java)?.copy(id = d.id) }
                message = if (episodes.isEmpty()) "No published episodes yet." else ""
            }.addOnFailureListener { message = "Could not load episodes. Check Firebase setup." }
    }
    LaunchedEffect(Unit) { loadEpisodes() }

    MaterialTheme {
        Scaffold(
            topBar = { TopAppBar(title = { Text("Drama Story") }, actions = {
                TextButton(onClick = { screen = "Home" }) { Text("Home") }
                TextButton(onClick = { screen = "Account" }) { Text(if (signedIn.isBlank()) "Login" else "Account") }
                TextButton(onClick = { screen = "Admin" }) { Text("Admin") }
            }) },
            bottomBar = {
                AndroidView(factory = { c -> AdView(c).apply {
                    setAdSize(AdSize.BANNER)
                    adUnitId = "ca-app-pub-3940256099942544/6300978111" // test ID only
                    loadAd(AdRequest.Builder().build())
                } }, modifier = Modifier.fillMaxWidth().height(50.dp))
            }
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                when (screen) {
                    "Home" -> {
                        Text("Animated drama episodes", style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { loadEpisodes() }) { Text("Refresh") }
                            OutlinedButton(onClick = { screen = "Account" }) { Text("Login / Sign up") }
                        }
                        Text(message)
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(episodes) { ep ->
                                Card(Modifier.fillMaxWidth(), onClick = { selected = ep; screen = "Player" }) {
                                    Column(Modifier.padding(14.dp)) {
                                        Text(ep.title, style = MaterialTheme.typography.titleMedium)
                                        Text(ep.description)
                                        Text("Tap to watch")
                                    }
                                }
                            }
                        }
                    }
                    "Account" -> {
                        Text("Login / Sign up", style = MaterialTheme.typography.headlineSmall)
                        if (signedIn.isNotBlank()) {
                            Text("Signed in: $signedIn")
                            Button(onClick = { auth.signOut(); signedIn = ""; message = "Signed out" }) { Text("Sign out") }
                        } else {
                            OutlinedTextField(email, { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(password, { password = it }, label = { Text("Password") }, modifier = Modifier.fillMaxWidth())
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { auth.signInWithEmailAndPassword(email.trim(), password)
                                    .addOnSuccessListener { signedIn = auth.currentUser?.email.orEmpty(); screen = "Home"; message = "Login successful" }
                                    .addOnFailureListener { message = it.localizedMessage ?: "Login failed" } }) { Text("Login") }
                                OutlinedButton(onClick = { auth.createUserWithEmailAndPassword(email.trim(), password)
                                    .addOnSuccessListener { signedIn = auth.currentUser?.email.orEmpty(); screen = "Home"; message = "Account created" }
                                    .addOnFailureListener { message = it.localizedMessage ?: "Sign up failed" } }) { Text("Sign up") }
                            }
                        }
                        Text(message)
                    }
                    "Player" -> {
                        val ep = selected
                        if (ep == null || ep.videoUrl.isBlank()) Text("No video URL configured for this episode.")
                        else {
                            Text(ep.title, style = MaterialTheme.typography.headlineSmall)
                            Spacer(Modifier.height(8.dp))
                            AndroidView(factory = { c ->
                                VideoView(c).apply {
                                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 650)
                                    setMediaController(MediaController(c))
                                    setVideoURI(Uri.parse(ep.videoUrl))
                                    setOnPreparedListener { it.isLooping = false; start() }
                                }
                            }, modifier = Modifier.fillMaxWidth().height(230.dp))
                            Text(ep.description)
                            Spacer(Modifier.height(8.dp))
                            Button(onClick = { downloadEpisode(context, ep) }) { Text("Download episode") }
                            Text("Download only if the content licence permits offline copies.")
                        }
                    }
                    "Admin" -> {
                        Text("Admin panel", style = MaterialTheme.typography.headlineSmall)
                        Text("Admin upload is not enabled in this starter. Add a trusted backend or Firebase Cloud Function that checks an admin custom claim before uploading videos and episode metadata.")
                        Text("Do not make uploads public to every signed-in user.")
                    }
                }
            }
        }
    }
}

fun downloadEpisode(context: Context, ep: Episode) {
    if (ep.videoUrl.isBlank()) return
    val req = DownloadManager.Request(Uri.parse(ep.videoUrl))
        .setTitle(ep.title.ifBlank { "Drama Story episode" })
        .setDescription("Downloading episode")
        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "DramaStory_${ep.id}.mp4")
        .setAllowedOverMetered(true).setAllowedOverRoaming(false)
    (context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(req)
}
