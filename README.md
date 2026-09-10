# Registro Digital — Comedores Populares

App Android para que las presidentas de comedores populares registren socias, cónyuges e hijos
a partir de fotos de su DNI, con extracción automática de datos vía Gemini 3.1 Pro y almacenamiento
centralizado en Google Sheets sin colisiones entre dispositivos.

## Estructura del repo

```
/docs        → PRD.md (especificación completa, backlog Scrum, esquema de datos)
/prompts     → Prompts listos para Gemini 3.1 Pro (extracción de DNI)
/backend     → Code.gs — backend en Google Apps Script (LockService, sin duplicados)
/android     → App Kotlin/Compose (Sprints 2+)
  ├── app/src/main/java/com/comedorespopulares/registro/
  │   ├── data/          → Modelos, API Gemini, API backend, Repositorio
  │   ├── ui/            → Theme, componentes, pantallas, ViewModel
  │   └── util/          → Validadores, fallback de sexo
  └── app/src/main/res/  → Strings, colores, XML configs
```

## Configuración inicial

1. Lee `docs/PRD.md` completo (contiene el flujo, el esquema de Sheets y el backlog por sprint).
2. Despliega `backend/Code.gs` como Web App en el Google Sheet real (ver comentario al inicio del archivo).
3. Copia `android/local.properties.example` a `android/local.properties` y completa:
   - `GEMINI_API_KEY` — tu API key de Gemini (https://aistudio.google.com/apikey)
   - `BACKEND_URL` — la URL del Web App de Apps Script desplegado
4. Abre la carpeta `android/` en Android Studio y ejecuta.

## Arquitectura

```
[App Android - Kotlin + Jetpack Compose]
   ├─ Camera/Gallery → captura foto del DNI
   ├─ GeminiDniExtractor (imagen + prompt JSON) → datos extraídos
   ├─ Validación local (regex DNI, campos no vacíos)
   ├─ RegistroViewModel (máquina de estados sealed classes)
   └─ Retrofit HTTPS → Google Apps Script Web App (backend)

[Backend - Google Apps Script Web App]
   ├─ doPost(e) recibe JSON
   ├─ LockService.getScriptLock() → sin colisiones
   ├─ Valida duplicado de DNI
   ├─ appendRow() en Registro_Oficial + Interno_Control
   └─ Responde { ok: true, numero: N }

[Google Sheets] — Registro_Oficial + Interno_Control
```

## Estado actual

- [x] Sprint 0 — PRD, esquema de datos, arquitectura definida
- [x] Sprint 1 — Backend Apps Script (escritura segura, sin duplicados)
- [x] Sprint 2 — Pantalla de captura + extracción Gemini (Socia)
- [x] Sprint 3 — Gestante/Discapacidad/Dirección
- [x] Sprint 4 — Estado civil + flujo de pareja
- [x] Sprint 5 — Flujo de hijos (N iteraciones)
- [x] Sprint 6 — Apoderado automático + envío secuencial
- [ ] Sprint 7 — QA + hardening
- [ ] Sprint 8 — Piloto

## Changelog

- **Sprint 0**: Setup inicial del repositorio, definición del PRD (`docs/PRD.md`), especificación de arquitectura centralizada, esquema de datos para Google Sheets (`Registro_Oficial` e `Interno_Control`), prompts de visión para extracción de DNI (`prompts/extraccion_dni.md`) y `.gitignore`.
- **Sprint 1**: Backend de datos en Google Apps Script (`backend/Code.gs`) implementando `doPost`, manejo de concurrencia con `LockService`, validación de duplicados de DNI y respuesta JSON en formato Web App.
- **Sprint 2**: Implementada la pantalla de captura `CapturaDniAnversoScreen.kt` + integración HTTP directa con Gemini Visión (`GeminiClient.kt`), parseo a `DatosDniAnverso`, campos 100% editables con pre-llenado, resaltado de advertencia visual en rojo para baja confianza y campos vacíos, validación local de DNI de 8 dígitos, retención de datos en `RegistroSociaViewModel.kt` y eliminación automática de fotos de memoria/caché tras su uso por privacidad (Ley N.° 29733).
- **Sprint 3**: Implementada la pantalla `PreguntasSociaScreen.kt` (Gestante y Discapacidad), extracción del reverso del DNI en `CapturaDniReversoScreen.kt` llamando a `GeminiClient.extraerDniReverso`, aplicación de regla de negocio `DatosDniReverso.mapearCentroPoblado` (función pura), asignación automática de `tipoBeneficiario = "1"` y validación de dirección y distrito obligatorios.
- **Sprint 4**: Implementada la pantalla `EstadoCivilScreen.kt` con opciones y campo libre para "Otro", aviso explícito de uso interno, función pura testeable `RegistroSociaViewModel.tienePareja(estadoCivil)`, flujo condicional reutilizando los componentes de DNI para Pareja (preguntando solo Discapacidad, sin Gestante), asignación automática de `tipoBeneficiario = "2"` y modelo de estado unificado `RegistroFamiliaState` con `idFamilia` en formato UUID.
- **Sprint 5**: Implementada la pantalla `NumeroHijosScreen.kt` con rango 0..15 e indicador visual, componente reutilizable `CapturaPersonaScreen.kt` parametrizado por `RolPersona` (SOCIA, PAREJA, HIJO), regla de preguntas condicionales (Gestante SI Y SOLO SI sexo es "F", con fallback heurístico y confirmación manual directiva), loop de N iteraciones con indicador de progreso ("Hijo 2 de 3") y asignación automática de `tipoBeneficiario = "2"` para cada hijo.
- **Sprint 6**: Implementada la regla de negocio `BackendClient.aplicarApoderado(persona, socia)` (función pura) que copia DNI y nombres completos de la Socia a los campos de apoderado de los Hijos (dejando vacíos los de Socia y Pareja), la pantalla de resumen `ResumenRegistroScreen.kt` para revisión de todos los integrantes, el cliente `BackendClient.kt` con envío secuencial respetando `LockService` y mapeo exacto al esquema de `doPost` con ID de dispositivo estable, manejo de errores detallado por integrante sin pérdida de datos, y la pantalla de confirmación final `RegistroExitosoScreen.kt` con los números de fila oficiales asignados ("Registrado con N° X").





