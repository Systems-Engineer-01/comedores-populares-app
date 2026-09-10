package com.comedorespopulares.registro.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.comedorespopulares.registro.data.GeminiClient
import com.comedorespopulares.registro.data.model.DatosDniAnverso
import com.comedorespopulares.registro.data.model.DatosDniReverso
import com.comedorespopulares.registro.util.Validators
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Estado completo de la Socia en el flujo de registro (Sprint 2 + Sprint 3).
 */
data class RegistroSociaUiState(
    // ───── ANVERSO (Sprint 2) ─────
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

    // ───── PREGUNTAS (Sprint 3) ─────
    val esGestante: Boolean = false,
    val tieneDiscapacidad: Boolean = false,
    val pasoPreguntasCompletado: Boolean = false,

    // ───── REVERSO (Sprint 3) ─────
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

    // ───── TIPO BENEFICIARIO (Sprint 3) ─────
    val tipoBeneficiario: String = "1"    // Fijado automáticamente en "1" (Socia, PRD secc 2 paso 5)
) {
    /** Valida los campos del anverso */
    val esAnversoValido: Boolean
        get() = Validators.esDniValido(dni) &&
                Validators.noEstaVacio(apellidoPaterno) &&
                Validators.noEstaVacio(apellidoMaterno) &&
                Validators.noEstaVacio(nombres)

    val requiereRevisionVisualAnverso: Boolean
        get() = confianzaAnverso == "baja" || !esAnversoValido

    /** Valida que los campos obligatorios del reverso (dirección y distrito) no queden vacíos */
    val esReversoValido: Boolean
        get() = direccion.isNotBlank() && distrito.isNotBlank()

    val requiereRevisionVisualReverso: Boolean
        get() = confianzaReverso == "baja" || !esReversoValido

    // Retrocompatibilidad con nombres de campos de Sprint 2
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

// Tipo alias de retrocompatibilidad
typealias CapturaDniAnversoUiState = RegistroSociaUiState

/**
 * ViewModel que administra el estado y las reglas de negocio del registro de la Socia.
 */
class RegistroSociaViewModel(
    private val geminiClient: GeminiClient = GeminiClient()
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegistroSociaUiState())
    val uiState: StateFlow<RegistroSociaUiState> = _uiState.asStateFlow()

    // ───── SPRINT 2: ANVERSO DNI ─────

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

    // ───── SPRINT 3: PREGUNTAS (GESTANTE / DISCAPACIDAD) ─────

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

    // ───── SPRINT 3: REVERSO DNI (DIRECCIÓN / DISTRITO) ─────

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

                // Aplicar regla de negocio pure function: centroPoblado = direccion
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
        _uiState.update { it.copy(pasoReversoCompletado = true) }
    }

    // Resetters
    fun resetPasoAnversoCompletado() {
        _uiState.update { it.copy(pasoAnversoCompletado = false) }
    }

    fun resetPasoPreguntasCompletado() {
        _uiState.update { it.copy(pasoPreguntasCompletado = false) }
    }

    fun resetPasoReversoCompletado() {
        _uiState.update { it.copy(pasoReversoCompletado = false) }
    }

    fun resetPasoCompletado() {
        resetPasoAnversoCompletado()
    }

    fun reiniciarFormulario() {
        _uiState.value = RegistroSociaUiState()
    }
}
