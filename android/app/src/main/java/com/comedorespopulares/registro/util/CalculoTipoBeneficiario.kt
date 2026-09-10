package com.comedorespopulares.registro.util

import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Resultado del cálculo del tipo de beneficiario para el titular.
 */
data class ResultadoTipoBeneficiario(
    val tipoBeneficiario: String,  // "1" (Socia) o "3" (Caso Social, 60+)
    val edad: Int?,                // Edad exacta en años si se pudo calcular
    val edadIndeterminada: Boolean  // true si no se pudo leer la fecha de nacimiento
)

/**
 * Función pura y aislada para calcular el tipo de beneficiario del titular
 * (Socia = 1, Caso Social adulto mayor 60+ = 3).
 *
 * NOTA: Esta regla aplica únicamente al titular (Socia / Caso Social).
 * Pareja e Hijos son siempre tipo "2".
 */
object CalculoTipoBeneficiario {

    private val formatters = listOf(
        DateTimeFormatter.ofPattern("dd/MM/yyyy"),
        DateTimeFormatter.ofPattern("d/M/yyyy"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        DateTimeFormatter.ofPattern("dd-MM-yyyy")
    )

    /**
     * Calcula la edad exacta y retorna "3" (Caso Social) si tiene 60 años o más,
     * o "1" (Socia) si tiene menos de 60 años.
     * Si no se puede parsear la fecha, retorna "1" con edadIndeterminada = true.
     */
    fun calcularTipoBeneficiario(
        fechaNacimientoStr: String,
        fechaActual: LocalDate = LocalDate.now()
    ): ResultadoTipoBeneficiario {
        val nacimiento = parsearFecha(fechaNacimientoStr)
            ?: return ResultadoTipoBeneficiario(
                tipoBeneficiario = "1",
                edad = null,
                edadIndeterminada = true
            )

        val period = Period.between(nacimiento, fechaActual)
        val edadAnios = period.years

        val tipo = if (edadAnios >= 60) "3" else "1"

        return ResultadoTipoBeneficiario(
            tipoBeneficiario = tipo,
            edad = edadAnios,
            edadIndeterminada = false
        )
    }

    private fun parsearFecha(fechaStr: String): LocalDate? {
        val limpia = fechaStr.trim()
        if (limpia.isBlank()) return null

        for (formatter in formatters) {
            try {
                return LocalDate.parse(limpia, formatter)
            } catch (_: DateTimeParseException) {
                // intentar siguiente formato
            }
        }
        return null
    }
}
