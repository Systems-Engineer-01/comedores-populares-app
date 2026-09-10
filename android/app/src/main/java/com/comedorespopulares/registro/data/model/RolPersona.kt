package com.comedorespopulares.registro.data.model

/**
 * Enum que define el rol de la persona dentro del flujo de registro.
 * Determina el tipo de beneficiario (1 = Socia, 2 = Usuario) y las preguntas aplicables.
 */
enum class RolPersona(val tituloLabel: String, val tipoBeneficiario: String) {
    SOCIA("Socia", "1"),
    PAREJA("Cónyuge / Pareja", "2"),
    HIJO("Hijo/a", "2")
}
