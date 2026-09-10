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

        // ───── Estado civil (paso 6 - Sprint 4) ─────
        is RegistroStep.EstadoCivil -> {
            val sociaViewModel: com.comedorespopulares.registro.ui.viewmodel.RegistroSociaViewModel =
                androidx.lifecycle.viewmodel.compose.viewModel()

            EstadoCivilScreen(
                onEstadoCivilSeleccionado = { estadoCivilFinal, tienePareja ->
                    sociaViewModel.seleccionarEstadoCivil(estadoCivilFinal)
                    viewModel.seleccionarEstadoCivil(estadoCivilFinal)
                },
                modifier = modifier
            )
        }

        // ───── Pareja: captura DNI anverso (paso 7 - Sprint 4 reutilizado) ─────
        is RegistroStep.CapturaDniAnversoPareja -> {
            val sociaViewModel: com.comedorespopulares.registro.ui.viewmodel.RegistroSociaViewModel =
                androidx.lifecycle.viewmodel.compose.viewModel()

            CapturaDniAnversoScreen(
                viewModel = sociaViewModel,
                onConfirmarSuccess = {
                    val stateSocia = sociaViewModel.uiState.value
                    viewModel.confirmarDatosPareja(
                        dni = stateSocia.dni,
                        apellidoPaterno = stateSocia.apellidoPaterno,
                        apellidoMaterno = stateSocia.apellidoMaterno,
                        nombres = stateSocia.nombres,
                        discapacidad = "No"
                    )
                },
                modifier = modifier
            )
        }

        // ───── Pareja: preguntas Discapacidad (paso 8 - Sprint 4 sin gestante) ─────
        is RegistroStep.FormularioPareja -> {
            val sociaViewModel: com.comedorespopulares.registro.ui.viewmodel.RegistroSociaViewModel =
                androidx.lifecycle.viewmodel.compose.viewModel()
            val stateSocia by sociaViewModel.uiState.collectAsState()

            PreguntasParejaScreen(
                tieneDiscapacidad = stateSocia.parejaDiscapacidad,
                onDiscapacidadChange = { sociaViewModel.setParejaDiscapacidad(it) },
                onContinuar = {
                    viewModel.confirmarDatosPareja(
                        dni = stateSocia.dni,
                        apellidoPaterno = stateSocia.apellidoPaterno,
                        apellidoMaterno = stateSocia.apellidoMaterno,
                        nombres = stateSocia.nombres,
                        discapacidad = if (stateSocia.parejaDiscapacidad) "Sí" else "No"
                    )
                },
                modifier = modifier
            )
        }

        // ───── Pareja: captura reverso (paso 9 - Sprint 4 reutilizado) ─────
        is RegistroStep.CapturaReversoPareja -> {
            val sociaViewModel: com.comedorespopulares.registro.ui.viewmodel.RegistroSociaViewModel =
                androidx.lifecycle.viewmodel.compose.viewModel()

            CapturaDniReversoScreen(
                viewModel = sociaViewModel,
                onConfirmarSuccess = {
                    val stateSocia = sociaViewModel.uiState.value
                    sociaViewModel.confirmarDatosPareja()
                    viewModel.confirmarDireccionPareja(
                        direccion = stateSocia.direccion,
                        distrito = stateSocia.distrito
                    )
                },
                modifier = modifier
            )
        }

        // ───── Hijos: cantidad (paso 11 - Sprint 5) ─────
        is RegistroStep.CantidadHijos -> {
            val sociaViewModel: com.comedorespopulares.registro.ui.viewmodel.RegistroSociaViewModel =
                androidx.lifecycle.viewmodel.compose.viewModel()

            NumeroHijosScreen(
                onCantidadConfirmada = { cantidad ->
                    sociaViewModel.establecerCantidadHijos(cantidad)
                    viewModel.establecerCantidadHijos(cantidad)
                },
                modifier = modifier
            )
        }

        // ───── Hijo: captura persona en loop de N (pasos 12a-12f - Sprint 5 reutilizado) ─────
        is RegistroStep.CapturaDniAnversoHijo, is RegistroStep.FormularioHijo, is RegistroStep.CapturaReversoHijo -> {
            val sociaViewModel: com.comedorespopulares.registro.ui.viewmodel.RegistroSociaViewModel =
                androidx.lifecycle.viewmodel.compose.viewModel()
            val stateSocia by sociaViewModel.uiState.collectAsState()

            CapturaPersonaScreen(
                viewModel = sociaViewModel,
                rol = com.comedorespopulares.registro.data.model.RolPersona.HIJO,
                indiceHijo = stateSocia.hijoIndiceActual,
                totalHijos = stateSocia.cantidadHijos,
                onPersonaCapturada = { personaHijo ->
                    sociaViewModel.agregarHijoYAvanzar(personaHijo)
                    viewModel.confirmarDatosHijo(
                        indice = stateSocia.hijoIndiceActual,
                        dni = personaHijo.dni,
                        apellidoPaterno = personaHijo.apellidoPaterno,
                        apellidoMaterno = personaHijo.apellidoMaterno,
                        nombres = personaHijo.nombres,
                        sexo = personaHijo.sexo,
                        gestante = personaHijo.gestante,
                        discapacidad = personaHijo.discapacidad
                    )
                    viewModel.confirmarDireccionHijo(
                        indice = stateSocia.hijoIndiceActual,
                        direccion = personaHijo.direccion,
                        distrito = personaHijo.distrito
                    )
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
