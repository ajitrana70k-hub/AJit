package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.MediaStorageHelper
import com.example.ui.components.formatDuration
import com.example.ui.theme.StudioCrimson
import com.example.ui.theme.StudioEmerald
import com.example.ui.viewmodel.MyVideoViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadAndCameraScreen(
    viewModel: MyVideoViewModel,
    initialIsShort: Boolean = false,
    onBack: () -> Unit,
    onOpenYouTubeConnect: () -> Unit,
    onUploadFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentChannel by viewModel.currentChannel.collectAsStateWithLifecycle()
    val uploadQueue by viewModel.uploadQueue.collectAsStateWithLifecycle()
    val recordedUri by viewModel.recordedVideoUri.collectAsStateWithLifecycle()

    var showCameraRecorder by remember { mutableStateOf(false) }
    var selectedVideoUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var fromLocalRecording by remember { mutableStateOf(false) }
    var customThumbnailUri by remember { mutableStateOf<Uri?>(null) }
    var isShort by remember { mutableStateOf(initialIsShort) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var hashtags by remember { mutableStateOf(if (initialIsShort) "#shorts #myvideo" else "#myvideo") }
    var tags by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Tech") }
    var visibility by remember { mutableStateOf("PUBLIC") }
    var publishToYouTube by remember { mutableStateOf(false) }

    LaunchedEffect(recordedUri) {
        if (recordedUri != null) {
            selectedVideoUris = listOf(recordedUri!!)
            fromLocalRecording = true
            viewModel.setRecordedVideoUri(null)
        }
    }

    val singleVideoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedVideoUris = listOf(uri)
            fromLocalRecording = false
        }
    }

    val multiVideoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 5)
    ) { uris ->
        if (uris.isNotEmpty()) {
            selectedVideoUris = uris
            fromLocalRecording = false
        }
    }

    val thumbnailPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) customThumbnailUri = uri
    }

    var cameraPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val camOk = grants[Manifest.permission.CAMERA] == true
        val audOk = grants[Manifest.permission.RECORD_AUDIO] == true
        cameraPermissionGranted = camOk && audOk
        if (cameraPermissionGranted) {
            showCameraRecorder = true
        } else {
            viewModel.showMessage("Camera and Microphone permissions are required to record videos.")
        }
    }

    if (showCameraRecorder) {
        CameraRecorderOverlay(
            onClose = { showCameraRecorder = false },
            onRecorded = { fileUri ->
                viewModel.setRecordedVideoUri(fileUri)
                showCameraRecorder = false
            },
            onError = { msg ->
                viewModel.showMessage(msg)
                showCameraRecorder = false
            }
        )
        return
    }

    BackHandler(onBack = onBack)

    val categories = listOf("Tech", "Cinema", "Music", "Gaming", "Education", "Vlog", "Comedy")
    val visibilities = listOf("PUBLIC", "UNLISTED", "PRIVATE")
    val ytAuthorizedForUpload = currentChannel?.youtubeConnected == true &&
        currentChannel?.youtubeScopes?.contains("youtube.upload") == true

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Create & Upload") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Active Upload / Transcoding Progress Queue
            uploadQueue.forEach { item ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(item.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("${item.progressPercent}%", style = MaterialTheme.typography.labelLarge)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { item.progressPercent / 100f },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(item.stageLabel, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            // Video Source Picker Actions (Gallery Single, Gallery Multiple, Record CameraX)
            Text("1. Select or Record Video", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ElevatedCard(
                    onClick = {
                        singleVideoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                        )
                    },
                    modifier = Modifier.weight(1f).testTag("pick_gallery_video_btn")
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Outlined.VideoLibrary, null, tint = StudioCrimson, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Gallery", style = MaterialTheme.typography.labelLarge)
                        Text("Pick video", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                ElevatedCard(
                    onClick = {
                        multiVideoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                        )
                    },
                    modifier = Modifier.weight(1f).testTag("pick_multi_video_btn")
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Outlined.LibraryAdd, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Batch", style = MaterialTheme.typography.labelLarge)
                        Text("Multi-upload", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                ElevatedCard(
                    onClick = {
                        if (cameraPermissionGranted) {
                            showCameraRecorder = true
                        } else {
                            cameraPermissionLauncher.launch(
                                arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
                            )
                        }
                    },
                    modifier = Modifier.weight(1f).testTag("record_camera_btn")
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Filled.Videocam, null, tint = StudioEmerald, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Record", style = MaterialTheme.typography.labelLarge)
                        Text("Camera", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            if (selectedVideoUris.isNotEmpty()) {
                Surface(
                    color = StudioEmerald.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.CheckCircle, null, tint = StudioEmerald)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (selectedVideoUris.size == 1) {
                                "1 video ready (${selectedVideoUris.first().lastPathSegment ?: "media"})"
                            } else {
                                "${selectedVideoUris.size} videos queued for batch upload"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { selectedVideoUris = emptyList() }) {
                            Icon(Icons.Filled.Close, "Clear selection")
                        }
                    }
                }
            }

            // Video Format Toggle (Long Video vs Short)
            Text("2. Format & Thumbnail", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = !isShort,
                    onClick = { isShort = false },
                    shape = SegmentedButtonDefaults.itemShape(0, 2),
                    label = { Text("Long Video (16:9)") }
                )
                SegmentedButton(
                    selected = isShort,
                    onClick = { isShort = true },
                    shape = SegmentedButtonDefaults.itemShape(1, 2),
                    label = { Text("Short (9:16)") }
                )
            }

            // Custom Thumbnail Picker
            OutlinedCard(
                onClick = {
                    thumbnailPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                modifier = Modifier.fillMaxWidth().testTag("pick_thumbnail_btn")
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (customThumbnailUri != null) {
                        AsyncImage(
                            model = customThumbnailUri,
                            contentDescription = "Custom thumbnail",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(width = 88.dp, height = 50.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(width = 88.dp, height = 50.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.AddPhotoAlternate, null)
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (customThumbnailUri != null) "Custom Thumbnail Selected" else "Upload Custom Thumbnail",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "Optional — auto-extracts a video frame if omitted",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Metadata Fields
            Text("3. Video Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("upload_title_input")
            )
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                minLines = 3,
                maxLines = 5,
                modifier = Modifier.fillMaxWidth().testTag("upload_desc_input")
            )
            OutlinedTextField(
                value = hashtags,
                onValueChange = { hashtags = it },
                label = { Text("Hashtags (e.g. #tech #filmmaking)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = tags,
                onValueChange = { tags = it },
                label = { Text("Search Tags (comma-separated)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Category Chips
            Text("Category", style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.take(4).forEach { cat ->
                    FilterChip(
                        selected = category == cat,
                        onClick = { category = cat },
                        label = { Text(cat) }
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.drop(4).forEach { cat ->
                    FilterChip(
                        selected = category == cat,
                        onClick = { category = cat },
                        label = { Text(cat) }
                    )
                }
            }

            // Visibility Selector
            Text("Visibility", style = MaterialTheme.typography.labelLarge)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                visibilities.forEachIndexed { idx, vis ->
                    SegmentedButton(
                        selected = visibility == vis,
                        onClick = { visibility = vis },
                        shape = SegmentedButtonDefaults.itemShape(idx, visibilities.size),
                        label = { Text(vis.lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
            }

            // Separate YouTube Cross-Upload Authorization Gate
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "YouTube Cross-Publishing (Separate Auth)",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                if (ytAuthorizedForUpload) {
                                    "Authorized for ${currentChannel?.youtubeChannelTitle}. Opt-in per upload."
                                } else {
                                    "Videos publish only to MY VIDEO unless you separately authorize YouTube upload scope."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (ytAuthorizedForUpload) {
                            Switch(
                                checked = publishToYouTube,
                                onCheckedChange = { publishToYouTube = it }
                            )
                        }
                    }
                    if (!ytAuthorizedForUpload) {
                        Spacer(modifier = Modifier.height(6.dp))
                        TextButton(onClick = onOpenYouTubeConnect) {
                            Text("Authorize YouTube Account")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Button(
                onClick = {
                    viewModel.publishVideos(
                        sourceUris = selectedVideoUris,
                        isLocalFileAlready = fromLocalRecording,
                        customThumbnailSourceUri = customThumbnailUri,
                        title = title,
                        description = description,
                        hashtags = hashtags,
                        tags = tags,
                        category = category,
                        visibility = visibility,
                        isShort = isShort,
                        publishToYouTube = publishToYouTube && ytAuthorizedForUpload
                    ) { ok ->
                        if (ok) onUploadFinished()
                    }
                },
                enabled = selectedVideoUris.isNotEmpty() && title.isNotBlank() && uploadQueue.isEmpty(),
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("publish_video_btn")
            ) {
                Icon(Icons.Filled.CloudUpload, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (selectedVideoUris.size > 1) "Publish ${selectedVideoUris.size} Videos"
                    else "Process & Publish"
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun CameraRecorderOverlay(
    onClose: () -> Unit,
    onRecorded: (Uri) -> Unit,
    onError: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var useFrontCamera by remember { mutableStateOf(false) }
    var activeRecording by remember { mutableStateOf<Recording?>(null) }
    var isRecording by remember { mutableStateOf(false) }
    var elapsedSec by remember { mutableLongStateOf(0L) }
    var videoCapture by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            elapsedSec = 0L
            while (isRecording) {
                delay(1000L)
                elapsedSec++
            }
        }
    }

    BackHandler {
        activeRecording?.stop()
        onClose()
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                }
            },
            update = { previewView ->
                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                cameraProviderFuture.addListener({
                    try {
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }
                        val recorder = Recorder.Builder()
                            .setQualitySelector(QualitySelector.from(Quality.HD))
                            .build()
                        val vc = VideoCapture.withOutput(recorder)
                        videoCapture = vc

                        val selector = if (useFrontCamera) {
                            CameraSelector.DEFAULT_FRONT_CAMERA
                        } else {
                            CameraSelector.DEFAULT_BACK_CAMERA
                        }
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview, vc)
                    } catch (e: Exception) {
                        onError("Camera unavailable on this device: ${e.message}")
                    }
                }, ContextCompat.getMainExecutor(context))
            },
            modifier = Modifier.fillMaxSize()
        )

        // Top controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    activeRecording?.stop()
                    onClose()
                },
                modifier = Modifier.clip(CircleShape).background(Color.Black.copy(alpha = 0.5f))
            ) {
                Icon(Icons.Filled.Close, "Close Camera", tint = Color.White)
            }

            if (isRecording) {
                Surface(color = StudioCrimson, shape = RoundedCornerShape(16.dp)) {
                    Text(
                        text = "REC ${formatDuration(elapsedSec)}",
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            IconButton(
                onClick = { if (!isRecording) useFrontCamera = !useFrontCamera },
                modifier = Modifier.clip(CircleShape).background(Color.Black.copy(alpha = 0.5f))
            ) {
                Icon(Icons.Filled.Cameraswitch, "Flip Camera", tint = Color.White)
            }
        }

        // Bottom Record Shutter
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.3f))
                    .clickable {
                        val vc = videoCapture ?: return@clickable
                        if (isRecording) {
                            activeRecording?.stop()
                            isRecording = false
                        } else {
                            val outFile = MediaStorageHelper.createRecordingOutputFile(context)
                            val outputOptions = FileOutputOptions.Builder(outFile).build()
                            val hasAudio = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                            val pending = vc.output.prepareRecording(context, outputOptions).apply {
                                if (hasAudio) withAudioEnabled()
                            }
                            activeRecording = pending.start(ContextCompat.getMainExecutor(context)) { event ->
                                when (event) {
                                    is VideoRecordEvent.Start -> isRecording = true
                                    is VideoRecordEvent.Finalize -> {
                                        isRecording = false
                                        if (!event.hasError()) {
                                            onRecorded(Uri.fromFile(outFile))
                                        } else {
                                            onError("Recording error: ${event.error}")
                                        }
                                    }
                                }
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(if (isRecording) 30.dp else 58.dp)
                        .clip(if (isRecording) RoundedCornerShape(8.dp) else CircleShape)
                        .background(StudioCrimson)
                )
            }
        }
    }
}
