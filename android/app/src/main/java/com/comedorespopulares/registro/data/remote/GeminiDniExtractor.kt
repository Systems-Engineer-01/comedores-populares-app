package com.comedorespopulares.registro.data.remote

import android.graphics.Bitmap
import com.comedorespopulares.registro.data.model.DniAnversoResult
import com.comedorespopulares.registro.data.model.DniReversoResult
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.serialization.json.Json

/**
 * Extrae datos del DNI peruano usando Gemini 3.1 Pro (visión multimodal).
 *
 * Usa los prompts definidos en /prompts/extraccion_dni.md para obtener
 * un JSON estricto que se parsea directamente a las data classes.
 */
class GeminiDniExtractor(apiKey: String) {

    private val model = GenerativeModel(
        modelName = "gemini-1.5-pro",   // Actualizar al modelo disponible más reciente
        apiKey = apiKey
    )

    private val json = Json { ignoreUnknownKeys = true }

    // ───── Prompt para ANVERSO (de /prompts/extraccion_dni.md) ─────
    private val promptAnverso = """
        Eres un extractor de datos de Documentos Nacionales de Identidad (DNI) del Perú. Se te entrega la foto
        del ANVERSO de un DNI. Devuelve ÚNICAMENTE un objeto JSON válido (sin texto adicional, sin markdown),
        con esta forma exacta:

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
        - Ignora la zona MRZ (las líneas con "<<<") si el texto impreso arriba ya es legible; úsala solo como
          respaldo si el texto impreso está borroso.
    """.trimIndent()

    // ───── Prompt para REVERSO (de /prompts/extraccion_dni.md) ─────
    private val promptReverso = """
        Eres un extractor de datos del REVERSO de un DNI peruano. Devuelve ÚNICAMENTE este JSON:

        {
          "direccion": "string, tal como aparece en el campo 'Dirección'",
          "distrito": "string, tal como aparece en el campo 'Distrito'",
          "provincia": "string",
          "departamento": "string",
          "confianza": "alta" | "media" | "baja"
        }

        Reglas:
        - No agregues el departamento/provincia dentro de "direccion"; van en campos separados.
        - Si el campo no es legible, usa "" y baja "confianza".
    """.trimIndent()

    /**
     * Extrae datos del ANVERSO del DNI.
     * @param bitmap Imagen de la foto del anverso del DNI.
     * @return DniAnversoResult con los campos extraídos.
     */
    suspend fun extraerAnverso(bitmap: Bitmap): DniAnversoResult {
        return try {
            val response = model.generateContent(
                content {
                    image(bitmap)
                    text(promptAnverso)
                }
            )
            val jsonText = limpiarJsonResponse(response.text ?: "")
            json.decodeFromString<DniAnversoResult>(jsonText)
        } catch (e: Exception) {
            // En caso de error, devolver resultado vacío con confianza baja
            DniAnversoResult(confianza = "baja")
        }
    }

    /**
     * Extrae datos del REVERSO del DNI.
     * @param bitmap Imagen de la foto del reverso del DNI.
     * @return DniReversoResult con los campos extraídos.
     */
    suspend fun extraerReverso(bitmap: Bitmap): DniReversoResult {
        return try {
            val response = model.generateContent(
                content {
                    image(bitmap)
                    text(promptReverso)
                }
            )
            val jsonText = limpiarJsonResponse(response.text ?: "")
            json.decodeFromString<DniReversoResult>(jsonText)
        } catch (e: Exception) {
            DniReversoResult(confianza = "baja")
        }
    }

    /**
     * Limpia la respuesta de Gemini por si incluye backticks markdown
     * o texto extra alrededor del JSON.
     */
    private fun limpiarJsonResponse(raw: String): String {
        var cleaned = raw.trim()
        // Remover bloques de código markdown (```json ... ```)
        if (cleaned.startsWith("```")) {
            cleaned = cleaned
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()
        }
        // Intentar extraer el primer { ... } si hay texto antes/después
        val start = cleaned.indexOf('{')
        val end = cleaned.lastIndexOf('}')
        if (start >= 0 && end > start) {
            cleaned = cleaned.substring(start, end + 1)
        }
        return cleaned
    }
}
