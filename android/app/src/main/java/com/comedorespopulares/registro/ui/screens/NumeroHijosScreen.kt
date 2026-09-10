package com.comedorespopulares.registro.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Pantalla Compose del Sprint 5: Selección de la cantidad de hijos (N) a registrar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NumeroHijosScreen(
    onCantidadConfirmada: (cantidad: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var cantidadText by remember { mutableStateOf("0") }

    val cantidadInt = cantidadText.toIntOrNull() ?: 0
    val MAX_HIJOS = 15

    val esCantidadValida = cantidadInt in 0..MAX_HIJOS
    val errorMensaje = when {
        cantidadInt < 0 -> "La cantidad no puede ser negativa."
        cantidadInt > MAX_HIJOS -> "El número máximo permitido es $MAX_HIJOS hijos."
        else -> null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Sprint 5 — Registro de Hijos", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("Cantidad de hijos/as a registrar", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.ChildCare,
                        contentDescription = "Hijos",
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "¿Cuántos hijos/as registrará para esta socia?",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Toda socia registra a sus hijos como usuarios beneficiarios (Tipo 2).",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Stepper +/-
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FilledIconButton(
                            onClick = {
                                if (cantidadInt > 0) cantidadText = (cantidadInt - 1).toString()
                            },
                            enabled = cantidadInt > 0,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Restar")
                        }

                        OutlinedTextField(
                            value = cantidadText,
                            onValueChange = { input ->
                                val filtrado = input.filter { it.isDigit() }.take(2)
                                cantidadText = filtrado
                            },
                            isError = !esCantidadValida,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(
                                textAlign = TextAlign.Center,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier
                                .width(100.dp)
                                .padding(horizontal = 12.dp),
                            shape = RoundedCornerShape(12.dp)
                        )

                        FilledIconButton(
                            onClick = {
                                if (cantidadInt < MAX_HIJOS) cantidadText = (cantidadInt + 1).toString()
                            },
                            enabled = cantidadInt < MAX_HIJOS,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Sumar")
                        }
                    }

                    if (errorMensaje != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorMensaje,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (esCantidadValida) {
                        onCantidadConfirmada(cantidadInt)
                    }
                },
                enabled = esCantidadValida,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.ArrowForward, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (cantidadInt == 0) "Continuar sin Hijos (Resumen)" else "Iniciar Registro de $cantidadInt Hijos",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
