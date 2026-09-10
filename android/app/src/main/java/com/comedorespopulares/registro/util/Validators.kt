package com.comedorespopulares.registro.util

/**
 * Validaciones locales antes de enviar datos al backend.
 * Corresponde a las reglas de validación mencionadas en el PRD (sección 5).
 */
object Validators {

    /** DNI peruano: exactamente 8 dígitos. */
    fun esDniValido(dni: String): Boolean {
        return dni.matches(Regex("^\\d{8}$"))
    }

    /** Verifica que un campo de texto no esté vacío. */
    fun noEstaVacio(valor: String): Boolean {
        return valor.isNotBlank()
    }

    /** Verifica que el tipo de beneficiario sea "1" o "2". */
    fun esTipoBeneficiarioValido(tipo: String): Boolean {
        return tipo == "1" || tipo == "2"
    }

    /**
     * Valida todos los campos obligatorios de una persona antes de enviar.
     * @return null si todo es válido, o un mensaje de error.
     */
    fun validarPersona(
        dni: String,
        apellidoPaterno: String,
        nombres: String,
        tipoBeneficiario: String,
        direccion: String,
        distrito: String
    ): String? {
        if (!esDniValido(dni)) return "DNI inválido (debe tener 8 dígitos)"
        if (!noEstaVacio(apellidoPaterno)) return "Falta Apellido Paterno"
        if (!noEstaVacio(nombres)) return "Faltan Nombres"
        if (!esTipoBeneficiarioValido(tipoBeneficiario)) return "Tipo de Beneficiario inválido"
        if (!noEstaVacio(direccion)) return "Falta Dirección"
        if (!noEstaVacio(distrito)) return "Falta Distrito"
        return null
    }
}
