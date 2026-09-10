package com.comedorespopulares.registro.data.model

import kotlinx.serialization.Serializable

/**
 * Resultado de la extracción del REVERSO del DNI peruano vía Gemini.
 * Corresponde al JSON definido en /prompts/extraccion_dni.md
 */
@Serializable
data class DniReversoResult(
    val direccion: String = "",
    val distrito: String = "",
    val provincia: String = "",
    val departamento: String = "",
    val confianza: String = "baja"   // "alta", "media", "baja"
) {
    val esConfiable: Boolean
        get() = confianza != "baja" &&
                direccion.isNotBlank() &&
                distrito.isNotBlank()
}
