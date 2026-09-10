package com.comedorespopulares.registro.data.model

/**
 * Respuesta del backend (Google Apps Script Web App).
 * { ok: true, numero: 128 } o { ok: false, error: "..." }
 */
data class RegistroResponse(
    val ok: Boolean,
    val numero: Int? = null,
    val error: String? = null
)
