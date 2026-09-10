package com.comedorespopulares.registro.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.comedorespopulares.registro.data.GeminiClient
import com.comedorespopulares.registro.data.model.DatosDniAnverso
import com.comedorespopulares.registro.data.model.DatosDniReverso
import com.comedorespopulares.registro.data.model.Persona
import com.comedorespopulares.registro.data.model.RegistroFamiliaState
import com.comedorespopulares.registro.util.Validators
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Estado completo de la Socia y la Familia en el flujo de registro (Sprint 2 al Sprint 5).
 */
data class RegistroSociaUiState(
    // ID único de familia para Interno_Control (Sprint 4)
    val idFamilia: String = RegistroFamiliaState.generarNuevoIdFamilia(),

    // ───── ANVERSO SOCIA (Sprint 2) ─────
    val dni: String = "",
    val apellidoPaterno: String = "",
    val apellidoMaterno: String = "",
    val nombres: String = "",
    val sexo: String = "F",              // Por definición del rol Socia
    val confianzaAnverso: String = "alta",
    val isLoadingAnverso: Boolean = false,
    val errorMessageAnverso: String? = null,
    val advertenciaAnverso: String? = null,
    val pasoAnversoCompletado: Boolean = false,

    // ───── PREGUNTAS SOCIA (Sprint 3) ─────
    val esGestante: Boolean = false,
    val tieneDiscapacidad: Boolean = false,
    val pasoPreguntasCompletado: Boolean = false,

    // ───── REVERSO SOCIA (Sprint 3) ─────
    val direccion: String = "",
    val distrito: String = "",
    val provincia: String = "",
    val departamento: String = "",
    val centroPoblado: String = "",       // Replicado de direccion
    val confianzaReverso: String = "alta",
    val isLoadingReverso: Boolean = false,
    val errorMessageReverso: String? = null,
    val advertenciaReverso: String? = null,
    val pasoReversoCompletado: Boolean = false,

    // ───── TIPO BENEFICIARIO SOCIA (Sprint 3) ─────
    val tipoBeneficiario: String = "1",   // Fijado automáticamente en "1" (Socia, PRD secc 2 paso 5)

    // ───── ESTADO CIVIL Y PAREJA (Sprint 4) ─────
    val estadoCivilDeclarado: String = "",
    val tienePareja: Boolean = false,
    val pasoEstadoCivilCompletado: Boolean = false,

    // Datos Pareja (Sprint 4)
    val parejaDni: String = "",
    val parejaApellidoPaterno: String = "",
    val parejaApellidoMaterno: String = "",
    val parejaNombres: String = "",
    val parejaSexo: String = "M",        // Rol Pareja (varón según PRD)
    val parejaDiscapacidad: Boolean = false,
    val parejaDireccion: String = "",
    val parejaDistrito: String = "",
    val parejaTipoBeneficiario: String = "2", // Automático "2" (Usuario, PRD secc 2 paso 10)
    val pasoParejaCompletado: Boolean = false,

    // ───── LOOP DE HIJOS (Sprint 5) ─────
    val cantidadHijos: Int = 0,
    val hijoIndiceActual: Int = 0,
    val pasoCantidadHijosCompletado: Boolean = false,
    val loopHijosCompletado: Boolean = false,

    // Estado Global Familiar
    val familiaState: RegistroFamiliaState = RegistroFamiliaState(idFamilia = idFamilia)
) {
    val esAnversoValido: Boolean
        get() = Validators.esDniValido(dni) &&
                Validators.noEstaVacio(apellidoPaterno) &&
                Validators.noEstaVacio(apellidoMaterno) &&
                Validators.noEstaVacio(nombres)

    val requiereRevisionVisualAnverso: Boolean
        get() = confianzaAnverso == "baja" || !esAnversoValido

    val esReversoValido: Boolean
        get() = direccion.isNotBlank() && distrito.isNotBlank()

    val requiereRevisionVisualReverso: Boolean
        get() = confianzaReverso == "baja" || !esReversoValido

    val esParejaAnversoValido: Boolean
        get() = Validators.esDniValido(parejaDni) &&
                Validators.noEstaVacio(parejaApellidoPaterno) &&
                Validators.noEstaVacio(parejaApellidoMaterno) &&
                Validators.noEstaVacio(parejaNombres)

    val esParejaReversoValido: Boolean
        get() = parejaDireccion.isNotBlank() && parejaDistrito.isNotBlank()

    // Retrocompatibilidad
    val confianza: String get() = confianzaAnverso
    val isLoading: Boolean get() = isLoadingAnverso
    val errorMessage: String? get() = errorMessageAnverso
    val advertencia: String? get() = advertenciaAnverso
    val pasoCompletado: Boolean get() = pasoAnversoCompletado
    val esValido: Boolean get() = esAnversoValido
    val requiereRevisionVisual: Boolean get() = requiereRevisionVisualAnverso
    val datosConfirmados: DatosDniAnverso?
        get() = if (pasoAnversoCompletado) DatosDniAnverso(dni, apellidoPaterno, apellidoMaterno, nombres, sexo, confianzaAnverso) else null
}

/**
 * ViewModel que administra el estado y las reglas de negocio del registro de la Socia y la Familia.
 */
class RegistroSociaViewModel(
    private val geminiClient: GeminiClient = GeminiClient()
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegistroSociaUiState())
    val uiState: StateFlow<RegistroSociaUiState> = _uiState.asStateFlow()

    // ───── SPRINT 4: REGLA DE NEGOCIO aislada y testable ─────

    /**
     * Evalúa si una socia activa el flujo de pareja según su estado civil declarado.
     * Devuelve `true` para cualquier valor que NO sea "Ninguna de las anteriores" ni vacío.
     */
    fun tienePareja(estadoCivil: String): Boolean {
        val limpio = estadoCivil.trim()
        return limpio.isNotBlank() && !limpio.equals("Ninguna de las anteriores", ignoreCase = true)
    }

    // ───── SPRINT 2: ANVERSO DNI SOCIA ─────

    fun procesarImagenDniAnverso(context: Context, uri: Uri) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoadingAnverso = true,
                    errorMessageAnverso = null,
                    advertenciaAnverso = null
                )
            }

            val resultado = geminiClient.extraerDniAnverso(context, uri)

            resultado.onSuccess { datos ->
                val advertenciaTexto = if (datos.confianza == "baja") {
                    "Confianza de lectura baja. Por favor verifique los campos resaltados."
                } else null

                _uiState.update { current ->
                    current.copy(
                        dni = datos.dni,
                        apellidoPaterno = datos.apellidoPaterno,
                        apellidoMaterno = datos.apellidoMaterno,
                        nombres = datos.nombres,
                        sexo = if (datos.sexo.isNotBlank()) datos.sexo else "F",
                        confianzaAnverso = datos.confianza,
                        isLoadingAnverso = false,
                        advertenciaAnverso = advertenciaTexto,
                        errorMessageAnverso = null
                    )
                }
            }.onFailure { exception ->
                _uiState.update { current ->
                    current.copy(
                        isLoadingAnverso = false,
                        errorMessageAnverso = "Error al procesar el anverso con Gemini: ${exception.localizedMessage ?: "Error desconocido"}.",
                        confianzaAnverso = "baja",
                        advertenciaAnverso = "Por favor ingrese los datos a mano."
                    )
                }
            }
        }
    }

    fun onDniChange(nuevoDni: String) {
        val dniFiltrado = nuevoDni.filter { it.isDigit() }.take(8)
        _uiState.update { it.copy(dni = dniFiltrado) }
    }

    fun onApellidoPaternoChange(nuevoApellido: String) {
        _uiState.update { it.copy(apellidoPaterno = nuevoApellido.uppercase()) }
    }

    fun onApellidoMaternoChange(nuevoApellido: String) {
        _uiState.update { it.copy(apellidoMaterno = nuevoApellido.uppercase()) }
    }

    fun onNombresChange(nuevosNombres: String) {
        _uiState.update { it.copy(nombres = nuevosNombres.uppercase()) }
    }

    fun onSexoChange(nuevoSexo: String) {
        _uiState.update { it.copy(sexo = nuevoSexo.uppercase().take(1)) }
    }

    fun confirmarDatosAnverso() {
        if (!_uiState.value.esAnversoValido) return
        _uiState.update { it.copy(pasoAnversoCompletado = true) }
    }

    // ───── SPRINT 3: PREGUNTAS SOCIA (GESTANTE / DISCAPACIDAD) ─────

    fun setGestante(esGestante: Boolean) {
        _uiState.update { it.copy(esGestante = esGestante) }
    }

    fun setDiscapacidad(tieneDiscapacidad: Boolean) {
        _uiState.update { it.copy(tieneDiscapacidad = tieneDiscapacidad) }
    }

    fun confirmarPreguntasSocia() {
        _uiState.update {
            it.copy(
                tipoBeneficiario = "1", // Automático para la Socia (PRD Sección 2 Paso 5)
                pasoPreguntasCompletado = true
            )
        }
    }

    // ───── SPRINT 3: REVERSO DNI SOCIA ─────

    fun procesarImagenDniReverso(context: Context, uri: Uri) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoadingReverso = true,
                    errorMessageReverso = null,
                    advertenciaReverso = null
                )
            }

            val resultado = geminiClient.extraerDniReverso(context, uri)

            resultado.onSuccess { datos ->
                val advertenciaTexto = if (datos.confianza == "baja") {
                    "Confianza de lectura baja en reverso. Verifique la dirección y distrito."
                } else null

                val cpMapeado = DatosDniReverso.mapearCentroPoblado(datos.direccion)

                _uiState.update { current ->
                    current.copy(
                        direccion = datos.direccion,
                        distrito = datos.distrito,
                        provincia = datos.provincia,
                        departamento = datos.departamento,
                        centroPoblado = cpMapeado,
                        confianzaReverso = datos.confianza,
                        isLoadingReverso = false,
                        advertenciaReverso = advertenciaTexto,
                        errorMessageReverso = null
                    )
                }
            }.onFailure { exception ->
                _uiState.update { current ->
                    current.copy(
                        isLoadingReverso = false,
                        errorMessageReverso = "Error al procesar el reverso con Gemini: ${exception.localizedMessage ?: "Error desconocido"}.",
                        confianzaReverso = "baja",
                        advertenciaReverso = "Por favor ingrese la dirección y distrito a mano."
                    )
                }
            }
        }
    }

    fun onDireccionChange(nuevaDireccion: String) {
        val direccionUpper = nuevaDireccion.uppercase()
        val cp = DatosDniReverso.mapearCentroPoblado(direccionUpper)
        _uiState.update { it.copy(direccion = direccionUpper, centroPoblado = cp) }
    }

    fun onDistritoChange(nuevoDistrito: String) {
        _uiState.update { it.copy(distrito = nuevoDistrito.uppercase()) }
    }

    fun onProvinciaChange(nuevaProvincia: String) {
        _uiState.update { it.copy(provincia = nuevaProvincia.uppercase()) }
    }

    fun onDepartamentoChange(nuevoDepartamento: String) {
        _uiState.update { it.copy(departamento = nuevoDepartamento.uppercase()) }
    }

    fun confirmarDatosReverso() {
        if (!_uiState.value.esReversoValido) return

        val sociaPersona = Persona(
            dni = _uiState.value.dni,
            apellidoPaterno = _uiState.value.apellidoPaterno,
            apellidoMaterno = _uiState.value.apellidoMaterno,
            nombres = _uiState.value.nombres,
            sexo = _uiState.value.sexo,
            gestante = if (_uiState.value.esGestante) "Sí" else "No",
            discapacidad = if (_uiState.value.tieneDiscapacidad) "Sí" else "No",
            direccion = _uiState.value.direccion,
            distrito = _uiState.value.distrito,
            tipoBeneficiario = "1",
            rol = "Socia"
        )

        _uiState.update { current ->
            current.copy(
                pasoReversoCompletado = true,
                familiaState = current.familiaState.copy(socia = sociaPersona)
            )
        }
    }

    // ───── SPRINT 4: ESTADO CIVIL Y PAREJA ─────

    fun seleccionarEstadoCivil(estadoCivil: String) {
        val activaPareja = tienePareja(estadoCivil)
        _uiState.update { current ->
            current.copy(
                estadoCivilDeclarado = estadoCivil,
                tienePareja = activaPareja,
                pasoEstadoCivilCompletado = true,
                familiaState = current.familiaState.copy(estadoCivilDeclarado = estadoCivil)
            )
        }
    }

    fun setParejaDiscapacidad(discapacidad: Boolean) {
        _uiState.update { it.copy(parejaDiscapacidad = discapacidad) }
    }

    fun onParejaDniChange(nuevoDni: String) {
        val dniFiltrado = nuevoDni.filter { it.isDigit() }.take(8)
        _uiState.update { it.copy(parejaDni = dniFiltrado) }
    }

    fun onParejaApellidoPaternoChange(nuevoApellido: String) {
        _uiState.update { it.copy(parejaApellidoPaterno = nuevoApellido.uppercase()) }
    }

    fun onParejaApellidoMaternoChange(nuevoApellido: String) {
        _uiState.update { it.copy(parejaApellidoMaterno = nuevoApellido.uppercase()) }
    }

    fun onParejaNombresChange(nuevosNombres: String) {
        _uiState.update { it.copy(parejaNombres = nuevosNombres.uppercase()) }
    }

    fun onParejaDireccionChange(nuevaDireccion: String) {
        _uiState.update { it.copy(parejaDireccion = nuevaDireccion.uppercase()) }
    }

    fun onParejaDistritoChange(nuevoDistrito: String) {
        _uiState.update { it.copy(parejaDistrito = nuevoDistrito.uppercase()) }
    }

    fun confirmarDatosPareja() {
        val state = _uiState.value
        val parejaPersona = Persona(
            dni = state.parejaDni,
            apellidoPaterno = state.parejaApellidoPaterno,
            apellidoMaterno = state.parejaApellidoMaterno,
            nombres = state.parejaNombres,
            sexo = "M",                  // Varón (PRD)
            gestante = "",               // No aplica gestante para varón
            discapacidad = if (state.parejaDiscapacidad) "Sí" else "No",
            direccion = state.parejaDireccion,
            distrito = state.parejaDistrito,
            tipoBeneficiario = "2",       // Automático "2" (Usuario, PRD secc 2 paso 10)
            rol = "Pareja"
        )

        _uiState.update { current ->
            current.copy(
                pasoParejaCompletado = true,
                familiaState = current.familiaState.copy(pareja = parejaPersona)
            )
        }
    }

    // ───── SPRINT 5: LOOP DE N HIJOS ─────

    fun establecerCantidadHijos(cantidad: Int) {
        _uiState.update { current ->
            current.copy(
                cantidadHijos = cantidad,
                hijoIndiceActual = 0,
                pasoCantidadHijosCompletado = true,
                loopHijosCompletado = (cantidad == 0)
            )
        }
    }

    fun agregarHijoYAvanzar(hijo: Persona) {
        _uiState.update { current ->
            // Asegurar tipoBeneficiario = "2" automático para hijo
            val hijoConTipo = hijo.copy(
                tipoBeneficiario = "2",
                rol = "Hijo"
            )

            val listaHijosActualizada = current.familiaState.hijos.toMutableList().apply {
                add(hijoConTipo)
            }

            val siguienteIndice = current.hijoIndiceActual + 1
            val completado = siguienteIndice >= current.cantidadHijos

            current.copy(
                hijoIndiceActual = siguienteIndice,
                loopHijosCompletado = completado,
                familiaState = current.familiaState.copy(hijos = listaHijosActualizada)
            )
        }
    }

    // Resetters
    fun resetPasoAnversoCompletado() { _uiState.update { it.copy(pasoAnversoCompletado = false) } }
    fun resetPasoPreguntasCompletado() { _uiState.update { it.copy(pasoPreguntasCompletado = false) } }
    fun resetPasoReversoCompletado() { _uiState.update { it.copy(pasoReversoCompletado = false) } }
    fun resetPasoEstadoCivilCompletado() { _uiState.update { it.copy(pasoEstadoCivilCompletado = false) } }
    fun resetPasoParejaCompletado() { _uiState.update { it.copy(pasoParejaCompletado = false) } }
    fun resetPasoCantidadHijosCompletado() { _uiState.update { it.copy(pasoCantidadHijosCompletado = false) } }
    fun resetPasoCompletado() { resetPasoAnversoCompletado() }

    fun reiniciarFormulario() {
        val nuevoId = RegistroFamiliaState.generarNuevoIdFamilia()
        _uiState.value = RegistroSociaUiState(idFamilia = nuevoId)
    }
}
