package com.comedorespopulares.registro.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessible
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ChildFriendly
import androidx.compose.material.icons.filled.PregnantWoman
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.comedorespopulares.registro.ui.viewmodel.RegistroSociaViewModel

/**
 * Pantalla Compose del Sprint 3: Preguntas adicionales para la Socia.
 *
 * Muestra dos preguntas obligatorias en el flujo de la Socia:
 * 1. ¿Está Gestante? (Sí/No)
 * 2. ¿Presenta alguna Discapacidad? (Sí/No)
 *
 * (Ambas preguntas visibles siempre para la Socia al ser mujer por definición de rol).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreguntasSociaScreen(
    viewModel: RegistroSociaViewModel,
    onContinuarSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    // Monitorear finalización del paso
    LaunchedEffect(state.pasoPreguntasCompletado) {
        if (state.pasoPreguntasCompletado) {
            viewModel.resetPasoPreguntasCompletado()
            onContinuarSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Sprint 3 — Condición de la Socia", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("Gestación y Discapacidad", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Marque las opciones que apliquen a la socia para el registro oficial.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Tarjeta Pregunta 1: Gestante
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (state.esGestante) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    else MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PregnantWoman,
                            contentDescription = "Gestante",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "¿Es Gestante?",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = if (state.esGestante) "Estado: SÍ" else "Estado: NO",
                                fontSize = 13.sp,
                                color = if (state.esGestante) MaterialTheme.colorScheme.primary else Color.Gray
                            )
                        }
                    }

                    Switch(
                        checked = state.esGestante,
                        onCheckedChange = { viewModel.setGestante(it) }
                    )
                }
            }

            // Tarjeta Pregunta 2: Discapacidad
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (state.tieneDiscapacidad) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                    else MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Accessible,
                            contentDescription = "Discapacidad",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "¿Tiene Discapacidad?",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = if (state.tieneDiscapacidad) "Estado: SÍ" else "Estado: NO",
                                fontSize = 13.sp,
                                color = if (state.tieneDiscapacidad) MaterialTheme.colorScheme.secondary else Color.Gray
                            )
                        }
                    }

                    Switch(
                        checked = state.tieneDiscapacidad,
                        onCheckedChange = { viewModel.setDiscapacidad(it) }
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.height(32.dp))

            // Botón Continuar al Reverso del DNI
            Button(
                onClick = { viewModel.confirmarPreguntasSocia() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.ArrowForward, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Continuar a Fotos del Reverso",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
