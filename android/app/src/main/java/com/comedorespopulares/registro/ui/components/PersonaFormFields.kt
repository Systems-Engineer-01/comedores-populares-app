package com.comedorespopulares.registro.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.comedorespopulares.registro.R

/**
 * Componente reutilizable "CapturaPersona" — muestra y permite editar
 * los datos extraídos del DNI (DNI, Apellidos, Nombres).
 *
 * Soporta dos modos:
 * - Lectura (confianza alta): campos deshabilitados
 * - Editable (confianza baja): campos habilitados para corrección manual
 *
 * Referencia: extraccion_dni.md, sección "Manejo de confianza: baja"
 *
 * @param dni Número de DNI
 * @param apellidoPaterno Apellido paterno
 * @param apellidoMaterno Apellido materno
 * @param nombres Nombres
 * @param sexo Sexo ("M" o "F") — se muestra si está disponible
 * @param editable Si los campos son editables (confianza baja)
 * @param onDniChange Callback al cambiar DNI
 * @param onApellidoPaternoChange Callback al cambiar apellido paterno
 * @param onApellidoMaternoChange Callback al cambiar apellido materno
 * @param onNombresChange Callback al cambiar nombres
 * @param errores Mapa de campo -> mensaje de error (para validación)
 */
@Composable
fun PersonaFormFields(
    dni: String,
    apellidoPaterno: String,
    apellidoMaterno: String,
    nombres: String,
    sexo: String = "",
    editable: Boolean = true,
    onDniChange: (String) -> Unit = {},
    onApellidoPaternoChange: (String) -> Unit = {},
    onApellidoMaternoChange: (String) -> Unit = {},
    onNombresChange: (String) -> Unit = {},
    errores: Map<String, String?> = emptyMap(),
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // DNI
            OutlinedTextField(
                value = dni,
                onValueChange = { if (it.length <= 8) onDniChange(it.filter { c -> c.isDigit() }) },
                label = { Text(stringResource(R.string.label_dni)) },
                enabled = editable,
                isError = errores["dni"] != null,
                supportingText = errores["dni"]?.let { { Text(it) } },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Apellido Paterno
            OutlinedTextField(
                value = apellidoPaterno,
                onValueChange = onApellidoPaternoChange,
                label = { Text(stringResource(R.string.label_apellido_paterno)) },
                enabled = editable,
                isError = errores["apellido_paterno"] != null,
                supportingText = errores["apellido_paterno"]?.let { { Text(it) } },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Apellido Materno
            OutlinedTextField(
                value = apellidoMaterno,
                onValueChange = onApellidoMaternoChange,
                label = { Text(stringResource(R.string.label_apellido_materno)) },
                enabled = editable,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Nombres
            OutlinedTextField(
                value = nombres,
                onValueChange = onNombresChange,
                label = { Text(stringResource(R.string.label_nombres)) },
                enabled = editable,
                isError = errores["nombres"] != null,
                supportingText = errores["nombres"]?.let { { Text(it) } },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Sexo (solo lectura, informativo)
            if (sexo.isNotBlank()) {
                OutlinedTextField(
                    value = if (sexo == "M") "Masculino" else "Femenino",
                    onValueChange = {},
                    label = { Text(stringResource(R.string.label_sexo)) },
                    enabled = false,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
