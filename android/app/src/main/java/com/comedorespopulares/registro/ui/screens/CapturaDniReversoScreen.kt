package com.comedorespopulares.registro.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.comedorespopulares.registro.ui.viewmodel.RegistroSociaUiState
import com.comedorespopulares.registro.ui.viewmodel.RegistroSociaViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Pantalla Compose para la captura y extracción del REVERSO del DNI de la Socia (Sprint 3).
 *
 * Cumple con todas las reglas de negocio del PRD:
 * - Reutiliza el patrón de cámara/galería del Sprint 2.
 * - Extrae Dirección, Distrito, Provincia y Departamento vía Gemini API.
 * - Mantiene los campos 100% EDITABLES.
 * - Muestra advertencia y borde rojo si la confianza es baja o falta dirección/distrito.
 * - Aplica validación: no permite continuar si dirección o distrito quedan vacíos.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CapturaDniReversoScreen(
    viewModel: RegistroSociaViewModel,
    onConfirmarSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()

    var mostrarCamaraDirecta by remember { mutableStateOf(false) }
    var permisoCamaraDenegado by remember { mutableStateOf(false) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { concedido ->
        if (concedido) {
            permisoCamaraDenegado = false
            mostrarCamaraDirecta = true
        } else {
            permisoCamaraDenegado = true
            Toast.makeText(
                context,
                "Se requiere permiso de cámara para tomar foto del DNI",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.procesarImagenDniReverso(context, it)
        }
    }

    LaunchedEffect(state.pasoReversoCompletado) {
        if (state.pasoReversoCompletado) {
            viewModel.resetPasoReversoCompletado()
            onConfirmarSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Sprint 3 — Reverso del DNI", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("Dirección y Distrito de la Socia", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (mostrarCamaraDirecta) {
                ReversoCameraXPreviewView(
                    context = context,
                    onFotoCapturada = { uri ->
                        mostrarCamaraDirecta = false
                        viewModel.procesarImagenDniReverso(context, uri)
                    },
                    onCancelar = { mostrarCamaraDirecta = false }
                )
            } else {
                FormularioCapturaReversoContent(
                    state = state,
                    permisoCamaraDenegado = permisoCamaraDenegado,
                    onAbrirAjustesClick = {
                        val intent = android.content.Intent(
                            android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        ).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    },
                    onTomarFotoClick = {
                        val tienePermiso = ContextCompat.checkSelfPermission(
                            context, Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED

                        if (tienePermiso) {
                            permisoCamaraDenegado = false
                            mostrarCamaraDirecta = true
                        } else {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    },
                    onSeleccionarGaleriaClick = {
                        galleryLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onReintentarExtraccionClick = {
                        viewModel.reintentarExtraccionReverso(context)
                    },
                    onDireccionChange = viewModel::onDireccionChange,
                    onDistritoChange = viewModel::onDistritoChange,
                    onProvinciaChange = viewModel::onProvinciaChange,
                    onDepartamentoChange = viewModel::onDepartamentoChange,
                    onConfirmarClick = viewModel::confirmarDatosReverso
                )
            }

            // Indicator de Carga Overlay mientras Gemini procesa el reverso
            if (state.isLoadingReverso) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black.copy(alpha = 0.65f)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 4.dp,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Analizando reverso del DNI con Gemini AI...",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Extrayendo Dirección y Distrito",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Contenido principal del formulario del Reverso del DNI.
 */
@Composable
private fun FormularioCapturaReversoContent(
    state: RegistroSociaUiState,
    permisoCamaraDenegado: Boolean,
    onAbrirAjustesClick: () -> Unit,
    onTomarFotoClick: () -> Unit,
    onSeleccionarGaleriaClick: () -> Unit,
    onReintentarExtraccionClick: () -> Unit,
    onDireccionChange: (String) -> Unit,
    onDistritoChange: (String) -> Unit,
    onProvinciaChange: (String) -> Unit,
    onDepartamentoChange: (String) -> Unit,
    onConfirmarClick: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Banner explicativo si el permiso de cámara fue denegado (Sprint 7)
        AnimatedVisibility(visible = permisoCamaraDenegado) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.NoPhotography,
                            contentDescription = "Permiso Denegado",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Permiso de cámara denegado. Se requiere para tomar fotos del DNI directamente.",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onAbrirAjustesClick,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Abrir Ajustes de la App")
                    }
                }
            }
        }

        // Tarjeta de captura de foto del reverso
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.HomeWork,
                    contentDescription = "DNI Reverso",
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Foto del REVERSO del DNI",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "Fotografíe la parte posterior donde aparece la dirección de domicilio.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onTomarFotoClick,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Tomar Foto")
                    }

                    OutlinedButton(
                        onClick = onSeleccionarGaleriaClick,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Galería")
                    }
                }
            }
        }

        // Banner de Advertencia/Error con opciones de Reintento (Sprint 7)
        AnimatedVisibility(visible = state.requiereRevisionVisualReverso || state.advertenciaReverso != null || state.errorMessageReverso != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (state.errorMessageReverso != null) MaterialTheme.colorScheme.errorContainer
                    else MaterialTheme.colorScheme.warningContainer
                ),
                border = BorderStroke(
                    1.dp,
                    if (state.errorMessageReverso != null) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.error
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Advertencia",
                            tint = if (state.errorMessageReverso != null) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = state.errorMessageReverso
                                ?: state.advertenciaReverso
                                ?: "Foto borrosa u oscura - sugerimos repetir la foto o corregir los datos resaltados.",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (state.errorMessageReverso != null) MaterialTheme.colorScheme.onErrorContainer
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        if (state.ultimaImagenReversoUri != null) {
                            OutlinedButton(
                                onClick = onReintentarExtraccionClick,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Reintentar Extracción", fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        Button(
                            onClick = onTomarFotoClick,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reintentar Foto", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Título de sección
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Dirección Extraída (EDITABLE)",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.weight(1f))
            if (state.confianzaReverso.isNotBlank()) {
                Surface(
                    color = when (state.confianzaReverso) {
                        "alta" -> Color(0xFF2E7D32)
                        "media" -> Color(0xFFF57F17)
                        else -> MaterialTheme.colorScheme.error
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Confianza: ${state.confianzaReverso.uppercase()}",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Campo Dirección
        val direccionError = state.direccion.isBlank()
        OutlinedTextField(
            value = state.direccion,
            onValueChange = onDireccionChange,
            label = { Text("Dirección completa *") },
            placeholder = { Text("Ej. AV. PERU 1234 PACHACUTEC") },
            isError = direccionError || state.confianzaReverso == "baja",
            supportingText = {
                if (direccionError) {
                    Text("La dirección es obligatoria", color = MaterialTheme.colorScheme.error)
                }
            },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            shape = RoundedCornerShape(12.dp)
        )

        // Campo Distrito
        val distritoError = state.distrito.isBlank()
        OutlinedTextField(
            value = state.distrito,
            onValueChange = onDistritoChange,
            label = { Text("Distrito *") },
            placeholder = { Text("Ej. VENTANILLA") },
            isError = distritoError || state.confianzaReverso == "baja",
            supportingText = {
                if (distritoError) {
                    Text("El distrito es obligatorio", color = MaterialTheme.colorScheme.error)
                }
            },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            shape = RoundedCornerShape(12.dp)
        )

        // Campo Provincia (Opcional)
        OutlinedTextField(
            value = state.provincia,
            onValueChange = onProvinciaChange,
            label = { Text("Provincia") },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            shape = RoundedCornerShape(12.dp)
        )

        // Campo Departamento (Opcional)
        OutlinedTextField(
            value = state.departamento,
            onValueChange = onDepartamentoChange,
            label = { Text("Departamento") },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            shape = RoundedCornerShape(12.dp)
        )

        // Indicador de Regla de Negocio: Centro Poblado = Dirección
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Centro Poblado asignado automáticamente como '${state.centroPoblado}' (Regla de negocio PRD).",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Botón Confirmar: Habilitado SOLO cuando dirección y distrito son no vacíos
        Button(
            onClick = onConfirmarClick,
            enabled = state.esReversoValido && !state.isLoadingReverso,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Default.Check, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Confirmar y Continuar a Estado Civil",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Componente CameraX para captura del reverso.
 */
@Composable
private fun ReversoCameraXPreviewView(
    context: Context,
    onFotoCapturada: (Uri) -> Unit,
    onCancelar: () -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = androidx.camera.core.Preview.Builder().build()
                    preview.setSurfaceProvider(previewView.surfaceProvider)

                    val imgCapture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build()
                    imageCapture = imgCapture

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imgCapture
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp)
                    .border(2.dp, Color.Cyan.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Color.Black.copy(alpha = 0.7f))
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onCancelar,
                modifier = Modifier
                    .size(50.dp)
                    .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(25.dp))
            ) {
                Icon(Icons.Default.Close, contentDescription = "Cancelar", tint = Color.White)
            }

            FloatingActionButton(
                onClick = {
                    val capture = imageCapture ?: return@FloatingActionButton
                    val photoFile = createTempReversoImageFile(context)
                    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

                    capture.takePicture(
                        outputOptions,
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                val savedUri = Uri.fromFile(photoFile)
                                onFotoCapturada(savedUri)
                            }

                            override fun onError(exc: ImageCaptureException) {
                                Toast.makeText(
                                    context,
                                    "Error al tomar foto: ${exc.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    )
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier.size(70.dp)
            ) {
                Icon(Icons.Default.PhotoCamera, contentDescription = "Capturar", modifier = Modifier.size(36.dp))
            }
        }
    }
}

private fun createTempReversoImageFile(context: Context): File {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(System.currentTimeMillis())
    val storageDir = context.cacheDir
    return File.createTempFile("JPEG_DNI_REVERSO_${timeStamp}_", ".jpg", storageDir)
}
