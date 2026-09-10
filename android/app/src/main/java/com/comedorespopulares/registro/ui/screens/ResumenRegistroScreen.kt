package com.comedorespopulares.registro.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.comedorespopulares.registro.data.model.Persona
import com.comedorespopulares.registro.data.model.RegistroFamiliaState
import com.comedorespopulares.registro.util.Validators

/**
 * Pantalla Compose del Sprint 6: Resumen y confirmación previa al envío del Registro Familiar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResumenRegistroScreen(
    familiaState: RegistroFamiliaState,
    isEnviando: Boolean,
    progresoMensaje: String?,
    errorMessage: String?,
    onEnviarClick: () -> Unit,
    onReiniciarClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val integrantes = familiaState.todosLosIntegrantes
    val totalIntegrantes = integrantes.size

    // Pre-flight validation contra el contrato del backend Code.gs
    val erroresValidacion: List<String> = remember(familiaState) {
        integrantes.mapNotNull { persona ->
            Validators.validarParaBackend(persona)
        }
    }
    val hayErroresPreflight = erroresValidacion.isNotEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Sprint 6 — Resumen Familiar", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("ID Familia: ${familiaState.idFamilia.take(8)}...", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header resumen de la familia
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.FamilyRestroom,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Familia lista para envío ($totalIntegrantes integrantes)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Estado Civil: ${familiaState.estadoCivilDeclarado.ifBlank { "No especificado" }}",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Banner de validación pre-flight (Sprint 7)
                AnimatedVisibility(visible = hayErroresPreflight) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = "Error de validación",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Atención: Hay datos incompletos antes de enviar al backend:",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            for (err in erroresValidacion) {
                                Text(
                                    text = "• $err",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(start = 8.dp, bottom = 2.dp)
                                )
                            }
                        }
                    }
                }

                // Banner de error en caso de fallo de red durante el envío secuencial
                AnimatedVisibility(visible = errorMessage != null && !hayErroresPreflight) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = "Error",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = errorMessage ?: "Ocurrió un error al enviar.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Lista de Tarjetas de Integrantes
                integrantes.forEachIndexed { index, persona ->
                    TarjetaIntegranteResumen(
                        index = index + 1,
                        persona = persona,
                        sociaDni = familiaState.socia?.dni ?: ""
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Botón Enviar al Backend
                Button(
                    onClick = onEnviarClick,
                    enabled = !isEnviando && totalIntegrantes > 0 && !hayErroresPreflight,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (errorMessage != null) "Reintentar Envío al Backend" else "Enviar Registro al Backend",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Overlay de Carga durante el envío secuencial con LockService
            if (isEnviando) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black.copy(alpha = 0.7f)
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
                            text = progresoMensaje ?: "Enviando secuencialmente al backend...",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Garantizando sincronización y LockService sin colisiones",
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
 * Tarjeta individual para mostrar datos de cada integrante en el resumen.
 */
@Composable
private fun TarjetaIntegranteResumen(
    index: Int,
    persona: Persona,
    sociaDni: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "$index. ${persona.rol.uppercase()}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                val labelTipo = when (persona.tipoBeneficiario) {
                    "1" -> "Tipo 1 — Socia"
                    "3" -> "Tipo 3 — Caso Social (Adulto Mayor)"
                    else -> "Tipo 2 — Usuario"
                }

                val colorTipo = when (persona.tipoBeneficiario) {
                    "1" -> MaterialTheme.colorScheme.primary
                    "3" -> Color(0xFF2E7D32)
                    else -> MaterialTheme.colorScheme.secondary
                }

                Surface(
                    color = colorTipo,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = labelTipo,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${persona.apellidoPaterno} ${persona.apellidoMaterno}, ${persona.nombres}",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "DNI: ${persona.dni} · Sexo: ${persona.sexo}",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Dirección: ${persona.direccion} (${persona.distrito})",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(modifier = Modifier.padding(top = 4.dp)) {
                Text(
                    text = "Gestante: ${persona.gestante.ifBlank { "No aplica" }} · Discapacidad: ${persona.discapacidad}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Muestra apoderado para hijos
            if (persona.rol == "Hijo" || persona.rol == "Hijo/a") {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Apoderado (Socia): DNI $sociaDni (Copiado automático)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}
