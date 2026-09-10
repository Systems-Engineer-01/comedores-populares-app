package com.comedorespopulares.registro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.ui.Modifier
import com.comedorespopulares.registro.ui.navigation.RegistroNavGraph
import com.comedorespopulares.registro.ui.theme.ComedoresPopularesTheme
import com.comedorespopulares.registro.ui.viewmodel.RegistroViewModel

/**
 * Activity principal — punto de entrada de la app.
 * Usa Jetpack Compose con el tema de Comedores Populares
 * y delega toda la navegación al RegistroNavGraph.
 */
class MainActivity : ComponentActivity() {

    private val viewModel: RegistroViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ComedoresPopularesTheme {
                RegistroNavGraph(
                    viewModel = viewModel,
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding()
                )
            }
        }
    }
}
