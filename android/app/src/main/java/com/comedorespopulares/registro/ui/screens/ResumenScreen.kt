package com.comedorespopulares.registro.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.comedorespopulares.registro.R
import com.comedorespopulares.registro.data.model.Persona
import com.comedorespopulares.registro.ui.theme.SuccessGreen

/**
 * Pantalla de resumen antes de enviar al backend.
 * Muestra todos los datos que se van a registrar para confirmación.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResumenScreen(
    sociaDni: String,
    sociaNombre: String,
    tienePareja: Boolean,
    parejaDni: String,
    parejaNombre: String,
    hijos: List<Persona>,
    onEnviar: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.titulo_resumen)) },
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
            // Socia
            ResumenPersonaCard(
                titulo = "Socia (Tipo 1)",
                dni = sociaDni,
                nombre = sociaNombre,
                icono = "👩"
            )

            // Pareja
            if (tienePareja) {
                ResumenPersonaCard(
                    titulo = "Cónyuge/Pareja (Tipo 2)",
                    dni = parejaDni,
                    nombre = parejaNombre,
                    icono = "👨"
                )
            }

            // Hijos
            hijos.forEachIndexed { index, hijo ->
                ResumenPersonaCard(
                    titulo = "Hijo ${index + 1} (Tipo 2)",
                    dni = hijo.dni,
                    nombre = "${hijo.apellidoPaterno} ${hijo.apellidoMaterno}, ${hijo.nombres}",
                    icono = if (hijo.esMujer) "👧" else "👦"
                )
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = onEnviar,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.btn_enviar_registro))
            }
        }
    }
}

@Composable
private fun ResumenPersonaCard(
    titulo: String,
    dni: String,
    nombre: String,
    icono: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = icono, style = MaterialTheme.typography.headlineMedium)
            Column {
                Text(
                    text = titulo,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(text = nombre, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = "DNI: $dni",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Pantalla de resultado después de enviar al backend.
 */
@Composable
fun ResultadoScreen(
    exitoso: Boolean,
    numeros: List<Int> = emptyList(),
    errorMensaje: String = "",
    onNuevoRegistro: () -> Unit,
    onReintentar: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            if (exitoso) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(96.dp),
                    tint = SuccessGreen
                )
                Text(
                    text = stringResource(R.string.registro_exitoso),
                    style = MaterialTheme.typography.headlineMedium,
                    color = SuccessGreen,
                    textAlign = TextAlign.Center
                )
                numeros.forEach { numero ->
                    Text(
                        text = stringResource(R.string.registro_numero, numero),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = null,
                    modifier = Modifier.size(96.dp),
                    tint = MaterialTheme.colorScheme.error
                )
                Text(
                    text = stringResource(R.string.error_registro),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = errorMensaje,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                OutlinedButton(
                    onClick = onReintentar,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(stringResource(R.string.btn_reintentar))
                }
            }

            Button(
                onClick = onNuevoRegistro,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(stringResource(R.string.btn_nuevo_registro))
            }
        }
    }
}
