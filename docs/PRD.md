# PRD — App Android "Registro Digital de Comedores Populares"
Gestor de proyecto: Claude · Ejecución de código: Antigravity + Gemini 3.1 Pro High
Última actualización: 2026-09-10

## 1. Hallazgo clave (bloqueante) antes de codear

Probé Tesseract OCR contra tu foto real de DNI (front y zona MRZ). Resultado: **falla**, incluso con
preprocesamiento (umbral Otsu, adaptativo, whitelist de caracteres). Causas: brillo/reflejo del plástico,
fondo con degradado, fuente tipo OCR-B de la MRZ que el modelo `eng`/`spa` de Tesseract no reconoce bien,
y fotos tomadas con celular en ángulo.

**Decisión de arquitectura:** no usar Tesseract/MRZ-regex como método principal. Usar **Gemini 3.1 Pro
(visión multimodal) como motor de extracción**, enviando la imagen del DNI + un prompt con schema JSON
estricto. Gemini generaliza mucho mejor a fotos imperfectas que un pipeline OCR clásico, y ya está en tu
stack (Antigravity). Ver `/prompts/extraccion_dni.md`.

Tesseract queda solo como **fallback opcional offline** si en el futuro se necesita funcionar sin conexión.

## 2. Flujo funcional completo (validado contigo)

```
INICIO → Registrar SOCIA
 1. Subir foto/PDF DNI (anverso) → extraer DNI, Ap. Paterno, Ap. Materno, Nombres
 2. Preguntar: ¿Gestante? (Sí/No)
 3. Preguntar: ¿Discapacidad? (Sí/No)
 4. Subir foto DNI (reverso) → extraer Dirección completa, Distrito
    → Centro Poblado = mismo valor que Dirección (el DNI peruano no trae centro poblado)
 5. Tipo de Beneficiario = "1" (automático, Socia)
 6. Preguntar estado civil (uso INTERNO, no va en la plantilla oficial):
    Soltera / Casada / Viuda / Divorciada / Acompañada / Ninguna de las anteriores / Otro (texto libre)
    → si NO es "Ninguna de las anteriores" ni vacío → activar flujo de CÓNYUGE/PAREJA

 SI TIENE PAREJA (según estado civil):
 7. Subir DNI de la pareja → extraer DNI, Ap. Paterno, Ap. Materno, Nombres
 8. Preguntar Discapacidad (Sí/No). NO preguntar Gestante (varón).
 9. Subir reverso → Dirección, Centro Poblado (= Dirección), Distrito
 10. Tipo de Beneficiario = "2" (automático, Usuario)

 SIEMPRE (toda socia registra hijos):
 11. Preguntar número de hijos (N)
 12. Repetir N veces:
     a. Subir DNI del hijo/a → extraer DNI, Ap. Paterno, Ap. Materno, Nombres
     b. Detectar sexo (campo "Sexo" del DNI o heurística de nombre) → si es mujer, SÍ preguntar
        Gestante; si es varón, NO preguntar Gestante (regla que diste aplica igual por sexo, no solo al hijo)
     c. Preguntar Discapacidad (Sí/No)
     d. Subir reverso → Dirección, Centro Poblado (= Dirección), Distrito
     e. Tipo de Beneficiario = "2" (automático)
     f. Datos del Apoderado (DNI, Ap. Paterno, Ap. Materno, Nombres) = se copian automáticamente de la SOCIA
 13. Al completar los N hijos → FIN del registro de esa socia
```

**Nota de negocio (confirmada):**
- Tipo 1 = Socia (cocina).
- Tipo 2 = Usuario (pareja e hijos).
- Estado civil **no existe en el formato oficial** que piden los agentes/supervisores → se guarda en una
  hoja/tabla interna separada, nunca mezclada con las columnas oficiales (ver sección 4).

## 3. Riesgo que agregué (no mencionado, pero crítico)

Dijiste "no quiero que se encimen los datos" y por eso definieron un campo N°. **Ese campo por sí solo NO
evita colisiones** si dos presidentas registran al mismo tiempo desde dos celulares distintos y cada app
calcula el siguiente N° localmente (ej. "última fila + 1"): ambas pueden calcular el mismo número y
sobrescribirse. Esto es un problema clásico de concurrencia.

**Solución:** el N° nunca lo calcula el celular. Lo asigna un backend central (Google Apps Script Web App)
usando `LockService`, que:
1. Recibe el JSON con los datos ya extraídos/validados desde el celular.
2. Toma un lock exclusivo sobre la hoja.
3. Calcula el siguiente N° real, hace `appendRow`, libera el lock.
4. Devuelve el N° asignado a la app (para mostrar "Registrado con N° 128" en pantalla).

Esto también te da: un solo punto de validación (evita duplicar el mismo DNI dos veces), logging de qué
dispositivo/presidenta registró qué fila, y evita exponer credenciales de Google Sheets dentro del APK.

## 4. Esquema de datos (Google Sheets)

### Hoja "Registro_Oficial" (esto es lo que ven agentes/supervisores — igual a tu plantilla)

| Col | Campo |
|---|---|
| A | N° (asignado por backend) |
| B | DNI |
| C | Apellido Paterno |
| D | Apellido Materno |
| E | Nombres |
| F | Gestante (Sí/No/—) |
| G | Discapacidad (Sí/No) |
| H | Dirección completa del beneficiario |
| I | Centro Poblado |
| J | Distrito |
| K | Tipo de Beneficiario (1 = Socia, 2 = Usuario, 3 = Caso Social — adulto mayor, 60+) |
| L | DNI del Apoderado (solo hijos) |
| M | Apellido Paterno del Apoderado |
| N | Apellido Materno del Apoderado |
| O | Nombres del Apoderado |

### Hoja "Interno_Control" (NO se comparte con supervisores)

| Col | Campo |
|---|---|
| A | ID_Familia (une a socia + pareja + hijos) |
| B | N° de fila en Registro_Oficial |
| C | Rol (Socia / Pareja / Hijo) |
| D | Estado civil declarado |
| E | ID_Presidenta / dispositivo |
| F | Fecha y hora de registro |
| G | DNI (para deduplicar) |

`ID_Familia` es la pieza que faltaba en tu tabla de ejemplo: sin ella no puedes saber, mirando la hoja
oficial, qué hijos pertenecen a qué socia una vez que hay cientos de filas mezcladas de distintas
presidentas.

## 5. Arquitectura técnica

```
[App Android - Kotlin + Jetpack Compose]
   ├─ CameraX → captura/selección de foto del DNI
   ├─ Cliente Gemini API (imagen + prompt JSON schema) → datos extraídos
   ├─ Validación local (regex DNI 8 dígitos, campos no vacíos)
   ├─ Máquina de estados del flujo (sección 2) — Kotlin selled classes / ViewModel
   └─ Llamada HTTPS → Google Apps Script Web App (backend)

[Backend - Google Apps Script Web App]
   ├─ doPost(e) recibe JSON
   ├─ LockService.getScriptLock()
   ├─ Valida duplicado de DNI
   ├─ SpreadsheetApp.appendRow() en ambas hojas
   └─ Responde { ok: true, numero: 128 }

[Google Sheets] — Registro_Oficial + Interno_Control
```

Por qué Apps Script y no la API de Sheets directo desde el celular: evita meter credenciales/OAuth de
Google dentro del APK (riesgo de seguridad) y te da el `LockService` gratis para la concurrencia.

## 6. Backlog Scrum (sprints de 1 semana, cada uno termina con push a GitHub)

**Repo:** `comedores-populares-app` (monorepo: `/android`, `/backend`, `/docs`, `/prompts`)

| Sprint | Objetivo | Historias de usuario | Entregable en GitHub |
|---|---|---|---|
| 0 | Setup | Crear repo, estructura de carpetas, README, este PRD | `docs/PRD.md`, `.gitignore` |
| 1 | Backend de datos | Como backend, debo recibir JSON y escribirlo sin colisiones | `backend/Code.gs` funcionando en Apps Script |
| 2 | Extracción DNI (Socia) | Como presidenta, subo el DNI y veo los campos autocompletados | Pantalla Compose + integración Gemini (prompt en `/prompts`) |
| 3 | Flujo Gestante/Discapacidad/Dirección | Checks Sí/No + extracción reverso | UI de formulario + validación |
| 4 | Estado civil + flujo pareja | Lógica condicional (Interno_Control) | ViewModel de estado civil |
| 5 | Flujo de hijos (N iteraciones) | Loop dinámico de N hijos + detección de sexo | Componente reutilizable "CapturaPersona" |
| 6 | Apoderado automático | Copiar datos de socia a hijos | Regla de negocio + tests |
| 7 | QA + hardening | Manejo de errores de cámara/OCR fallido, reintentos | Casos de prueba, changelog |
| 8 | Piloto | Prueba con 2-3 presidentas reales | Feedback + ajustes finales |

## 7. Cumplimiento y privacidad (Perú)

Este sistema procesa DNI (dato personal sensible bajo la Ley N.° 29733). Recomendaciones mínimas:
- No guardar fotos del DNI de forma permanente en el celular ni en Sheets — procesarlas y descartarlas
  (o subirlas cifradas a Drive con acceso restringido si se requiere respaldo).
- Acceso a la hoja de cálculo solo para cuentas autorizadas del programa.
- Consentimiento informado de la socia para el tratamiento de sus datos y los de su familia.
