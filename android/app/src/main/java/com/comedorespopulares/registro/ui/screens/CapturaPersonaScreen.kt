package com.comedorespopulares.registro.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessible
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Female
import androidx.compose.material.icons.filled.HomeWork
import androidx.compose.material.icons.filled.Male
import androidx.compose.material.icons.filled.PregnantWoman
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.comedorespopulares.registro.data.model.Persona
import com.comedorespopulares.registro.data.model.RolPersona
import com.comedorespopulares.registro.ui.viewmodel.RegistroSociaViewModel
import com.comedorespopulares.registro.util.SexoFallback

/**
 * Componente reutilizable universal `CapturaPersonaScreen` (Entregable Central del Sprint 5).
 *
 * Sirve para Socia, Pareja e Hijos (parametrizado por `RolPersona`).
 * Administra el flujo interno de 3 fases:
 * 1. Anverso DNI (DNI, Apellidos, Nombres, Sexo)
 * 2. Evaluación de Sexo y Preguntas (Gestante solo si sexo == "F", Discapacidad siempre)
 * 3. Reverso DNI (Dirección, Distrito, Centro Poblado = Dirección)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CapturaPersonaScreen(
    viewModel: RegistroSociaViewModel,
    rol: RolPersona,
    indiceHijo: Int = 0,
    totalHijos: Int = 0,
    onPersonaCapturada: (Persona) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var faseActual by remember { mutableStateOf(1) } // 1: Anverso, 2: Preguntas, 3: Reverso

    // Estado local para la persona capturada en esta instancia
    var dni by remember { mutableStateOf("") }
    var apPaterno by remember { mutableStateOf("") }
    var apMaterno by remember { mutableStateOf("") }
    var nombres by remember { mutableStateOf("") }
    var sexoDetectado by remember { mutableStateOf("") } // "F" o "M"
    var sexoPreguntaManual by remember { mutableStateOf(false) }

    var esGestante by remember { mutableStateOf(false) }
    var tieneDiscapacidad by remember { mutableStateOf(false) }

    var direccion by remember { mutableStateOf("") }
    var distrito by remember { mutableStateOf("") }

    val tituloHeader = when (rol) {
        RolPersona.SOCIA -> "Captura de Socia"
        RolPersona.PAREJA -> "Captura de Cónyuge / Pareja"
        RolPersona.HIJO -> "Captura de Hijo/a (${indiceHijo + 1} de $totalHijos)"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(tituloHeader, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = when (faseActual) {
                                1 -> "Fase 1: Foto del Anverso DNI"
                                2 -> "Fase 2: Condición Física y Preguntas"
                                else -> "Fase 3: Foto del Reverso DNI (Dirección)"
                            },
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
            when (faseActual) {
                // ───── FASE 1: ANVERSO DNI ─────
                1 -> {
                    CapturaDniAnversoScreen(
                        viewModel = viewModel,
                        onConfirmarSuccess = {
                            val state = viewModel.uiState.value
                            dni = state.dni
                            apPaterno = state.apellidoPaterno
                            apMaterno = state.apellidoMaterno
                            nombres = state.nombres

                            // Detección de sexo según PRD & extraccion_dni.md
                            var sexoInf = state.sexo.uppercase().take(1)
                            if (sexoInf != "F" && sexoInf != "M") {
                                // Fallback 1: heurística de nombres peruanos
                                sexoInf = SexoFallback.inferirSexo(nombres) ?: ""
                            }

                            if (sexoInf == "F" || sexoInf == "M") {
                                sexoDetectado = sexoInf
                                sexoPreguntaManual = false
                            } else {
                                // Fallback 2: Pregunta manual directa "¿Es hombre o mujer?"
                                sexoDetectado = ""
                                sexoPreguntaManual = true
                            }

                            faseActual = 2
                        }
                    )
                }

                // ───── FASE 2: PREGUNTAS CONDICIONALES ─────
                2 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Si el sexo es indeterminado, preguntar explícitamente "¿Es hombre o mujer?"
                        if (sexoPreguntaManual || sexoDetectado.isBlank()) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "¿Es hombre o mujer?",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                    Text(
                                        text = "No se pudo determinar automáticamente el sexo en el DNI de $nombres.",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                sexoDetectado = "F"
                                                sexoPreguntaManual = false
                                            },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (sexoDetectado == "F") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                            )
                                        ) {
                                            Icon(Icons.Default.Female, contentDescription = null)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Mujer")
                                        }

                                        Button(
                                            onClick = {
                                                sexoDetectado = "M"
                                                sexoPreguntaManual = false
                                            },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (sexoDetectado == "M") MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant
                                            )
                                        ) {
                                            Icon(Icons.Default.Male, contentDescription = null)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Hombre")
                                        }
                                    }
                                }
                            }
                        }

                        // Pregunta Gestante: SE PREGUNTA SI Y SOLO SI sexo == "F"
                        if (sexoDetectado == "F") {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (esGestante) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                    else MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Icon(Icons.Default.PregnantWoman, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text("¿Está Gestante?", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    }
                                    Switch(checked = esGestante, onCheckedChange = { esGestante = it })
                                }
                            }
                        }

                        // Pregunta Discapacidad: SIEMPRE SE PREGUNTA PARA CUALQUIER ROL
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (tieneDiscapacidad) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                                else MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.Accessible, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(32.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("¿Tiene Discapacidad?", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                }
                                Switch(checked = tieneDiscapacidad, onCheckedChange = { tieneDiscapacidad = it })
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                if (sexoDetectado.isNotBlank()) {
                                    faseActual = 3
                                } else {
                                    Toast.makeText(context, "Por favor seleccione si es hombre o mujer", Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = sexoDetectado.isNotBlank(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.ArrowForward, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Continuar a Reverso DNI", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // ───── FASE 3: REVERSO DNI ─────
                3 -> {
                    CapturaDniReversoScreen(
                        viewModel = viewModel,
                        onConfirmarSuccess = {
                            val state = viewModel.uiState.value
                            direccion = state.direccion
                            distrito = state.distrito

                            // Construir objeto Persona parametrizado por RolPersona
                            val personaFinal = Persona(
                                dni = dni,
                                apellidoPaterno = apPaterno,
                                apellidoMaterno = apMaterno,
                                nombres = nombres,
                                sexo = sexoDetectado,
                                gestante = if (sexoDetectado == "F") (if (esGestante) "Sí" else "No") else "",
                                discapacidad = if (tieneDiscapacidad) "Sí" else "No",
                                direccion = direccion,
                                distrito = distrito,
                                tipoBeneficiario = rol.tipoBeneficiario,
                                rol = rol.tituloLabel
                            )

                            onPersonaCapturada(personaFinal)
                        }
                    )
                }
            }
        }
    }
}
