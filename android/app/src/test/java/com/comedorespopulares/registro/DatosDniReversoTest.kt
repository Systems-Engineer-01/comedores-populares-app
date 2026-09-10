package com.comedorespopulares.registro

import com.comedorespopulares.registro.data.model.DatosDniReverso
import org.junit.Assert.*
import org.junit.Test

class DatosDniReversoTest {

    @Test
    fun `mapearCentroPoblado replica la direccion limpiando espacios`() {
        val direccion = " AV. PERU 1234 PACHACUTEC  "
        val resultado = DatosDniReverso.mapearCentroPoblado(direccion)

        assertEquals("AV. PERU 1234 PACHACUTEC", resultado)
    }

    @Test
    fun `esValido es true solo cuando direccion y distrito no estan vacios`() {
        val valido = DatosDniReverso(
            direccion = "JR. UNION 456",
            distrito = "VENTANILLA",
            provincia = "CALLAO",
            departamento = "CALLAO"
        )
        assertTrue(valido.esValido)

        val sinDistrito = DatosDniReverso(
            direccion = "JR. UNION 456",
            distrito = "  "
        )
        assertFalse(sinDistrito.esValido)

        val sinDireccion = DatosDniReverso(
            direccion = "",
            distrito = "VENTANILLA"
        )
        assertFalse(sinDireccion.esValido)
    }
}
