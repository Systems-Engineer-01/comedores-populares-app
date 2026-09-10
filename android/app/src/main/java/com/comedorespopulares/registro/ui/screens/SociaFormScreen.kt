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
 * Sprint 2-3 — Formulario de la Socia con datos extraídos + Gestante/Discapacidad.
 * Pasos 2-3 del flujo (PRD sección 2).
 *
 * Si la confianza es baja, los campos son editables.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SociaFormScreen(
    datosExtraidos: DniAnversoResult?,
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
    var sexo by remember(datosExtraidos) { mutableStateOf(datosExtraidos?.sexo ?: "") }
    var gestante by remember { mutableStateOf("No") }
    var discapacidad by remember { mutableStateOf("No") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Datos de la Socia") },
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
            // Banner de confianza baja
            if (esEditable) {
                ConfidenceBanner()
            }

            // Campos extraídos del DNI
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

            // Pregunta: ¿Gestante? (solo si es mujer)
            if (sexo != "M") {
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

            // Pregunta: ¿Discapacidad?
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

            // Botón continuar
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
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(stringResource(R.string.btn_continuar))
            }
        }
    }
}
