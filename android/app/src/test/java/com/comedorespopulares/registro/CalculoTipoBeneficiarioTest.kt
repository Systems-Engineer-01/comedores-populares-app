package com.comedorespopulares.registro

import com.comedorespopulares.registro.util.CalculoTipoBeneficiario
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class CalculoTipoBeneficiarioTest {

    private val fechaReferencia = LocalDate.of(2026, 9, 10)

    @Test
    fun `titular con 60 anios o mas asigna tipo 3 Caso Social`() {
        // Nació el 10 de septiembre de 1966 -> Cumple 60 años hoy (2026-09-10)
        val resultadoExacto60 = CalculoTipoBeneficiario.calcularTipoBeneficiario("10/09/1966", fechaReferencia)
        assertEquals("3", resultadoExacto60.tipoBeneficiario)
        assertEquals(60, resultadoExacto60.edad)
        assertFalse(resultadoExacto60.edadIndeterminada)

        // Nació en 1950 -> 76 años
        val resultado76 = CalculoTipoBeneficiario.calcularTipoBeneficiario("15/04/1950", fechaReferencia)
        assertEquals("3", resultado76.tipoBeneficiario)
        assertEquals(76, resultado76.edad)
        assertFalse(resultado76.edadIndeterminada)
    }

    @Test
    fun `titular con menos de 60 anios asigna tipo 1 Socia`() {
        // Nació el 11 de septiembre de 1966 -> Mañana cumple 60 años, hoy tiene 59 años
        val resultado59 = CalculoTipoBeneficiario.calcularTipoBeneficiario("11/09/1966", fechaReferencia)
        assertEquals("1", resultado59.tipoBeneficiario)
        assertEquals(59, resultado59.edad)
        assertFalse(resultado59.edadIndeterminada)

        // Nació en 1990 -> 36 años
        val resultado36 = CalculoTipoBeneficiario.calcularTipoBeneficiario("01/01/1990", fechaReferencia)
        assertEquals("1", resultado36.tipoBeneficiario)
        assertEquals(36, resultado36.edad)
        assertFalse(resultado36.edadIndeterminada)
    }

    @Test
    fun `fecha invalida o vacia marca edadIndeterminada true y asigna 1 por defecto`() {
        val resultadoVacio = CalculoTipoBeneficiario.calcularTipoBeneficiario("", fechaReferencia)
        assertEquals("1", resultadoVacio.tipoBeneficiario)
        assertNull(resultadoVacio.edad)
        assertTrue(resultadoVacio.edadIndeterminada)

        val resultadoInvalido = CalculoTipoBeneficiario.calcularTipoBeneficiario("fecha_ilegible", fechaReferencia)
        assertEquals("1", resultadoInvalido.tipoBeneficiario)
        assertNull(resultadoInvalido.edad)
        assertTrue(resultadoInvalido.edadIndeterminada)
    }
}
