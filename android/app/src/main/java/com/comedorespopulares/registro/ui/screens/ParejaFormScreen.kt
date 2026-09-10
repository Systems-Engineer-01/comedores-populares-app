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
 * Sprint 4 — Formulario de la Pareja con datos extraídos.
 * Pasos 7-8 del flujo (PRD sección 2).
 *
 * Solo pregunta Discapacidad. NO pregunta Gestante (varón).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParejaFormScreen(
    datosExtraidos: DniAnversoResult?,
    onConfirmar: (
        dni: String,
        apellidoPaterno: String,
        apellidoMaterno: String,
        nombres: String,
        discapacidad: String
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    val esEditable = datosExtraidos?.esConfiable != true

    var dni by remember(datosExtraidos) { mutableStateOf(datosExtraidos?.dni ?: "") }
    var apellidoPaterno by remember(datosExtraidos) { mutableStateOf(datosExtraidos?.apellido_paterno ?: "") }
    var apellidoMaterno by remember(datosExtraidos) { mutableStateOf(datosExtraidos?.apellido_materno ?: "") }
    var nombres by remember(datosExtraidos) { mutableStateOf(datosExtraidos?.nombres ?: "") }
    var discapacidad by remember { mutableStateOf("No") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.titulo_pareja)) },
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
                sexo = datosExtraidos?.sexo ?: "",
                editable = esEditable,
                onDniChange = { dni = it },
                onApellidoPaternoChange = { apellidoPaterno = it },
                onApellidoMaternoChange = { apellidoMaterno = it },
                onNombresChange = { nombres = it }
            )

            // Solo Discapacidad — NO Gestante (regla de negocio para pareja/varón)
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

            Button(
                onClick = { onConfirmar(dni, apellidoPaterno, apellidoMaterno, nombres, discapacidad) },
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
