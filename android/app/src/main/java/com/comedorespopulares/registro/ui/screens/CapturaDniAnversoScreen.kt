package com.comedorespopulares.registro.ui.screens

import android.Manifest
import android.content.Context
import androidx.core.content.contentValuesOf
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.comedorespopulares.registro.ui.viewmodel.CapturaDniAnversoUiState
import com.comedorespopulares.registro.ui.viewmodel.RegistroSociaViewModel
import com.comedorespopulares.registro.util.Validators
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Pantalla Compose para la captura y extracción del ANVERSO del DNI de la Socia (Sprint 2).
 *
 * Cumple con todas las reglas de negocio del PRD:
 * - Integración con CameraX + Selector de Galería alternativo.
 * - Loading overlay mientras procesa Gemini API.
 * - 4 campos EDITABLES (DNI, Ap. Paterno, Ap. Materno, Nombres) pre-llenados.
 * - Resaltado en ROJO si confianza == "baja" o campos vacíos/DNI inválido.
 * - Advertencia visual de revisión para la presidenta.
 * - Habilitación del botón "Confirmar" solo si los 4 campos son válidos y no vacíos.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CapturaDniAnversoScreen(
    viewModel: RegistroSociaViewModel,
    onConfirmarSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()

    var mostrarCamaraDirecta by remember { mutableStateOf(false) }

    // Launcher de permisos de cámara
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { concedido ->
        if (concedido) {
            mostrarCamaraDirecta = true
        } else {
            Toast.makeText(
                context,
                "Se requiere permiso de cámara para tomar fotos del DNI",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // Launcher para selección de galería (alternativa)
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.procesarImagenDniAnverso(context, it)
        }
    }

    // Monitorear cuando el paso es completado con éxito para avanzar
    LaunchedEffect(state.pasoCompletado) {
        if (state.pasoCompletado) {
            viewModel.resetPasoCompletado()
            onConfirmarSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Sprint 2 — Extracción DNI (Socia)", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("Captura del Anverso del DNI", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                // Vista de Cámara CameraX en vivo
                CameraXPreviewView(
                    context = context,
                    onFotoCapturada = { uri ->
                        mostrarCamaraDirecta = false
                        viewModel.procesarImagenDniAnverso(context, uri)
                    },
                    onCancelar = {
                        mostrarCamaraDirecta = false
                    }
                )
            } else {
                // Pantalla Principal de Captura y Edición
                FormularioCapturaAnversoContent(
                    state = state,
                    onTomarFotoClick = {
                        val tienePermiso = ContextCompat.checkSelfPermission(
                            context, Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED

                        if (tienePermiso) {
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
                    onDniChange = viewModel::onDniChange,
                    onApellidoPaternoChange = viewModel::onApellidoPaternoChange,
                    onApellidoMaternoChange = viewModel::onApellidoMaternoChange,
                    onNombresChange = viewModel::onNombresChange,
                    onConfirmarClick = viewModel::confirmarDatosAnverso
                )
            }

            // Indicator de Carga Overlay mientras Gemini procesa
            if (state.isLoading) {
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
                            text = "Analizando anverso del DNI con Gemini AI...",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Extrayendo Nombres, Apellidos y DNI",
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
 * Contenido principal del formulario de captura y campos editables.
 */
@Composable
private fun FormularioCapturaAnversoContent(
    state: CapturaDniAnversoUiState,
    onTomarFotoClick: () -> Unit,
    onSeleccionarGaleriaClick: () -> Unit,
    onDniChange: (String) -> Unit,
    onApellidoPaternoChange: (String) -> Unit,
    onApellidoMaternoChange: (String) -> Unit,
    onNombresChange: (String) -> Unit,
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
        // Tarjeta de captura de foto
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
                    imageVector = Icons.Default.Badge,
                    contentDescription = "DNI Anverso",
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Foto del ANVERSO del DNI",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "Fotografíe la cara donde aparecen la foto y datos personales de la socia.",
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

        // Banner de Advertencia si la confianza es baja o falló la extracción
        AnimatedVisibility(visible = state.requiereRevisionVisual || state.advertencia != null || state.errorMessage != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (state.errorMessage != null) MaterialTheme.colorScheme.errorContainer
                    else MaterialTheme.colorScheme.warningContainer
                ),
                border = BorderStroke(
                    1.dp,
                    if (state.errorMessage != null) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.error
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Advertencia",
                        tint = if (state.errorMessage != null) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = state.errorMessage
                            ?: state.advertencia
                            ?: "Confianza baja o campos incompletos. Revise y corrija los datos resaltados antes de continuar.",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (state.errorMessage != null) MaterialTheme.colorScheme.onErrorContainer
                        else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Título de sección de datos
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Campos Autocompletados (SIEMPRE EDITABLES)",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.weight(1f))
            if (state.confianza.isNotBlank()) {
                Surface(
                    color = when (state.confianza) {
                        "alta" -> Color(0xFF2E7D32)
                        "media" -> Color(0xFFF57F17)
                        else -> MaterialTheme.colorScheme.error
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Confianza: ${state.confianza.uppercase()}",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Campo DNI (8 dígitos)
        val dniEsError = !Validators.esDniValido(state.dni) && state.dni.isNotBlank()
        OutlinedTextField(
            value = state.dni,
            onValueChange = onDniChange,
            label = { Text("DNI (8 dígitos) *") },
            placeholder = { Text("Ej. 12345678") },
            isError = dniEsError || (state.confianza == "baja" && state.dni.isBlank()),
            supportingText = {
                if (dniEsError) {
                    Text("El DNI debe tener exactamente 8 dígitos numéricos", color = MaterialTheme.colorScheme.error)
                } else if (state.dni.isBlank()) {
                    Text("Campo obligatorio", color = MaterialTheme.colorScheme.error)
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            shape = RoundedCornerShape(12.dp),
            trailingIcon = {
                if (Validators.esDniValido(state.dni)) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "Válido", tint = Color(0xFF2E7D32))
                } else {
                    Icon(Icons.Default.Error, contentDescription = "Revisar", tint = MaterialTheme.colorScheme.error)
                }
            }
        )

        // Campo Apellido Paterno
        val apPaternoError = state.apellidoPaterno.isBlank()
        OutlinedTextField(
            value = state.apellidoPaterno,
            onValueChange = onApellidoPaternoChange,
            label = { Text("Apellido Paterno *") },
            isError = apPaternoError || state.confianza == "baja",
            supportingText = {
                if (apPaternoError) {
                    Text("Campo obligatorio", color = MaterialTheme.colorScheme.error)
                }
            },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            shape = RoundedCornerShape(12.dp)
        )

        // Campo Apellido Materno
        val apMaternoError = state.apellidoMaterno.isBlank()
        OutlinedTextField(
            value = state.apellidoMaterno,
            onValueChange = onApellidoMaternoChange,
            label = { Text("Apellido Materno *") },
            isError = apMaternoError || state.confianza == "baja",
            supportingText = {
                if (apMaternoError) {
                    Text("Campo obligatorio", color = MaterialTheme.colorScheme.error)
                }
            },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            shape = RoundedCornerShape(12.dp)
        )

        // Campo Nombres
        val nombresError = state.nombres.isBlank()
        OutlinedTextField(
            value = state.nombres,
            onValueChange = onNombresChange,
            label = { Text("Nombres Completo *") },
            isError = nombresError || state.confianza == "baja",
            supportingText = {
                if (nombresError) {
                    Text("Campo obligatorio", color = MaterialTheme.colorScheme.error)
                }
            },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Botón Confirmar: Habilitado SOLO cuando los 4 campos son válidos y no vacíos
        Button(
            onClick = onConfirmarClick,
            enabled = state.esValido && !state.isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Default.ArrowForward, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Confirmar y Continuar",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Componente CameraX para la vista previa de cámara en vivo y captura de foto.
 */
@Composable
private fun CameraXPreviewView(
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

        // Overlay con guía visual para encuadrar el DNI
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
                    .border(2.dp, Color.Green.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
            )
        }

        // Botones de acción en la parte inferior
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

            // Botón Obturador
            FloatingActionButton(
                onClick = {
                    val capture = imageCapture ?: return@FloatingActionButton
                    val photoFile = createTempImageFile(context)
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

/**
 * Crea un archivo temporal en la caché de la app para almacenar la foto capturada.
 */
private fun createTempImageFile(context: Context): File {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(System.currentTimeMillis())
    val storageDir = context.cacheDir
    return File.createTempFile("JPEG_DNI_${timeStamp}_", ".jpg", storageDir)
}

// Support color definition for Warning container if not defined in theme
val ColorScheme.warningContainer: Color
    @Composable get() = Color(0xFFFFF3E0)
