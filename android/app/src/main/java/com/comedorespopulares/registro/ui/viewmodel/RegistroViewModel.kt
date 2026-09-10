package com.comedorespopulares.registro.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.comedorespopulares.registro.data.model.DniAnversoResult
import com.comedorespopulares.registro.data.model.DniReversoResult
import com.comedorespopulares.registro.data.model.Persona
import com.comedorespopulares.registro.data.repository.RegistroRepository
import com.comedorespopulares.registro.util.SexoFallback
import com.comedorespopulares.registro.util.Validators
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel principal que implementa la máquina de estados del flujo completo
 * descrito en la sección 2 del PRD.
 *
 * Flujo: Socia → [Pareja (condicional)] → Hijos (N iteraciones) → Resumen → Envío
 *
 * Usa sealed classes para representar cada paso y un StateFlow para la UI.
 */
class RegistroViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = RegistroRepository()

    // ───── Estado principal ─────

    private val _uiState = MutableStateFlow(RegistroUiState())
    val uiState: StateFlow<RegistroUiState> = _uiState.asStateFlow()

    // IDs generados al iniciar un nuevo registro
    private var idFamilia: String = ""
    private var presidentaId: String = ""

    // ───── Acciones del flujo ─────

    /** Inicia un nuevo registro de socia. */
    fun iniciarRegistro() {
        idFamilia = repository.generarIdFamilia()
        presidentaId = repository.obtenerPresidentaId()
        _uiState.value = RegistroUiState(step = RegistroStep.CapturaDniAnversoSocia)
    }

    /** Reinicia todo el estado para un nuevo registro. */
    fun reiniciar() {
        _uiState.value = RegistroUiState()
    }

    // ───── Extracción de DNI ─────

    /** Procesa la imagen del anverso del DNI. */
    fun procesarAnverso(uri: Uri, target: PersonaTarget) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, imagenActual = uri) }
            val bitmap = cargarBitmap(uri) ?: run {
                _uiState.update { it.copy(isLoading = false, errorMensaje = "No se pudo cargar la imagen") }
                return@launch
            }

            val resultado = repository.extraerAnverso(bitmap)
            _uiState.update { state ->
                when (target) {
                    PersonaTarget.SOCIA -> state.copy(
                        isLoading = false,
                        anversoActual = resultado,
                        sociaAnverso = resultado,
                        step = RegistroStep.FormularioSocia
                    )
                    PersonaTarget.PAREJA -> state.copy(
                        isLoading = false,
                        anversoActual = resultado,
                        parejaAnverso = resultado,
                        step = RegistroStep.FormularioPareja
                    )
                    PersonaTarget.HIJO -> state.copy(
                        isLoading = false,
                        anversoActual = resultado,
                        hijoAnversoActual = resultado,
                        step = RegistroStep.FormularioHijo(state.hijoIndiceActual)
                    )
                }
            }
        }
    }

    /** Procesa la imagen del reverso del DNI. */
    fun procesarReverso(uri: Uri, target: PersonaTarget) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, imagenActual = uri) }
            val bitmap = cargarBitmap(uri) ?: run {
                _uiState.update { it.copy(isLoading = false, errorMensaje = "No se pudo cargar la imagen") }
                return@launch
            }

            val resultado = repository.extraerReverso(bitmap)
            _uiState.update { state ->
                when (target) {
                    PersonaTarget.SOCIA -> state.copy(
                        isLoading = false,
                        reversoActual = resultado,
                        sociaReverso = resultado
                    )
                    PersonaTarget.PAREJA -> state.copy(
                        isLoading = false,
                        reversoActual = resultado,
                        parejaReverso = resultado
                    )
                    PersonaTarget.HIJO -> state.copy(
                        isLoading = false,
                        reversoActual = resultado,
                        hijoReversoActual = resultado
                    )
                }
            }
        }
    }

    // ───── Formulario Socia (pasos 2–5 del PRD) ─────

    fun confirmarDatosSocia(
        dni: String,
        apellidoPaterno: String,
        apellidoMaterno: String,
        nombres: String,
        sexo: String,
        gestante: String,
        discapacidad: String
    ) {
        val error = Validators.validarPersona(
            dni, apellidoPaterno, nombres, "1",
            _uiState.value.sociaReverso?.direccion ?: "pendiente",
            _uiState.value.sociaReverso?.distrito ?: "pendiente"
        )
        if (error != null && error.contains("Dirección").not() && error.contains("Distrito").not()) {
            _uiState.update { it.copy(errorMensaje = error) }
            return
        }

        _uiState.update {
            it.copy(
                sociaDni = dni,
                sociaApellidoPaterno = apellidoPaterno,
                sociaApellidoMaterno = apellidoMaterno,
                sociaNombres = nombres,
                sociaSexo = sexo,
                sociaGestante = gestante,
                sociaDiscapacidad = discapacidad,
                step = RegistroStep.CapturaReversoSocia,
                errorMensaje = null
            )
        }
    }

    fun confirmarDireccionSocia(direccion: String, distrito: String) {
        _uiState.update {
            it.copy(
                sociaDireccion = direccion,
                sociaDistrito = distrito,
                step = RegistroStep.EstadoCivil
            )
        }
    }

    // ───── Estado civil (paso 6 del PRD) ─────

    fun seleccionarEstadoCivil(estadoCivil: String) {
        val tienePareja = estadoCivil.isNotBlank() &&
                estadoCivil != "Ninguna de las anteriores" &&
                estadoCivil != ""

        _uiState.update {
            it.copy(
                estadoCivil = estadoCivil,
                tienePareja = tienePareja,
                step = if (tienePareja)
                    RegistroStep.CapturaDniAnversoPareja
                else
                    RegistroStep.CantidadHijos
            )
        }
    }

    // ───── Pareja (pasos 7–10 del PRD) ─────

    fun confirmarDatosPareja(
        dni: String,
        apellidoPaterno: String,
        apellidoMaterno: String,
        nombres: String,
        discapacidad: String
    ) {
        _uiState.update {
            it.copy(
                parejaDni = dni,
                parejaApellidoPaterno = apellidoPaterno,
                parejaApellidoMaterno = apellidoMaterno,
                parejaNombres = nombres,
                parejaDiscapacidad = discapacidad,
                step = RegistroStep.CapturaReversoPareja
            )
        }
    }

    fun confirmarDireccionPareja(direccion: String, distrito: String) {
        _uiState.update {
            it.copy(
                parejaDireccion = direccion,
                parejaDistrito = distrito,
                step = RegistroStep.CantidadHijos
            )
        }
    }

    // ───── Hijos (pasos 11–12 del PRD) ─────

    fun establecerCantidadHijos(cantidad: Int) {
        _uiState.update {
            it.copy(
                cantidadHijos = cantidad,
                hijos = MutableList(cantidad) { Persona() },
                hijoIndiceActual = 0,
                step = if (cantidad > 0)
                    RegistroStep.CapturaDniAnversoHijo(0)
                else
                    RegistroStep.Resumen
            )
        }
    }

    fun confirmarDatosHijo(
        indice: Int,
        dni: String,
        apellidoPaterno: String,
        apellidoMaterno: String,
        nombres: String,
        sexo: String,
        gestante: String,
        discapacidad: String
    ) {
        _uiState.update { state ->
            val hijosActualizados = state.hijos.toMutableList()
            hijosActualizados[indice] = hijosActualizados[indice].copy(
                dni = dni,
                apellidoPaterno = apellidoPaterno,
                apellidoMaterno = apellidoMaterno,
                nombres = nombres,
                sexo = sexo,
                gestante = gestante,
                discapacidad = discapacidad,
                tipoBeneficiario = "2",
                rol = "Hijo",
                // Apoderado = datos de la Socia (Sprint 6, automático)
                apoderadoDni = state.sociaDni,
                apoderadoApellidoPaterno = state.sociaApellidoPaterno,
                apoderadoApellidoMaterno = state.sociaApellidoMaterno,
                apoderadoNombres = state.sociaNombres
            )
            state.copy(
                hijos = hijosActualizados,
                step = RegistroStep.CapturaReversoHijo(indice)
            )
        }
    }

    fun confirmarDireccionHijo(indice: Int, direccion: String, distrito: String) {
        _uiState.update { state ->
            val hijosActualizados = state.hijos.toMutableList()
            hijosActualizados[indice] = hijosActualizados[indice].copy(
                direccion = direccion,
                distrito = distrito
            )

            val siguienteIndice = indice + 1
            val siguienteStep = if (siguienteIndice < state.cantidadHijos)
                RegistroStep.CapturaDniAnversoHijo(siguienteIndice)
            else
                RegistroStep.Resumen

            state.copy(
                hijos = hijosActualizados,
                hijoIndiceActual = siguienteIndice,
                step = siguienteStep
            )
        }
    }

    /**
     * Determina el sexo del hijo. Usa el campo del DNI primero;
     * si viene vacío, usa fallback de nombres.
     * Si ni eso funciona, devuelve null → la UI debe preguntar.
     */
    fun determinarSexoHijo(sexoDni: String, nombres: String): String? {
        if (sexoDni.isNotBlank()) return sexoDni
        return SexoFallback.inferirSexo(nombres)
    }

    // ───── Envío al backend (paso 13 del PRD) ─────

    fun enviarRegistro() {
        viewModelScope.launch {
            _uiState.update { it.copy(step = RegistroStep.Enviando) }

            val state = _uiState.value

            // Construir persona Socia
            val socia = Persona(
                dni = state.sociaDni,
                apellidoPaterno = state.sociaApellidoPaterno,
                apellidoMaterno = state.sociaApellidoMaterno,
                nombres = state.sociaNombres,
                sexo = state.sociaSexo,
                gestante = state.sociaGestante,
                discapacidad = state.sociaDiscapacidad,
                direccion = state.sociaDireccion,
                distrito = state.sociaDistrito,
                tipoBeneficiario = "1",
                rol = "Socia"
            )

            // Construir persona Pareja (si aplica)
            val pareja = if (state.tienePareja) {
                Persona(
                    dni = state.parejaDni,
                    apellidoPaterno = state.parejaApellidoPaterno,
                    apellidoMaterno = state.parejaApellidoMaterno,
                    nombres = state.parejaNombres,
                    gestante = "",  // No se pregunta gestante al varón
                    discapacidad = state.parejaDiscapacidad,
                    direccion = state.parejaDireccion,
                    distrito = state.parejaDistrito,
                    tipoBeneficiario = "2",
                    rol = "Pareja"
                )
            } else null

            val resultado = repository.registrarFamiliaCompleta(
                socia = socia,
                pareja = pareja,
                hijos = state.hijos,
                estadoCivil = state.estadoCivil,
                idFamilia = idFamilia,
                presidentaId = presidentaId
            )

            resultado.fold(
                onSuccess = { numeros ->
                    _uiState.update {
                        it.copy(
                            step = RegistroStep.Completado(numeros),
                            errorMensaje = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            step = RegistroStep.Error(
                                error.message ?: "Error desconocido"
                            )
                        )
                    }
                }
            )
        }
    }

    fun limpiarError() {
        _uiState.update { it.copy(errorMensaje = null) }
    }

    // ───── Utilidades internas ─────

    private fun cargarBitmap(uri: Uri): Bitmap? {
        return try {
            val inputStream = getApplication<Application>()
                .contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            null
        }
    }
}

// ───── Modelos de estado ─────

/**
 * Paso actual del flujo de registro.
 * Sealed classes para exhaustive when en la UI.
 */
sealed class RegistroStep {
    object Inicio : RegistroStep()

    // Socia
    object CapturaDniAnversoSocia : RegistroStep()
    object FormularioSocia : RegistroStep()
    object CapturaReversoSocia : RegistroStep()
    object EstadoCivil : RegistroStep()

    // Pareja
    object CapturaDniAnversoPareja : RegistroStep()
    object FormularioPareja : RegistroStep()
    object CapturaReversoPareja : RegistroStep()

    // Hijos
    object CantidadHijos : RegistroStep()
    data class CapturaDniAnversoHijo(val indice: Int) : RegistroStep()
    data class FormularioHijo(val indice: Int) : RegistroStep()
    data class CapturaReversoHijo(val indice: Int) : RegistroStep()

    // Fin
    object Resumen : RegistroStep()
    object Enviando : RegistroStep()
    data class Completado(val numeros: List<Int>) : RegistroStep()
    data class Error(val mensaje: String) : RegistroStep()
}

/** Identifica a quién pertenece la operación actual. */
enum class PersonaTarget {
    SOCIA, PAREJA, HIJO
}

/**
 * Estado completo de la UI del registro.
 * Contiene todos los datos parciales de socia, pareja e hijos.
 */
data class RegistroUiState(
    val step: RegistroStep = RegistroStep.Inicio,
    val isLoading: Boolean = false,
    val errorMensaje: String? = null,
    val imagenActual: Uri? = null,

    // Resultados de extracción temporales
    val anversoActual: DniAnversoResult? = null,
    val reversoActual: DniReversoResult? = null,

    // ── Socia ──
    val sociaAnverso: DniAnversoResult? = null,
    val sociaReverso: DniReversoResult? = null,
    val sociaDni: String = "",
    val sociaApellidoPaterno: String = "",
    val sociaApellidoMaterno: String = "",
    val sociaNombres: String = "",
    val sociaSexo: String = "",
    val sociaGestante: String = "",
    val sociaDiscapacidad: String = "No",
    val sociaDireccion: String = "",
    val sociaDistrito: String = "",

    // ── Estado civil ──
    val estadoCivil: String = "",
    val tienePareja: Boolean = false,

    // ── Pareja ──
    val parejaAnverso: DniAnversoResult? = null,
    val parejaReverso: DniReversoResult? = null,
    val parejaDni: String = "",
    val parejaApellidoPaterno: String = "",
    val parejaApellidoMaterno: String = "",
    val parejaNombres: String = "",
    val parejaDiscapacidad: String = "No",
    val parejaDireccion: String = "",
    val parejaDistrito: String = "",

    // ── Hijos ──
    val cantidadHijos: Int = 0,
    val hijoIndiceActual: Int = 0,
    val hijos: List<Persona> = emptyList(),
    val hijoAnversoActual: DniAnversoResult? = null,
    val hijoReversoActual: DniReversoResult? = null
)
