package com.comedorespopulares.registro.ui.screens

import android.net.Uri
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
import com.comedorespopulares.registro.data.model.DniReversoResult
import com.comedorespopulares.registro.ui.components.ConfidenceBanner
import com.comedorespopulares.registro.ui.components.DniCaptureCard

/**
 * Sprint 3 — Captura del reverso del DNI para extraer Dirección y Distrito.
 * Paso 4 del flujo (PRD sección 2).
 * Reutilizable para socia, pareja e hijos (cambiando título y callback).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReversoScreen(
    titulo: String,
    imagenUri: Uri?,
    isLoading: Boolean,
    datosExtraidos: DniReversoResult?,
    onImagenCapturada: (Uri) -> Unit,
    onConfirmar: (direccion: String, distrito: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val esEditable = datosExtraidos?.esConfiable != true
    var direccion by remember(datosExtraidos) { mutableStateOf(datosExtraidos?.direccion ?: "") }
    var distrito by remember(datosExtraidos) { mutableStateOf(datosExtraidos?.distrito ?: "") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(titulo) },
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
            DniCaptureCard(
                titulo = stringResource(R.string.captura_titulo_reverso),
                imagenUri = imagenUri,
                onImagenCapturada = onImagenCapturada,
                isLoading = isLoading
            )

            if (datosExtraidos != null) {
                if (esEditable) {
                    ConfidenceBanner()
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = direccion,
                            onValueChange = { direccion = it },
                            label = { Text(stringResource(R.string.label_direccion)) },
                            enabled = esEditable,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Centro Poblado = Dirección (automático, solo lectura)
                        OutlinedTextField(
                            value = direccion,
                            onValueChange = {},
                            label = { Text(stringResource(R.string.label_centro_poblado)) },
                            enabled = false,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = distrito,
                            onValueChange = { distrito = it },
                            label = { Text(stringResource(R.string.label_distrito)) },
                            enabled = esEditable,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Button(
                    onClick = { onConfirmar(direccion, distrito) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    enabled = direccion.isNotBlank() && distrito.isNotBlank()
                ) {
                    Text(stringResource(R.string.btn_continuar))
                }
            }
        }
    }
}
