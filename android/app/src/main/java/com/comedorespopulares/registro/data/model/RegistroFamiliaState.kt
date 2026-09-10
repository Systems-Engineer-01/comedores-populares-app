package com.comedorespopulares.registro.data.model

import java.util.UUID

/**
 * Modelo de estado global del registro familiar (Sprint 4).
 * Contiene 1 Socia + 0 o 1 Pareja + N Hijos en una lista.
 * Mantiene un `idFamilia` único (UUID) generado UNA vez por socia
 * y compartido por todos los miembros en la columna ID_Familia de `Interno_Control`.
 */
data class RegistroFamiliaState(
    val idFamilia: String = UUID.randomUUID().toString(),
    val socia: Persona? = null,
    val pareja: Persona? = null,
    val hijos: List<Persona> = emptyList(),
    val estadoCivilDeclarado: String = "",
    val presidentaId: String = ""
) {
    /**
     * Indica si hay una pareja registrada en esta familia.
     */
    val tieneParejaRegistrada: Boolean
        get() = pareja != null

    /**
     * Devuelve la lista completa de todos los integrantes de la familia para el envío al backend.
     */
    val todosLosIntegrantes: List<Persona>
        get() = listOfNotNull(socia, pareja) + hijos

    companion object {
        /**
         * Genera un nuevo ID de familia único en formato UUID.
         */
        fun generarNuevoIdFamilia(): String = UUID.randomUUID().toString()
    }
}
