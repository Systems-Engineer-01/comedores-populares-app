package com.comedorespopulares.registro

import com.comedorespopulares.registro.ui.viewmodel.RegistroSociaViewModel
import org.junit.Assert.*
import org.junit.Test

class RegistroSociaViewModelTest {

    private val viewModel = RegistroSociaViewModel()

    @Test
    fun `tienePareja devuelve true para estados civiles con pareja`() {
        assertTrue(viewModel.tienePareja("Casada"))
        assertTrue(viewModel.tienePareja("Acompañada"))
        assertTrue(viewModel.tienePareja("Soltera"))
        assertTrue(viewModel.tienePareja("Viuda"))
        assertTrue(viewModel.tienePareja("Divorciada"))
        assertTrue(viewModel.tienePareja("Otro: Conviviente de hecho"))
    }

    @Test
    fun `tienePareja devuelve false para Ninguna de las anteriores o vacio`() {
        assertFalse(viewModel.tienePareja("Ninguna de las anteriores"))
        assertFalse(viewModel.tienePareja("ninguna de las anteriores"))
        assertFalse(viewModel.tienePareja(""))
        assertFalse(viewModel.tienePareja("   "))
    }
}
