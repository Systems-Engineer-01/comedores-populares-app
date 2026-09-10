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

    /**
     * Espejo de la función `validar(data)` de /backend/Code.gs para pre-validar
     * personas antes de iniciar el envío secuencial.
     * @return null si es válido, o mensaje de error formateado.
     */
    fun validarParaBackend(persona: com.comedorespopulares.registro.data.model.Persona): String? {
        if (!esDniValido(persona.dni)) return "${persona.rol}: DNI inválido (debe tener 8 dígitos numericos)"
        if (!noEstaVacio(persona.apellidoPaterno)) return "${persona.rol}: Falta Apellido Paterno"
        if (!noEstaVacio(persona.nombres)) return "${persona.rol}: Faltan Nombres"
        if (!esTipoBeneficiarioValido(persona.tipoBeneficiario)) return "${persona.rol}: Tipo de Beneficiario inválido (${persona.tipoBeneficiario})"
        if (!noEstaVacio(persona.direccion)) return "${persona.rol}: Falta Dirección"
        if (!noEstaVacio(persona.distrito)) return "${persona.rol}: Falta Distrito"
        return null
    }
}
