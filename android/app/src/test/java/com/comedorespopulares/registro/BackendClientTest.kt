package com.comedorespopulares.registro

import com.comedorespopulares.registro.data.model.Persona
import com.comedorespopulares.registro.data.remote.BackendClient
import org.junit.Assert.assertEquals
import org.junit.Test

class BackendClientTest {

    private val backendClient = BackendClient()

    private val sociaPrueba = Persona(
        dni = "08765432",
        apellidoPaterno = "QUISPE",
        apellidoMaterno = "MAMANI",
        nombres = "MARIA ELENA",
        sexo = "F",
        gestante = "No",
        discapacidad = "No",
        direccion = "AV. LOS INCAS 123",
        distrito = "SAN JUAN DE LURIGANCHO",
        tipoBeneficiario = "1",
        rol = "Socia"
    )

    @Test
    fun `aplicarApoderado para Hijo copia datos de Socia`() {
        val hijoPrueba = Persona(
            dni = "77665544",
            apellidoPaterno = "QUISPE",
            apellidoMaterno = "GONZALES",
            nombres = "JUAN CARLOS",
            sexo = "M",
            gestante = "No",
            discapacidad = "No",
            direccion = "AV. LOS INCAS 123",
            distrito = "SAN JUAN DE LURIGANCHO",
            tipoBeneficiario = "2",
            rol = "Hijo"
        )

        val hijoConApoderado = backendClient.aplicarApoderado(hijoPrueba, sociaPrueba)

        assertEquals("08765432", hijoConApoderado.apoderadoDni)
        assertEquals("QUISPE", hijoConApoderado.apoderadoApellidoPaterno)
        assertEquals("MAMANI", hijoConApoderado.apoderadoApellidoMaterno)
        assertEquals("MARIA ELENA", hijoConApoderado.apoderadoNombres)
    }

    @Test
    fun `aplicarApoderado para Socia y Pareja deja apoderado vacio`() {
        val parejaPrueba = Persona(
            dni = "01234567",
            apellidoPaterno = "GONZALES",
            apellidoMaterno = "RODRIGUEZ",
            nombres = "PEDRO",
            sexo = "M",
            gestante = "",
            discapacidad = "No",
            direccion = "AV. LOS INCAS 123",
            distrito = "SAN JUAN DE LURIGANCHO",
            tipoBeneficiario = "2",
            rol = "Pareja"
        )

        val sociaConApoderado = backendClient.aplicarApoderado(sociaPrueba, sociaPrueba)
        val parejaConApoderado = backendClient.aplicarApoderado(parejaPrueba, sociaPrueba)

        assertEquals("", sociaConApoderado.apoderadoDni)
        assertEquals("", sociaConApoderado.apoderadoApellidoPaterno)
        assertEquals("", sociaConApoderado.apoderadoApellidoMaterno)
        assertEquals("", sociaConApoderado.apoderadoNombres)

        assertEquals("", parejaConApoderado.apoderadoDni)
        assertEquals("", parejaConApoderado.apoderadoApellidoPaterno)
        assertEquals("", parejaConApoderado.apoderadoApellidoMaterno)
        assertEquals("", parejaConApoderado.apoderadoNombres)
    }
}
