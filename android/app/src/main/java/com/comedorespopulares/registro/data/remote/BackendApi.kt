package com.comedorespopulares.registro.data.remote

import com.comedorespopulares.registro.data.model.RegistroRequest
import com.comedorespopulares.registro.data.model.RegistroResponse
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * API para comunicarse con el backend de Google Apps Script Web App.
 * La URL base se configura en local.properties como BACKEND_URL.
 *
 * El endpoint POST recibe un [RegistroRequest] y devuelve un [RegistroResponse]
 * con { ok: true, numero: N } o { ok: false, error: "..." }
 */
interface BackendApi {

    /**
     * Registra una persona (socia, pareja o hijo) en Google Sheets.
     * El backend asigna el N° secuencial con LockService para evitar colisiones.
     */
    @POST("exec")
    suspend fun registrarPersona(@Body request: RegistroRequest): RegistroResponse
}
