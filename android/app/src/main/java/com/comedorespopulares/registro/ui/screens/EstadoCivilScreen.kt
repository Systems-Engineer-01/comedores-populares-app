package com.comedorespopulares.registro.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Pantalla Compose del Sprint 4: Selección de Estado Civil de la Socia.
 *
 * Muestra las opciones de estado civil para uso interno de la organización.
 * Incluye aviso explícito de que no se comparte en el reporte oficial.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EstadoCivilScreen(
    onEstadoCivilSeleccionado: (estadoCivilFinal: String, tienePareja: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val opcionesPredefinidas = listOf(
        "Soltera",
        "Casada",
        "Viuda",
        "Divorciada",
        "Acompañada",
        "Ninguna de las anteriores",
        "Otro"
    )

    var opcionSeleccionada by remember { mutableStateOf("") }
    var textoOtro by remember { mutableStateOf("") }

    val estadoCivilFinal = if (opcionSeleccionada == "Otro") textoOtro.trim().uppercase() else opcionSeleccionada
    val tienePareja = tieneParejaEvaluador(estadoCivilFinal)
    val esFormularioValido = opcionSeleccionada.isNotBlank() && (opcionSeleccionada != "Otro" || textoOtro.isNotBlank())

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Sprint 4 — Estado Civil (Interno)", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("Configuración de Cónyuge/Pareja", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                .verticalScroll(rememberScrollState())
        ) {
            // Nota informativa de privacidad / uso interno (PRD sección 2 y 4)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Privacidad",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Uso Interno: Este dato es exclusivo para el control interno de la organización (hoja Interno_Control) y NO se comparte en la plantilla o reporte oficial a supervisores.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = "Seleccione el estado civil declarado por la socia:",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // Lista de RadioButtons
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    opcionesPredefinidas.forEach { opcion ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { opcionSeleccionada = opcion }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (opcionSeleccionada == opcion),
                                onClick = { opcionSeleccionada = opcion }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = opcion,
                                fontSize = 15.sp,
                                fontWeight = if (opcionSeleccionada == opcion) FontWeight.Bold else FontWeight.Normal,
                                color = if (opcionSeleccionada == opcion) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Campo de texto libre para opción "Otro"
            AnimatedVisibility(visible = opcionSeleccionada == "Otro") {
                OutlinedTextField(
                    value = textoOtro,
                    onValueChange = { textoOtro = it },
                    label = { Text("Especifique estado civil *") },
                    placeholder = { Text("Ej. Conviviente en unión de hecho") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Indicador dinámico de si activa flujo de Pareja
            if (esFormularioValido) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (tienePareja) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = if (tienePareja) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (tienePareja) "Se activará la captura del DNI del Cónyuge/Pareja."
                            else "No se registrará pareja. Se saltará directo al registro de hijos.",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Botón Continuar
            Button(
                onClick = {
                    if (esFormularioValido) {
                        onEstadoCivilSeleccionado(estadoCivilFinal, tienePareja)
                    }
                },
                enabled = esFormularioValido,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.ArrowForward, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (tienePareja) "Continuar a DNI de Pareja" else "Continuar a Registro de Hijos",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Función aislada y testeable unitariamente (Regla de negocio del Sprint 4):
 * Devuelve true para cualquier estado civil excepto "Ninguna de las anteriores" o vacíos.
 */
fun tieneParejaEvaluador(estadoCivil: String): Boolean {
    val limpio = estadoCivil.trim()
    return limpio.isNotBlank() && !limpio.equals("Ninguna de las anteriores", ignoreCase = true)
}
