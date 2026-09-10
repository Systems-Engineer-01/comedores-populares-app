package com.comedorespopulares.registro.data.model

/**
 * Modelo unificado de una persona (socia, pareja o hijo).
 * Contiene todos los datos necesarios para una fila en Registro_Oficial.
 */
data class Persona(
    // Datos del anverso
    val dni: String = "",
    val apellidoPaterno: String = "",
    val apellidoMaterno: String = "",
    val nombres: String = "",
    val sexo: String = "",               // "M" o "F"

    // Preguntas
    val gestante: String = "",           // "Sí", "No", "" (no aplica)
    val discapacidad: String = "No",     // "Sí" o "No"

    // Datos del reverso
    val direccion: String = "",
    val distrito: String = "",

    // Automáticos
    val tipoBeneficiario: String = "",   // "1" (Socia) o "2" (Usuario)

    // Apoderado (solo para hijos — se copia de la socia automáticamente)
    val apoderadoDni: String = "",
    val apoderadoApellidoPaterno: String = "",
    val apoderadoApellidoMaterno: String = "",
    val apoderadoNombres: String = "",

    // Rol para la hoja interna
    val rol: String = ""                 // "Socia", "Pareja", "Hijo"
) {
    /** Centro Poblado = Dirección (el DNI peruano no trae centro poblado). */
    val centroPoblado: String get() = direccion

    /** Indica si es mujer (para decidir si preguntar Gestante). */
    val esMujer: Boolean get() = sexo.equals("F", ignoreCase = true)
}
