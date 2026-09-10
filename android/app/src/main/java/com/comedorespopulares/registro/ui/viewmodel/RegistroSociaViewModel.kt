package com.comedorespopulares.registro.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.comedorespopulares.registro.data.GeminiClient
import com.comedorespopulares.registro.data.model.DatosDniAnverso
import com.comedorespopulares.registro.util.Validators
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Estado UI de la pantalla de captura y edición de DNI Anverso de la Socia (Sprint 2).
 */
data class CapturaDniAnversoUiState(
    val dni: String = "",
    val apellidoPaterno: String = "",
    val apellidoMaterno: String = "",
    val nombres: String = "",
    val sexo: String = "",
    val confianza: String = "alta",     // "alta" | "media" | "baja"
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val advertencia: String? = null,
    val datosConfirmados: DatosDniAnverso? = null,
    val pasoCompletado: Boolean = false
) {
    /** Valida que el DNI tenga 8 dígitos (regex) y los 4 campos principales no estén vacíos */
    val esValido: Boolean
        get() = Validators.esDniValido(dni) &&
                Validators.noEstaVacio(apellidoPaterno) &&
                Validators.noEstaVacio(apellidoMaterno) &&
                Validators.noEstaVacio(nombres)

    /** Indica si se debe mostrar advertencia visual (borde rojo / banner de atención) */
    val requiereRevisionVisual: Boolean
        get() = confianza == "baja" ||
                !Validators.esDniValido(dni) ||
                apellidoPaterno.isBlank() ||
                apellidoMaterno.isBlank() ||
                nombres.isBlank()
}

/**
 * ViewModel para el registro de la Socia y la extracción del DNI Anverso.
 * Retiene el estado del flujo sin enviar al backend todavía (se envía al final de todo el flujo).
 */
class RegistroSociaViewModel(
    private val geminiClient: GeminiClient = GeminiClient()
) : ViewModel() {

    private val _uiState = MutableStateFlow(CapturaDniAnversoUiState())
    val uiState: StateFlow<CapturaDniAnversoUiState> = _uiState.asStateFlow()

    /**
     * Procesa la imagen capturada/seleccionada del DNI Anverso mediante Gemini 2.0 Flash.
     */
    fun procesarImagenDniAnverso(context: Context, uri: Uri) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    advertencia = null
                )
            }

            val resultado = geminiClient.extraerDniAnverso(context, uri)

            resultado.onSuccess { datos ->
                val advertenciaTexto = if (datos.confianza == "baja") {
                    "Confianza de lectura baja. Por favor verifique y corrija los campos resaltados."
                } else null

                _uiState.update { current ->
                    current.copy(
                        dni = datos.dni,
                        apellidoPaterno = datos.apellidoPaterno,
                        apellidoMaterno = datos.apellidoMaterno,
                        nombres = datos.nombres,
                        sexo = datos.sexo,
                        confianza = datos.confianza,
                        isLoading = false,
                        advertencia = advertenciaTexto,
                        errorMessage = null
                    )
                }
            }.onFailure { exception ->
                _uiState.update { current ->
                    current.copy(
                        isLoading = false,
                        errorMessage = "Error al procesar la foto con Gemini: ${exception.localizedMessage ?: "Error desconocido"}. Ingrese los datos manualmente.",
                        confianza = "baja",
                        advertencia = "No se pudo autocompletar. Por favor llene los campos a mano."
                    )
                }
            }
        }
    }

    fun onDniChange(nuevoDni: String) {
        // Filtrar solo dígitos y máximo 8 caracteres
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

    /**
     * Guarda el resultado de este paso en el estado local del flujo
     * (SIN enviar nada al backend todavía — ocurre al final de todo el registro de la socia).
     */
    fun confirmarDatosAnverso() {
        val currentState = _uiState.value
        if (!currentState.esValido) return

        val datosGuardados = DatosDniAnverso(
            dni = currentState.dni,
            apellidoPaterno = currentState.apellidoPaterno,
            apellidoMaterno = currentState.apellidoMaterno,
            nombres = currentState.nombres,
            sexo = currentState.sexo,
            confianza = currentState.confianza
        )

        _uiState.update {
            it.copy(
                datosConfirmados = datosGuardados,
                pasoCompletado = true
            )
        }
    }

    fun resetPasoCompletado() {
        _uiState.update { it.copy(pasoCompletado = false) }
    }

    fun reiniciarFormulario() {
        _uiState.value = CapturaDniAnversoUiState()
    }
}
