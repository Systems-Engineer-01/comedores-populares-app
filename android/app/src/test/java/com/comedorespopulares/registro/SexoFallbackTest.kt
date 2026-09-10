package com.comedorespopulares.registro

import com.comedorespopulares.registro.util.SexoFallback
import org.junit.Assert.*
import org.junit.Test

class SexoFallbackTest {

    @Test
    fun `inferirSexo identifica correctamente nombres femeninos comunes`() {
        assertEquals("F", SexoFallback.inferirSexo("MARIA ELENA"))
        assertEquals("F", SexoFallback.inferirSexo("Ana Paula"))
        assertEquals("F", SexoFallback.inferirSexo("ROSA ISABEL"))
        assertEquals("F", SexoFallback.inferirSexo("CARMEN SOFIA"))
    }

    @Test
    fun `inferirSexo identifica correctamente nombres masculinos comunes`() {
        assertEquals("M", SexoFallback.inferirSexo("JOSE LUIS"))
        assertEquals("M", SexoFallback.inferirSexo("Juan Carlos"))
        assertEquals("M", SexoFallback.inferirSexo("PEDRO ALBERTO"))
        assertEquals("M", SexoFallback.inferirSexo("CARLOS DANIEL"))
    }

    @Test
    fun `inferirSexo devuelve null para nombres ambiguos o no registrados`() {
        assertNull(SexoFallback.inferirSexo("ALEX"))
        assertNull(SexoFallback.inferirSexo("ROBIN"))
        assertNull(SexoFallback.inferirSexo("RENE"))
        assertNull(SexoFallback.inferirSexo(""))
    }
}
