package com.comedorespopulares.registro

import com.comedorespopulares.registro.data.model.Persona
import com.comedorespopulares.registro.util.Validators
import org.junit.Assert.*
import org.junit.Test

class ValidatorsTest {

    @Test
    fun `esDniValido valida exactamente 8 digitos numericos`() {
        assertTrue(Validators.esDniValido("12345678"))
        assertTrue(Validators.esDniValido("00112233"))

        assertFalse(Validators.esDniValido("1234567"))   // 7 dígitos
        assertFalse(Validators.esDniValido("123456789")) // 9 dígitos
        assertFalse(Validators.esDniValido("1234567A"))  // Con letras
        assertFalse(Validators.esDniValido(""))          // Vacío
    }

    @Test
    fun `noEstaVacio verifica cadenas con texto real`() {
        assertTrue(Validators.noEstaVacio("Hola"))
        assertFalse(Validators.noEstaVacio(""))
        assertFalse(Validators.noEstaVacio("   "))
    }

    @Test
    fun `esTipoBeneficiarioValido acepta 1 2 y 3`() {
        assertTrue(Validators.esTipoBeneficiarioValido("1"))
        assertTrue(Validators.esTipoBeneficiarioValido("2"))
        assertTrue(Validators.esTipoBeneficiarioValido("3"))
        assertFalse(Validators.esTipoBeneficiarioValido("4"))
        assertFalse(Validators.esTipoBeneficiarioValido("0"))
    }

    @Test
    fun `validarParaBackend retorna null si la persona cumple todos los campos`() {
        val personaValida = Persona(
            dni = "08765432",
            apellidoPaterno = "QUISPE",
            apellidoMaterno = "MAMANI",
            nombres = "MARIA",
            sexo = "F",
            direccion = "AV. PERU 123",
            distrito = "VENTANILLA",
            tipoBeneficiario = "1",
            rol = "Socia"
        )
        assertNull(Validators.validarParaBackend(personaValida))
    }

    @Test
    fun `validarParaBackend retorna mensaje de error especifico si falta algun campo`() {
        val personaSinDni = Persona(
            dni = "123",
            apellidoPaterno = "QUISPE",
            nombres = "MARIA",
            direccion = "AV. PERU 123",
            distrito = "VENTANILLA",
            tipoBeneficiario = "1",
            rol = "Socia"
        )
        assertNotNull(Validators.validarParaBackend(personaSinDni))
        assertTrue(Validators.validarParaBackend(personaSinDni)!!.contains("DNI inválido"))

        val personaSinNombre = Persona(
            dni = "12345678",
            apellidoPaterno = "QUISPE",
            nombres = "",
            direccion = "AV. PERU 123",
            distrito = "VENTANILLA",
            tipoBeneficiario = "1",
            rol = "Socia"
        )
        assertNotNull(Validators.validarParaBackend(personaSinNombre))
        assertTrue(Validators.validarParaBackend(personaSinNombre)!!.contains("Faltan Nombres"))
    }
}
