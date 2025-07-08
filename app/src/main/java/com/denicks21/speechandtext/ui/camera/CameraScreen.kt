package com.denicks21.speechandtext.ui.camera

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController

// -----------------------
// Constantes de estilo
// -----------------------
private const val PREVIEW_HEIGHT_RATIO = 0.5f           // 50% de la pantalla
private const val CARD_ALPHA = 0.3f                     // Transparencia estándar
private val CARD_CORNER = 16.dp                         // Radio de bordes
// -----------------------

@Composable
fun CameraPreviewScreen(navController: NavHostController) {
    val context = LocalContext.current

    // Estado de permiso
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted }

    // Solicita permiso si es necesario
    LaunchedEffect(Unit) {
        if (!hasPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }
    if (!hasPermission) return

    // Preparar CameraX
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    DisposableEffect(lifecycleOwner) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val provider = cameraProviderFuture.get()
            val preview = androidx.camera.core.Preview.Builder()
                .build()
                .also { it.setSurfaceProvider(previewView.surfaceProvider) }
            provider.unbindAll()
            provider.bindToLifecycle(
                lifecycleOwner,
                androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA,
                preview
            )
        }, ContextCompat.getMainExecutor(context))
        onDispose {
            ProcessCameraProvider.getInstance(context).get().unbindAll()
        }
    }

    // Layout principal
    BoxWithConstraints(modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colors.background)
        .padding(16.dp)
    ) {
        val screenHeight = maxHeight
        val previewHeight = screenHeight * PREVIEW_HEIGHT_RATIO

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // — Card con la vista previa de la cámara (mitad pantalla) —
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(previewHeight)
                    .clip(RoundedCornerShape(CARD_CORNER)),
                backgroundColor = MaterialTheme.colors.surface.copy(alpha = CARD_ALPHA),
                elevation = 0.dp
            ) {
                AndroidView(
                    factory = { previewView },
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // — Card de información / UI debajo de la cámara —
            // Segunda Card: métricas, 50%
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .weight(1f - PREVIEW_HEIGHT_RATIO)  // ocupa el resto
                    .clip(RoundedCornerShape(CARD_CORNER)),
                backgroundColor = MaterialTheme.colors.surface.copy(alpha = CARD_ALPHA),
                elevation = 0.dp
            ) {
                // Reemplaza este Column con el contenido de tu HomePage / métricas, etc.
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // EL CONTENIDO
                    // a mostrar en la segunda Card en la pantalla
                }
            }
        }
    }
}


