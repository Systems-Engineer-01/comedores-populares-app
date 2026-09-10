package com.comedorespopulares.registro.data.model

import kotlinx.serialization.Serializable

/**
 * Resultado de la extracción del ANVERSO del DNI peruano vía Gemini.
 * Corresponde al JSON definido en /prompts/extraccion_dni.md
 */
@Serializable
data class DniAnversoResult(
    val dni: String = "",
    val apellido_paterno: String = "",
    val apellido_materno: String = "",
    val nombres: String = "",
    val sexo: String = "",           // "M" o "F"
    val confianza: String = "baja"   // "alta", "media", "baja"
) {
    val esConfiable: Boolean
        get() = confianza != "baja" &&
                dni.isNotBlank() &&
                apellido_paterno.isNotBlank() &&
                nombres.isNotBlank()
}
