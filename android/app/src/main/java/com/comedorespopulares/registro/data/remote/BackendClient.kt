package com.comedorespopulares.registro.data.remote

import android.content.Context
import android.provider.Settings
import com.comedorespopulares.registro.BuildConfig
import com.comedorespopulares.registro.data.model.Persona
import com.comedorespopulares.registro.data.model.RegistroFamiliaState
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Resultado individual de la registración de un integrante de la familia devuelto por Apps Script.
 */
data class ResultadoEnvioPersona(
    val persona: Persona,
    val exito: Boolean,
    val numeroAsignado: Int? = null,
    val errorMensaje: String? = null
)

/**
 * Resultado global del proceso de envío secuencial de la familia al backend.
 */
data class ResultadoEnvioFamilia(
    val todosExitosos: Boolean,
    val resultados: List<ResultadoEnvioPersona>,
    val errorGlobal: String? = null
)

/**
 * Cliente de comunicación con el Backend en Google Apps Script Web App.
 *
 * CUMPLE CON REQUERIMIENTOS DEL SPRINT 6:
 * - Envío estrictamente secuencial (respetando LockService de Apps Script).
 * - Mapeo de campos exacto al contrato del doPost de /backend/Code.gs.
 * - Detención inmediata ante falla (ej. DNI duplicado) informando la persona específica.
 */
class BackendClient(
    private val baseUrl: String = BuildConfig.BACKEND_URL,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(45, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(45, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build(),
    private val gson: Gson = Gson()
) {

    /**
     * Obtiene un ID de dispositivo estable (Android ID) para la presidenta.
     */
    fun obtenerPresidentaId(context: Context): String {
        return try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
                ?: "DISPOSITIVO_${UUID.randomUUID().toString().take(8)}"
        } catch (_: Exception) {
            "DISPOSITIVO_DESCONOCIDO"
        }
    }

    /**
     * Copia automáticamente los datos de la Socia como apoderado del Hijo/a (Regla del Sprint 6).
     * Para Socia y Pareja los campos de apoderado quedan vacíos ("").
     */
    fun aplicarApoderado(persona: Persona, socia: Persona): Persona {
        return if (persona.rol == "Hijo" || persona.rol == "Hijo/a") {
            persona.copy(
                apoderadoDni = socia.dni,
                apoderadoApellidoPaterno = socia.apellidoPaterno,
                apoderadoApellidoMaterno = socia.apellidoMaterno,
                apoderadoNombres = socia.nombres
            )
        } else {
            persona.copy(
                apoderadoDni = "",
                apoderadoApellidoPaterno = "",
                apoderadoApellidoMaterno = "",
                apoderadoNombres = ""
            )
        }
    }

    /**
     * Envía secuencialmente a los integrantes de la familia (Socia -> Pareja -> Hijos).
     */
    suspend fun enviarFamiliaSecuencialmente(
        context: Context,
        familiaState: RegistroFamiliaState,
        onProgreso: (personaActual: Persona, indice: Int, total: Int) -> Unit = { _, _, _ -> }
    ): ResultadoEnvioFamilia = withContext(Dispatchers.IO) {
        val socia = familiaState.socia
            ?: return@withContext ResultadoEnvioFamilia(false, emptyList(), "No se ha registrado la Socia")

        val presidentaId = obtenerPresidentaId(context)

        // Preparar lista de personas aplicando regla de apoderado para hijos
        val integrantesAEnviar = mutableListOf<Persona>()

        // 1. Socia
        integrantesAEnviar.add(aplicarApoderado(socia, socia))

        // 2. Pareja (si aplica)
        familiaState.pareja?.let { pareja ->
            integrantesAEnviar.add(aplicarApoderado(pareja, socia))
        }

        // 3. Hijos con apoderado copiado de la socia
        familiaState.hijos.forEach { hijo ->
            integrantesAEnviar.add(aplicarApoderado(hijo, socia))
        }

        val resultados = mutableListOf<ResultadoEnvioPersona>()
        val total = integrantesAEnviar.size

        for ((indice, persona) in integrantesAEnviar.withIndex()) {
            withContext(Dispatchers.Main) {
                onProgreso(persona, indice + 1, total)
            }

            val jsonBody = construirJsonBody(
                idFamilia = familiaState.idFamilia,
                presidentaId = presidentaId,
                estadoCivilDeclarado = familiaState.estadoCivilDeclarado,
                persona = persona
            )

            val resultado = enviarPersonaSingle(jsonBody, persona)
            resultados.add(resultado)

            // Si falla, detener el envío de los siguientes miembros
            if (!resultado.exito) {
                return@withContext ResultadoEnvioFamilia(
                    todosExitosos = false,
                    resultados = resultados,
                    errorGlobal = "Falla al registrar a ${persona.rol} (${persona.nombres} DNI: ${persona.dni}): ${resultado.errorMensaje}"
                )
            }
        }

        ResultadoEnvioFamilia(
            todosExitosos = true,
            resultados = resultados
        )
    }

    /**
     * Construye el JSON exacto esperado por /backend/Code.gs (doPost).
     */
    fun construirJsonBody(
        idFamilia: String,
        presidentaId: String,
        estadoCivilDeclarado: String,
        persona: Persona
    ): JsonObject {
        return JsonObject().apply {
            addProperty("id_familia", idFamilia)
            addProperty("rol", persona.rol)
            addProperty("presidenta_id", presidentaId)
            addProperty("estado_civil", if (persona.rol == "Socia") estadoCivilDeclarado else "")
            addProperty("dni", persona.dni)
            addProperty("apellido_paterno", persona.apellidoPaterno)
            addProperty("apellido_materno", persona.apellidoMaterno)
            addProperty("nombres", persona.nombres)
            addProperty("gestante", persona.gestante)
            addProperty("discapacidad", persona.discapacidad)
            addProperty("direccion", persona.direccion)
            addProperty("distrito", persona.distrito)
            addProperty("tipo_beneficiario", persona.tipoBeneficiario)
            addProperty("apoderado_dni", persona.apoderadoDni)
            addProperty("apoderado_apellido_paterno", persona.apoderadoApellidoPaterno)
            addProperty("apoderado_apellido_materno", persona.apoderadoApellidoMaterno)
            addProperty("apoderado_nombres", persona.apoderadoNombres)
        }
    }

    /**
     * Realiza una llamada HTTP POST a Apps Script.
     */
    private fun enviarPersonaSingle(jsonBody: JsonObject, persona: Persona): ResultadoEnvioPersona {
        if (baseUrl.isBlank() || baseUrl.contains("TU_DEPLOYMENT_ID")) {
            return ResultadoEnvioPersona(
                persona = persona,
                exito = false,
                errorMensaje = "URL de backend no configurada en local.properties / BuildConfig"
            )
        }

        val requestBody = jsonBody.toString()
            .toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url(baseUrl)
            .post(requestBody)
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                val responseStr = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    return ResultadoEnvioPersona(
                        persona = persona,
                        exito = false,
                        errorMensaje = "Error HTTP ${response.code}: $responseStr"
                    )
                }

                val jsonResp = gson.fromJson(responseStr, JsonObject::class.java)
                val ok = jsonResp.get("ok")?.asBoolean ?: false

                if (ok) {
                    val numero = jsonResp.get("numero")?.asInt
                    ResultadoEnvioPersona(
                        persona = persona,
                        exito = true,
                        numeroAsignado = numero
                    )
                } else {
                    val error = jsonResp.get("error")?.asString ?: "Error al registrar en backend"
                    ResultadoEnvioPersona(
                        persona = persona,
                        exito = false,
                        errorMensaje = error
                    )
                }
            }
        } catch (e: Exception) {
            ResultadoEnvioPersona(
                persona = persona,
                exito = false,
                errorMensaje = "Excepción de red: ${e.localizedMessage ?: "Error de conexión"}"
            )
        }
    }
}
