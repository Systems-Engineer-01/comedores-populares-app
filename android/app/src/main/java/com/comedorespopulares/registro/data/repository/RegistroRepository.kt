package com.comedorespopulares.registro.data.repository

import android.graphics.Bitmap
import com.comedorespopulares.registro.BuildConfig
import com.comedorespopulares.registro.data.model.*
import com.comedorespopulares.registro.data.remote.BackendApi
import com.comedorespopulares.registro.data.remote.GeminiDniExtractor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Repositorio central que orquesta la extracción de DNI (Gemini) y el envío
 * al backend (Apps Script). Es la única fuente de verdad para el flujo de registro.
 */
class RegistroRepository {

    private val geminiExtractor = GeminiDniExtractor(BuildConfig.GEMINI_API_KEY)

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG)
                    HttpLoggingInterceptor.Level.BODY
                else
                    HttpLoggingInterceptor.Level.NONE
            }
        )
        // Apps Script redirige de /macros/s/.../exec al servidor real;
        // Retrofit necesita seguir esos redirects.
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val backendApi: BackendApi = Retrofit.Builder()
        .baseUrl(BuildConfig.BACKEND_URL.trimEnd('/') + "/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(BackendApi::class.java)

    // ───── Extracción de DNI ─────

    /** Extrae datos del anverso de un DNI. */
    suspend fun extraerAnverso(bitmap: Bitmap): DniAnversoResult {
        return geminiExtractor.extraerAnverso(bitmap)
    }

    /** Extrae datos del reverso de un DNI. */
    suspend fun extraerReverso(bitmap: Bitmap): DniReversoResult {
        return geminiExtractor.extraerReverso(bitmap)
    }

    // ───── Registro en backend ─────

    /**
     * Genera un ID de familia único para agrupar socia + pareja + hijos.
     * Formato: F-{UUID corto}
     */
    fun generarIdFamilia(): String {
        return "F-${UUID.randomUUID().toString().take(8).uppercase()}"
    }

    /**
     * Obtiene un identificador del dispositivo/presidenta.
     * En producción se podría usar el Android ID o un login.
     */
    fun obtenerPresidentaId(): String {
        return "device-${UUID.randomUUID().toString().take(6)}"
    }

    /**
     * Envía los datos de una persona al backend.
     * @return RegistroResponse con el número asignado o error.
     */
    suspend fun registrarPersona(
        persona: Persona,
        idFamilia: String,
        presidentaId: String,
        estadoCivil: String = ""
    ): RegistroResponse {
        val request = RegistroRequest(
            idFamilia = idFamilia,
            rol = persona.rol,
            presidentaId = presidentaId,
            estadoCivil = estadoCivil,
            dni = persona.dni,
            apellidoPaterno = persona.apellidoPaterno,
            apellidoMaterno = persona.apellidoMaterno,
            nombres = persona.nombres,
            gestante = persona.gestante,
            discapacidad = persona.discapacidad,
            direccion = persona.direccion,
            distrito = persona.distrito,
            tipoBeneficiario = persona.tipoBeneficiario,
            apoderadoDni = persona.apoderadoDni,
            apoderadoApellidoPaterno = persona.apoderadoApellidoPaterno,
            apoderadoApellidoMaterno = persona.apoderadoApellidoMaterno,
            apoderadoNombres = persona.apoderadoNombres
        )

        return try {
            backendApi.registrarPersona(request)
        } catch (e: Exception) {
            RegistroResponse(
                ok = false,
                error = "Error de conexión: ${e.localizedMessage ?: "Sin detalles"}"
            )
        }
    }

    /**
     * Registra una familia completa (socia + pareja opcional + hijos).
     * Envía una persona a la vez, secuencialmente, para respetar el LockService.
     * @return Lista de números asignados o el primer error encontrado.
     */
    suspend fun registrarFamiliaCompleta(
        socia: Persona,
        pareja: Persona?,
        hijos: List<Persona>,
        estadoCivil: String,
        idFamilia: String,
        presidentaId: String
    ): Result<List<Int>> {
        val numerosAsignados = mutableListOf<Int>()

        // 1. Registrar socia
        val respSocia = registrarPersona(socia, idFamilia, presidentaId, estadoCivil)
        if (!respSocia.ok) return Result.failure(Exception(respSocia.error))
        respSocia.numero?.let { numerosAsignados.add(it) }

        // 2. Registrar pareja (si existe)
        if (pareja != null) {
            val respPareja = registrarPersona(pareja, idFamilia, presidentaId)
            if (!respPareja.ok) return Result.failure(Exception(respPareja.error))
            respPareja.numero?.let { numerosAsignados.add(it) }
        }

        // 3. Registrar cada hijo
        for (hijo in hijos) {
            val respHijo = registrarPersona(hijo, idFamilia, presidentaId)
            if (!respHijo.ok) return Result.failure(Exception(respHijo.error))
            respHijo.numero?.let { numerosAsignados.add(it) }
        }

        return Result.success(numerosAsignados)
    }
}
