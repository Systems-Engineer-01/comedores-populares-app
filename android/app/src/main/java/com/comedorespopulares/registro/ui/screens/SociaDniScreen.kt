package com.comedorespopulares.registro.ui.screens

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.comedorespopulares.registro.R
import com.comedorespopulares.registro.ui.components.DniCaptureCard

/**
 * Sprint 2 — Pantalla de captura del DNI anverso de la Socia.
 * Paso 1 del flujo (PRD sección 2).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SociaDniScreen(
    imagenUri: Uri?,
    isLoading: Boolean,
    onImagenCapturada: (Uri) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Paso 1: DNI de la Socia") },
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
            Text(
                text = "Suba la foto del ANVERSO (frente) del DNI de la socia. Los datos se extraerán automáticamente.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            DniCaptureCard(
                titulo = stringResource(R.string.captura_titulo_anverso),
                imagenUri = imagenUri,
                onImagenCapturada = onImagenCapturada,
                isLoading = isLoading
            )
        }
    }
}
