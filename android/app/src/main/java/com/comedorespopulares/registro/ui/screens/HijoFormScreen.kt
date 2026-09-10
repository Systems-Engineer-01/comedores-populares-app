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
import com.comedorespopulares.registro.data.model.DniAnversoResult
import com.comedorespopulares.registro.ui.components.ConfidenceBanner
import com.comedorespopulares.registro.ui.components.PersonaFormFields

/**
 * Sprint 5-6 — Formulario de un hijo/a con datos extraídos.
 * Paso 12 del flujo (PRD sección 2).
 *
 * Regla de sexo: si es mujer SÍ pregunta Gestante; si es varón NO.
 * Si el sexo no se detectó, pregunta al usuario.
 * Apoderado se copia automáticamente de la socia (mostrado como solo lectura).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HijoFormScreen(
    indice: Int,
    totalHijos: Int,
    datosExtraidos: DniAnversoResult?,
    sexoDetectado: String?,
    apoderadoNombre: String,
    onConfirmar: (
        dni: String,
        apellidoPaterno: String,
        apellidoMaterno: String,
        nombres: String,
        sexo: String,
        gestante: String,
        discapacidad: String
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    val esEditable = datosExtraidos?.esConfiable != true

    var dni by remember(datosExtraidos) { mutableStateOf(datosExtraidos?.dni ?: "") }
    var apellidoPaterno by remember(datosExtraidos) { mutableStateOf(datosExtraidos?.apellido_paterno ?: "") }
    var apellidoMaterno by remember(datosExtraidos) { mutableStateOf(datosExtraidos?.apellido_materno ?: "") }
    var nombres by remember(datosExtraidos) { mutableStateOf(datosExtraidos?.nombres ?: "") }
    var sexo by remember(datosExtraidos, sexoDetectado) {
        mutableStateOf(sexoDetectado ?: datosExtraidos?.sexo ?: "")
    }
    var gestante by remember { mutableStateOf("No") }
    var discapacidad by remember { mutableStateOf("No") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(R.string.hijo_numero, indice + 1, totalHijos))
                },
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (esEditable) {
                ConfidenceBanner()
            }

            PersonaFormFields(
                dni = dni,
                apellidoPaterno = apellidoPaterno,
                apellidoMaterno = apellidoMaterno,
                nombres = nombres,
                sexo = sexo,
                editable = esEditable,
                onDniChange = { dni = it },
                onApellidoPaternoChange = { apellidoPaterno = it },
                onApellidoMaternoChange = { apellidoMaterno = it },
                onNombresChange = { nombres = it }
            )

            // Si el sexo no se pudo detectar, preguntar directamente
            if (sexo.isBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.pregunta_sexo_hijo),
                            style = MaterialTheme.typography.titleSmall
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            FilterChip(
                                selected = sexo == "M",
                                onClick = { sexo = "M" },
                                label = { Text("Hombre") }
                            )
                            FilterChip(
                                selected = sexo == "F",
                                onClick = { sexo = "F" },
                                label = { Text("Mujer") }
                            )
                        }
                    }
                }
            }

            // Gestante: solo si es mujer
            if (sexo == "F") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.pregunta_gestante),
                            style = MaterialTheme.typography.titleSmall
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            FilterChip(
                                selected = gestante == "Sí",
                                onClick = { gestante = "Sí" },
                                label = { Text(stringResource(R.string.si)) }
                            )
                            FilterChip(
                                selected = gestante == "No",
                                onClick = { gestante = "No" },
                                label = { Text(stringResource(R.string.no)) }
                            )
                        }
                    }
                }
            }

            // Discapacidad
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.pregunta_discapacidad),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        FilterChip(
                            selected = discapacidad == "Sí",
                            onClick = { discapacidad = "Sí" },
                            label = { Text(stringResource(R.string.si)) }
                        )
                        FilterChip(
                            selected = discapacidad == "No",
                            onClick = { discapacidad = "No" },
                            label = { Text(stringResource(R.string.no)) }
                        )
                    }
                }
            }

            // Apoderado automático (Sprint 6 — datos de la socia)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Apoderado (automático: datos de la socia)",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = apoderadoNombre,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Button(
                onClick = {
                    onConfirmar(
                        dni, apellidoPaterno, apellidoMaterno, nombres,
                        sexo,
                        if (sexo == "M") "" else gestante,
                        discapacidad
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = sexo.isNotBlank()
            ) {
                Text(stringResource(R.string.btn_continuar))
            }
        }
    }
}
