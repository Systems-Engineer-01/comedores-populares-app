package com.comedorespopulares.registro.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.comedorespopulares.registro.ui.components.LoadingOverlay
import com.comedorespopulares.registro.ui.screens.*
import com.comedorespopulares.registro.ui.viewmodel.PersonaTarget
import com.comedorespopulares.registro.ui.viewmodel.RegistroStep
import com.comedorespopulares.registro.ui.viewmodel.RegistroViewModel

/**
 * Grafo de navegación basado en la máquina de estados del ViewModel.
 *
 * En lugar de usar NavHost con rutas, usamos un when exhaustivo sobre
 * RegistroStep — esto simplifica la navegación ya que el flujo es
 * estrictamente lineal y controlado por el ViewModel.
 */
@Composable
fun RegistroNavGraph(
    viewModel: RegistroViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    when (val step = state.step) {
        // ───── Inicio ─────
        is RegistroStep.Inicio -> {
            HomeScreen(
                onRegistrarClick = { viewModel.iniciarRegistro() },
                modifier = modifier
            )
        }

        // ───── Socia: captura DNI anverso (paso 1 - Sprint 2) ─────
        is RegistroStep.CapturaDniAnversoSocia -> {
            val sociaViewModel: com.comedorespopulares.registro.ui.viewmodel.RegistroSociaViewModel =
                androidx.lifecycle.viewmodel.compose.viewModel()

            CapturaDniAnversoScreen(
                viewModel = sociaViewModel,
                onConfirmarSuccess = {
                    val datosConfirmados = sociaViewModel.uiState.value.datosConfirmados
                    if (datosConfirmados != null) {
                        viewModel.confirmarDatosSocia(
                            dni = datosConfirmados.dni,
                            apellidoPaterno = datosConfirmados.apellidoPaterno,
                            apellidoMaterno = datosConfirmados.apellidoMaterno,
                            nombres = datosConfirmados.nombres,
                            sexo = datosConfirmados.sexo,
                            gestante = "No",
                            discapacidad = "No"
                        )
                    }
                },
                modifier = modifier
            )
        }

        // ───── Socia: preguntas Gestante/Discapacidad (paso 2-3 - Sprint 3) ─────
        is RegistroStep.FormularioSocia -> {
            val sociaViewModel: com.comedorespopulares.registro.ui.viewmodel.RegistroSociaViewModel =
                androidx.lifecycle.viewmodel.compose.viewModel()

            PreguntasSociaScreen(
                viewModel = sociaViewModel,
                onContinuarSuccess = {
                    val stateSocia = sociaViewModel.uiState.value
                    viewModel.confirmarDatosSocia(
                        dni = stateSocia.dni,
                        apellidoPaterno = stateSocia.apellidoPaterno,
                        apellidoMaterno = stateSocia.apellidoMaterno,
                        nombres = stateSocia.nombres,
                        sexo = stateSocia.sexo,
                        gestante = if (stateSocia.esGestante) "Sí" else "No",
                        discapacidad = if (stateSocia.tieneDiscapacidad) "Sí" else "No"
                    )
                },
                modifier = modifier
            )
        }

        // ───── Socia: captura DNI reverso (paso 4 - Sprint 3) ─────
        is RegistroStep.CapturaReversoSocia -> {
            val sociaViewModel: com.comedorespopulares.registro.ui.viewmodel.RegistroSociaViewModel =
                androidx.lifecycle.viewmodel.compose.viewModel()

            CapturaDniReversoScreen(
                viewModel = sociaViewModel,
                onConfirmarSuccess = {
                    val stateSocia = sociaViewModel.uiState.value
                    viewModel.confirmarDireccionSocia(
                        direccion = stateSocia.direccion,
                        distrito = stateSocia.distrito
                    )
                },
                modifier = modifier
            )
        }

        // ───── Estado civil (paso 6) ─────
        is RegistroStep.EstadoCivil -> {
            EstadoCivilScreen(
                onEstadoSeleccionado = { viewModel.seleccionarEstadoCivil(it) },
                modifier = modifier
            )
        }

        // ───── Pareja: captura DNI anverso (paso 7) ─────
        is RegistroStep.CapturaDniAnversoPareja -> {
            SociaDniScreen(  // Reutilizamos el mismo componente
                imagenUri = state.imagenActual,
                isLoading = state.isLoading,
                onImagenCapturada = { uri ->
                    viewModel.procesarAnverso(uri, PersonaTarget.PAREJA)
                },
                modifier = modifier
            )
        }

        // ───── Pareja: formulario (paso 8) ─────
        is RegistroStep.FormularioPareja -> {
            ParejaFormScreen(
                datosExtraidos = state.parejaAnverso,
                onConfirmar = { dni, apP, apM, nombres, discapacidad ->
                    viewModel.confirmarDatosPareja(dni, apP, apM, nombres, discapacidad)
                },
                modifier = modifier
            )
        }

        // ───── Pareja: captura reverso (paso 9) ─────
        is RegistroStep.CapturaReversoPareja -> {
            ReversoScreen(
                titulo = "Reverso DNI — Pareja",
                imagenUri = state.imagenActual,
                isLoading = state.isLoading,
                datosExtraidos = state.parejaReverso,
                onImagenCapturada = { uri ->
                    viewModel.procesarReverso(uri, PersonaTarget.PAREJA)
                },
                onConfirmar = { direccion, distrito ->
                    viewModel.confirmarDireccionPareja(direccion, distrito)
                },
                modifier = modifier
            )
        }

        // ───── Hijos: cantidad (paso 11) ─────
        is RegistroStep.CantidadHijos -> {
            HijosCountScreen(
                onCantidadConfirmada = { viewModel.establecerCantidadHijos(it) },
                modifier = modifier
            )
        }

        // ───── Hijo: captura DNI anverso (paso 12a) ─────
        is RegistroStep.CapturaDniAnversoHijo -> {
            SociaDniScreen(  // Reutilizamos
                imagenUri = state.imagenActual,
                isLoading = state.isLoading,
                onImagenCapturada = { uri ->
                    viewModel.procesarAnverso(uri, PersonaTarget.HIJO)
                },
                modifier = modifier
            )
        }

        // ───── Hijo: formulario (pasos 12b-12f) ─────
        is RegistroStep.FormularioHijo -> {
            val sexoDetectado = viewModel.determinarSexoHijo(
                state.hijoAnversoActual?.sexo ?: "",
                state.hijoAnversoActual?.nombres ?: ""
            )
            HijoFormScreen(
                indice = step.indice,
                totalHijos = state.cantidadHijos,
                datosExtraidos = state.hijoAnversoActual,
                sexoDetectado = sexoDetectado,
                apoderadoNombre = "${state.sociaApellidoPaterno} ${state.sociaApellidoMaterno}, ${state.sociaNombres} — DNI: ${state.sociaDni}",
                onConfirmar = { dni, apP, apM, nombres, sexo, gestante, discapacidad ->
                    viewModel.confirmarDatosHijo(
                        step.indice, dni, apP, apM, nombres, sexo, gestante, discapacidad
                    )
                },
                modifier = modifier
            )
        }

        // ───── Hijo: captura reverso (paso 12d) ─────
        is RegistroStep.CapturaReversoHijo -> {
            ReversoScreen(
                titulo = "Reverso DNI — Hijo ${step.indice + 1}",
                imagenUri = state.imagenActual,
                isLoading = state.isLoading,
                datosExtraidos = state.hijoReversoActual,
                onImagenCapturada = { uri ->
                    viewModel.procesarReverso(uri, PersonaTarget.HIJO)
                },
                onConfirmar = { direccion, distrito ->
                    viewModel.confirmarDireccionHijo(step.indice, direccion, distrito)
                },
                modifier = modifier
            )
        }

        // ───── Resumen ─────
        is RegistroStep.Resumen -> {
            ResumenScreen(
                sociaDni = state.sociaDni,
                sociaNombre = "${state.sociaApellidoPaterno} ${state.sociaApellidoMaterno}, ${state.sociaNombres}",
                tienePareja = state.tienePareja,
                parejaDni = state.parejaDni,
                parejaNombre = "${state.parejaApellidoPaterno} ${state.parejaApellidoMaterno}, ${state.parejaNombres}",
                hijos = state.hijos,
                onEnviar = { viewModel.enviarRegistro() },
                modifier = modifier
            )
        }

        // ───── Enviando ─────
        is RegistroStep.Enviando -> {
            LoadingOverlay(mensaje = "Enviando datos al servidor…")
        }

        // ───── Completado ─────
        is RegistroStep.Completado -> {
            ResultadoScreen(
                exitoso = true,
                numeros = step.numeros,
                onNuevoRegistro = { viewModel.reiniciar() },
                modifier = modifier
            )
        }

        // ───── Error ─────
        is RegistroStep.Error -> {
            ResultadoScreen(
                exitoso = false,
                errorMensaje = step.mensaje,
                onNuevoRegistro = { viewModel.reiniciar() },
                onReintentar = { viewModel.enviarRegistro() },
                modifier = modifier
            )
        }
    }
}
