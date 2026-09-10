package com.comedorespopulares.registro.data.model

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Data class que representa los datos extraídos del REVERSO del DNI.
 * Corresponde a la especificación de /prompts/extraccion_dni.md
 */
@Serializable
data class DatosDniReverso(
    val direccion: String = "",
    val distrito: String = "",
    val provincia: String = "",
    val departamento: String = "",
    val confianza: String = "baja"     // "alta" | "media" | "baja"
) {
    /**
     * Propiedad calculada para centro poblado (replica la dirección).
     */
    val centroPoblado: String
        get() = mapearCentroPoblado(direccion)

    /**
     * Valida que los campos obligatorios del reverso (dirección y distrito) no estén vacíos.
     */
    val esValido: Boolean
        get() = direccion.isNotBlank() && distrito.isNotBlank()

    companion object {
        /**
         * Función pura testeable que replica la dirección como centro poblado
         * ya que el DNI peruano no trae centro poblado impreso (Regla del PRD).
         */
        fun mapearCentroPoblado(direccion: String): String = direccion.trim()
    }
}
