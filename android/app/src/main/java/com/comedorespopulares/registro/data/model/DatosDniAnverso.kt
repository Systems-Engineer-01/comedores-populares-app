package com.comedorespopulares.registro.data.model

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Data class que representa los datos extraídos del ANVERSO del DNI de la Socia.
 * Corresponde a la especificación de /prompts/extraccion_dni.md
 */
@Serializable
data class DatosDniAnverso(
    val dni: String = "",
    
    @SerializedName("apellido_paterno")
    @SerialName("apellido_paterno")
    val apellidoPaterno: String = "",
    
    @SerializedName("apellido_materno")
    @SerialName("apellido_materno")
    val apellidoMaterno: String = "",
    
    val nombres: String = "",
    val sexo: String = "",            // "M" o "F"
    val confianza: String = "baja"     // "alta" | "media" | "baja"
) {
    /**
     * Devuelve true si los 4 campos principales están completos y la confianza no es baja.
     */
    val esValido: Boolean
        get() = dni.matches(Regex("^\\d{8}$")) &&
                apellidoPaterno.isNotBlank() &&
                apellidoMaterno.isNotBlank() &&
                nombres.isNotBlank()
}
