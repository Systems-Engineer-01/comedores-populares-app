package com.comedorespopulares.registro

import android.app.Application

/**
 * Clase Application principal.
 * Puede usarse para inicializar SDKs o configuraciones globales.
 */
class ComedoresApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Inicializaciones globales si se necesitan en el futuro
        // (analytics, crash reporting, etc.)
    }
}
