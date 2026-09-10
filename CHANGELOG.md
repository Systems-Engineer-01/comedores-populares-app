# Registro Digital — Comedores Populares: CHANGELOG

Este documento registra los cambios, correcciones de errores, casos borde cubiertos y consideraciones técnicas de cada Sprint en la aplicación Android y Backend.

---

## Sprint 7 — Hardening, Manejo de Errores y Testeo Unitario

**Fecha:** 2026-09-10  
**Objetivo:** Robustecer la aplicación ante fallos de cámara, fotos ilegibles, problemas de conectividad con Gemini/Backend y validar reglas de negocio mediante pruebas unitarias completas.

### 1. Manejo de Errores de Cámara y Permisos
- **Permiso de Cámara Denegado:**
  - Se implementó una tarjeta informativa destacada (`AnimatedVisibility`) cuando la aplicación no cuenta con permisos de cámara (`Manifest.permission.CAMERA`).
  - Se agregó un botón **"Abrir Ajustes de la App"** que invoca directamente la pantalla de configuración del sistema Android (`Settings.ACTION_APPLICATION_DETAILS_SETTINGS`) con el URI del paquete (`package:com.comedorespopulares.registro`).
- **Fotos Borrosas u Oscuras:**
  - Si Gemini API devuelve `confianza: "baja"` en el anverso o reverso, la interfaz presenta un banner de advertencia claro: `"Foto borrosa u oscura - sugerimos repetir la foto o corregir los datos"`.
  - Se añadieron dos botones de acción inmediata: **"Reintentar Foto"** (abre nuevamente la cámara en vivo) y **"Reintentar Extracción"** (vuelve a enviar la imagen en caché a Gemini API sin obligar a tomar una nueva foto).

### 2. Manejo de Errores de Red y Reintentos
- **Fallo en Gemini AI (Timeout u HTTP 5xx):**
  - Al ocurrir un error de red con la API de Gemini, la app no borra el formulario ni la foto previamente seleccionada/capturada.
  - El URI de la última foto tomada se almacena en el estado (`ultimaImagenAnversoUri` / `ultimaImagenReversoUri`) para permitir un reintento limpio con el botón **"Reintentar Extracción"**.
- **Fallo en Backend de Apps Script (`doPost`):**
  - En la pantalla de resumen (`ResumenRegistroScreen.kt`), si el envío secuencial falla por problemas de conectividad o respuesta del backend, los datos de todos los integrantes de la familia se conservan intactos.
  - El botón de envío cambia automáticamente a **"Reintentar Envío al Backend"**, permitiendo reintentar la operación sin solicitar nuevamente el llenado del registro.

### 3. Validaciones Pre-Flight (Antes del Envío)
- **Validación Espejo con Backend Apps Script:**
  - Se implementó `Validators.validarParaBackend(persona)`, el cual replica fielmente la función `validar(data)` de `backend/Code.gs`:
    1. DNI de 8 dígitos numéricos (`^\d{8}$`).
    2. Apellido Paterno no vacío.
    3. Nombres no vacíos.
    4. Tipo Beneficiario en `{"1", "2"}`.
    5. Dirección y Distrito no vacíos.
  - `ResumenRegistroScreen.kt` ejecuta esta pre-validación antes de habilitar el botón de envío y despliega un bloque descriptivo rojo en caso de detectar campos incompletos o DNI inválidos en cualquier integrante.

### 4. Cobertura de Pruebas Unitarias (JUnit 4)
Se creó e integró una suite completa de pruebas unitarias aisladas en `app/src/test/java/com/comedorespopulares/registro/`:
1. `BackendClientTest.kt`:
   - Pruebas de `aplicarApoderado` para copiar DNI/Apellidos/Nombres de la Socia a los Hijos.
   - Verificación de que Socia y Pareja mantienen apoderado vacío `""`.
2. `DatosDniReversoTest.kt`:
   - Pruebas de `mapearCentroPoblado` (replicación limpia de dirección sin espacios sobrantes).
   - Verificación de `esValido` para el reverso del DNI.
3. `RegistroSociaViewModelTest.kt`:
   - Pruebas de la regla `tienePareja(estadoCivil)` para todas las opciones de estado civil declaradas.
4. `SexoFallbackTest.kt`:
   - Pruebas de la heurística de inferencia de sexo basada en el primer nombre (nombres femeninos, masculinos y ambiguos).
5. `ValidatorsTest.kt`:
   - Pruebas de la expresión regular de DNI, validadores de texto no vacío y validación completa `validarParaBackend`.

### 5. Riesgos Conocidos y Recomendaciones para el Piloto (Sprint 8)
1. **Conectividad a Internet en Terreno:**
   - La API de Gemini exige conexión HTTP a internet. En comedores de zonas periféricas o con señal móvil débil, se recomienda asegurar buena señal o usar conexión Wi-Fi/Hotspot durante las pruebas del piloto.
2. **Cuota y Límites de la API Key de Gemini:**
   - Para el piloto masivo, asegurarse de que la `GEMINI_API_KEY` tenga cuotas suficientes habilitadas en Google Cloud Console para evitar errores HTTP 429 (Rate Limit).
3. **Impresión de DNI Desgastados:**
   - DNI muy desgastados o con reflejos de luz solar pueden generar `confianza: "baja"`. La presidenta siempre cuenta con la opción de editar manualmente los campos autocompletados.

---

## Sprints Anteriores

- **Sprint 6:** Apoderado automático para hijos, pantalla de resumen familiar, envío secuencial con `LockService` a Apps Script y pantalla de confirmación.
- **Sprint 5:** Flujo de N Hijos (0..15), componente reutilizable `CapturaPersonaScreen`, regla de sexo/gestante y preguntas de condición física.
- **Sprint 4:** Selección de Estado Civil, regla `tienePareja`, flujo condicional para pareja con asignación automática de `tipoBeneficiario = "2"`.
- **Sprint 3:** Preguntas de Gestante y Discapacidad, extracción de reverso de DNI (Dirección, Distrito) y mapeo de Centro Poblado.
- **Sprint 2:** Extracción de anverso de DNI vía Gemini Visión, pre-llenado editable, alertas visuales por baja confianza y validación local de DNI.
- **Sprint 1:** Backend Google Apps Script (`Code.gs`) con `doPost`, `LockService` y prevención de duplicados de DNI.
- **Sprint 0:** PRD, arquitectura, prompts de extracción y esquema de Google Sheets.
