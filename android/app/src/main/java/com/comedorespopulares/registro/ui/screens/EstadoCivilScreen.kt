package com.comedorespopulares.registro.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.comedorespopulares.registro.R

/**
 * Sprint 4 — Pantalla de selección de estado civil de la socia.
 * Paso 6 del flujo (PRD sección 2).
 *
 * El estado civil NO va en la plantilla oficial — se guarda en Interno_Control.
 * Si la socia NO elige "Ninguna de las anteriores" → se activa el flujo de pareja.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EstadoCivilScreen(
    onEstadoSeleccionado: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val opciones = listOf(
        stringResource(R.string.estado_soltera),
        stringResource(R.string.estado_casada),
        stringResource(R.string.estado_viuda),
        stringResource(R.string.estado_divorciada),
        stringResource(R.string.estado_acompanada),
        stringResource(R.string.estado_ninguna),
        stringResource(R.string.estado_otro)
    )

    var seleccionado by remember { mutableStateOf("") }
    var textoOtro by remember { mutableStateOf("") }
    var mostrarOtro by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.titulo_estado_civil)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Seleccione el estado civil de la socia (uso interno, no va en la plantilla oficial):",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            opciones.forEach { opcion ->
                Card(
                    onClick = {
                        seleccionado = opcion
                        mostrarOtro = opcion == stringResource(R.string.estado_otro)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (seleccionado == opcion)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        RadioButton(
                            selected = seleccionado == opcion,
                            onClick = {
                                seleccionado = opcion
                                mostrarOtro = opcion == "Otro"
                            }
                        )
                        Text(
                            text = opcion,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }

            // Campo de texto libre si eligió "Otro"
            if (mostrarOtro) {
                OutlinedTextField(
                    value = textoOtro,
                    onValueChange = { textoOtro = it },
                    label = { Text(stringResource(R.string.hint_estado_otro)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    val valor = if (mostrarOtro && textoOtro.isNotBlank()) textoOtro else seleccionado
                    onEstadoSeleccionado(valor)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = seleccionado.isNotBlank()
            ) {
                Text(stringResource(R.string.btn_continuar))
            }
        }
    }
}
