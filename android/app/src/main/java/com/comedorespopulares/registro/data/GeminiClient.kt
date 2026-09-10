package com.comedorespopulares.registro.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.comedorespopulares.registro.BuildConfig
import com.comedorespopulares.registro.data.model.DatosDniAnverso
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.util.concurrent.TimeUnit

/**
 * Módulo de integración HTTP con la API de Gemini (Modelo de Visión).
 *
 * JUSTIFICACIÓN DE ELECCIÓN DE CLIENTE HTTP (OkHttp):
 * Se elige OkHttp directamente sobre Retrofit para este servicio por las siguientes razones:
 * 1. Control directo y granular sobre la construcción del cuerpo JSON multimodal (payload Base64 de la imagen).
 * 2. Fácil configuración de timeouts de red extensos (60s) específicos para llamadas de visión por IA.
 * 3. Procesamiento directo del String de respuesta para limpiar cercos de código Markdown (` ```json `)
 *    antes del parseo con Gson.
 * 4. Menor sobrecarga al tratarse de un endpoint multimodal único con estructura de request dinámica.
 */
class GeminiClient(
    private val apiKey: String = BuildConfig.GEMINI_API_KEY,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build(),
    private val gson: Gson = Gson()
) {

    companion object {
        private const val GEMINI_ENDPOINT =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent"

        val PROMPT_DNI_ANVERSO = """
            Eres un extractor de datos de Documentos Nacionales de Identidad (DNI) del Perú. Se te entrega la foto del ANVERSO de un DNI. Devuelve ÚNICAMENTE un objeto JSON válido (sin texto adicional, sin markdown), con esta forma exacta:

            {
              "dni": "string de 8 dígitos, sin espacios ni guiones",
              "apellido_paterno": "string en mayúsculas",
              "apellido_materno": "string en mayúsculas",
              "nombres": "string en mayúsculas",
              "sexo": "M" o "F",
              "confianza": "alta" | "media" | "baja"
            }

            Reglas:
            - Si algún campo no se puede leer con certeza, usa "" (cadena vacía) en ese campo y baja "confianza".
            - No inventes datos. No completes con suposiciones.
            - El campo "sexo" viene del recuadro "Sexo" del DNI (M/F).
            - Ignora la zona MRZ (las líneas con "<<<") si el texto impreso arriba ya es legible; úsala solo como respaldo si el texto impreso está borroso.
        """.trimIndent()
    }

    /**
     * Extrae los datos del anverso del DNI a partir de una Uri local de imagen.
     * CUMPLE CON LEY N.° 29733: Elimina los datos de imagen de memoria/disco tras su uso.
     */
    suspend fun extraerDniAnverso(context: Context, uri: Uri): Result<DatosDniAnverso> =
        withContext(Dispatchers.IO) {
            var bitmap: Bitmap? = null
            var byteArrayOutputStream: ByteArrayOutputStream? = null
            var base64Image: String? = null

            try {
                // 1. Cargar bitmap y convertir a Base64 con compresión controlada
                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                if (bitmap == null) {
                    return@withContext Result.failure(Exception("No se pudo cargar la imagen del DNI"))
                }

                byteArrayOutputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, byteArrayOutputStream)
                val bytes = byteArrayOutputStream.toByteArray()
                base64Image = Base64.encodeToString(bytes, Base64.NO_WRAP)

                // PRIVACIDAD (Ley N.° 29733): Limpiar bitmap y buffer inmediatamente
                bitmap.recycle()
                bitmap = null
                byteArrayOutputStream.close()
                byteArrayOutputStream = null

                // 2. Ejecutar llamada HTTP a Gemini
                val resultado = ejecutarLlamadaGemini(base64Image, PROMPT_DNI_ANVERSO)

                // 3. Eliminar archivo de caché si fue creado temporalmente
                limpiarArchivoTemporal(context, uri)

                resultado
            } catch (e: Exception) {
                // Asegurar limpieza en caso de excepción
                bitmap?.recycle()
                limpiarArchivoTemporal(context, uri)
                Result.failure(e)
            }
        }

    /**
     * Ejecuta la llamada HTTP REST a la API de Gemini 2.0 Flash (Visión).
     */
    private fun ejecutarLlamadaGemini(base64Image: String, prompt: String): Result<DatosDniAnverso> {
        if (apiKey.isBlank() || apiKey == "TU_API_KEY_AQUI") {
            return Result.failure(
                IllegalStateException(
                    "GEMINI_API_KEY no está configurada en local.properties / BuildConfig."
                )
            )
        }

        val url = "$GEMINI_ENDPOINT?key=$apiKey"

        val jsonRequestBody = JsonObject().apply {
            val contentsArray = com.google.gson.JsonArray().apply {
                val contentObj = JsonObject().apply {
                    val partsArray = com.google.gson.JsonArray().apply {
                        // Part 1: Prompt
                        add(JsonObject().apply { addProperty("text", prompt) })

                        // Part 2: Imagen Base64
                        add(JsonObject().apply {
                            add("inline_data", JsonObject().apply {
                                addProperty("mime_type", "image/jpeg")
                                addProperty("data", base64Image)
                            })
                        })
                    }
                    add("parts", partsArray)
                }
                add(contentObj)
            }
            add("contents", contentsArray)

            // Generation config con JSON mime type
            add("generationConfig", JsonObject().apply {
                addProperty("temperature", 0.1)
                addProperty("responseMimeType", "application/json")
            })
        }

        val requestBody = jsonRequestBody.toString()
            .toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorMsg = response.body?.string() ?: response.message
                    return Result.failure(Exception("Error en Gemini API (${response.code}): $errorMsg"))
                }

                val responseBodyStr = response.body?.string()
                    ?: return Result.failure(Exception("Respuesta vacía de Gemini API"))

                val jsonResponse = gson.fromJson(responseBodyStr, JsonObject::class.java)

                val candidates = jsonResponse.getAsJsonArray("candidates")
                if (candidates == null || candidates.size() == 0) {
                    return Result.failure(Exception("Gemini no devolvió candidatos válidos"))
                }

                val firstCandidate = candidates[0].asJsonObject
                val content = firstCandidate.getAsJsonObject("content")
                val parts = content.getAsJsonArray("parts")
                val rawText = parts[0].asJsonObject.get("text").asString

                // Limpiar backticks markdown antes de parsear
                val cleanedJson = limpiarMarkdownJson(rawText)

                val datosDni = gson.fromJson(cleanedJson, DatosDniAnverso::class.java)
                Result.success(datosDni)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Limpia la respuesta de Gemini eliminando cercos de código Markdown (` ```json ... ``` `).
     */
    fun limpiarMarkdownJson(rawText: String): String {
        var texto = rawText.trim()
        if (texto.startsWith("```json")) {
            texto = texto.substringAfter("```json")
        } else if (texto.startsWith("```JSON")) {
            texto = texto.substringAfter("```JSON")
        } else if (texto.startsWith("```")) {
            texto = texto.substringAfter("```")
        }

        if (texto.endsWith("```")) {
            texto = texto.substringBeforeLast("```")
        }

        return texto.trim()
    }

    /**
     * PRIVACIDAD (Ley N.° 29733): Elimina el archivo de imagen del almacenamiento temporal/caché.
     */
    private fun limpiarArchivoTemporal(context: Context, uri: Uri) {
        try {
            if (uri.scheme == "file") {
                val file = Uri.parse(uri.toString()).path?.let { File(it) }
                if (file != null && file.exists()) {
                    file.delete()
                }
            } else if (uri.scheme == "content") {
                context.contentResolver.delete(uri, null, null)
            }
        } catch (_: Exception) {
            // Ignorar errores de borrado de caché si el archivo ya no existe
        }
    }
}
