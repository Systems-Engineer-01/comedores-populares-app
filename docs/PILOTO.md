# Guía Operativa, Checklist y Registro del Piloto (Sprint 8)

**Aplicación:** Registro Digital de Comedores Populares  
**Versión:** 1.0.0-pilot (Sprint 8)  
**Fecha:** 2026-09-10  
**Estado:** Listo para distribución y pruebas de campo con presidentas reales  

---

## 1. Guía de Generación y Distribución del APK

Debido a que los archivos binarios (`.apk`) no deben versionarse en el repositorio de Git por buenas prácticas de desarrollo, a continuación se detallan las instrucciones reproducibles para generar el paquete de distribución para el piloto.

### Opcion A: Vía Línea de Comandos (CLI)
1. Abrir la terminal en la raíz de la carpeta `/android`.
2. Verificar que la variable `JAVA_HOME` apunte a JDK 17 o superior (ejemplo en Windows PowerShell/CMD):
   ```cmd
   set JAVA_HOME=C:\Program Files\Android\jbr
   ```
3. Asegurarse de tener configurado `android/local.properties` con las llaves de API requeridas:
   ```properties
   GEMINI_API_KEY=tu_api_key_de_gemini
   BACKEND_URL=https://script.google.com/macros/s/TU_DEPLOYMENT_ID/exec
   ```
4. Ejecutar el comando de compilación del paquete APK de depuración / piloto:
   ```cmd
   gradlew.bat assembleDebug
   ```
5. El archivo APK generado estará disponible en:
   `android/app/build/outputs/apk/debug/app-debug.apk`

### Opción B: Vía Android Studio
1. Abrir la carpeta `android/` en Android Studio.
2. Sincronizar el proyecto (`Sync Project with Gradle Files`).
3. En el menú superior, seleccionar **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
4. Una vez terminada la compilación, hacer clic en la notificación flotante `"locate"` para abrir la carpeta contenedora con el archivo `app-debug.apk`.

### Distribución a Celulares de Piloto
- Enviar el archivo `app-debug.apk` a los dispositivos de las presidentas piloto mediante WhatsApp, Telegram, Google Drive o conexión directa por cable USB.

---

## 2. Checklist de Instalación en el Celular de Presidentas

Antes de iniciar la jornada de registro en el comedor popular, verificar cada punto en el dispositivo móvil:

- [ ] **Requisito de Sistema Operativo:** Celular Android con versión 7.0 (Nougat / API level 24) o superior.
- [ ] **Permisos de Fuentes Desconocidas:** Habilitar la opción *"Instalar aplicaciones desconocidas"* o *"Permitir desde esta fuente"* en el navegador/gestor de archivos al abrir el archivo `.apk`.
- [ ] **Permiso de Cámara:**
  - Al abrir la app por primera vez, presionar "Permitir" cuando el sistema pida acceso a la cámara (`Manifest.permission.CAMERA`).
  - *Verificación de error:* Si se denegó el permiso por error, presionar el botón **"Abrir Ajustes de la App"** en el banner rojo para otorgarlo.
- [ ] **Conexión a Internet Activa:**
  - Comprobar que el celular disponga de señal de datos móviles (4G/5G) o Wi-Fi estable.
  - *Motivo:* La llamada a Gemini Visión AI y el envío secuencial a Google Apps Script requieren salida HTTPS a internet.
- [ ] **Configuración de Backend:** Confirmar que la app apunte a la URL de producción del Web App de Apps Script vinculado al Google Sheet oficial.

---

## 3. Guion de Pruebas Operativas (Escenarios para Presidentas)

Para validar todas las ramas de decisión y reglas de negocio del sistema, pedir a cada presidenta de prueba registrar los siguientes 4 casos:

### Escenario 1: Socia con Cónyuge/Pareja e Hijos (Flujo Completo)
- **Instrucción:** Registrar una Socia declarando estado civil "Casada" o "Acompañada", con 2 hijos (ej. 1 niña y 1 niño).
- **Validación del flujo:**
  - Extrae anverso y reverso de la Socia. Pregunta Gestante y Discapacidad.
  - Activa automáticamente el flujo de la Pareja. Pregunta Discapacidad (no pregunta Gestante por ser varón).
  - Pide cantidad de hijos (`N = 2`).
  - Para la niña (sexo "F"), pregunta Gestante y Discapacidad. Para el niño (sexo "M"), NO pregunta Gestante.
  - Verifica que los datos del apoderado de ambos hijos se hayan copiado automáticamente de la Socia.
  - Muestra la pantalla de resumen con 4 integrantes (Socia, Pareja, Hijo 1, Hijo 2) y realiza el envío exitoso.

### Escenario 2: Socia Unipersonal / Sin Pareja
- **Instrucción:** Registrar una Socia declarando estado civil "Ninguna de las anteriores" o "Soltera sin pareja", con 0 hijos.
- **Validación del flujo:**
  - Al seleccionar "Ninguna de las anteriores", se omite la captura de Pareja.
  - Al ingresar `N = 0` en hijos, salta directamente al Resumen Familiar mostrando solo a la Socia (1 integrante) con Tipo Beneficiario = 1.

### Escenario 3: Familia Numerosa (> 2 Hijos)
- **Instrucción:** Registrar una Socia con 3 o más hijos (ej. `N = 4`).
- **Validación del flujo:**
  - El contador de hijos valida que `N` esté en el rango permitido (0 a 15).
  - El header muestra el avance correcto (`Hijo 1 de 4`, `Hijo 2 de 4`, etc.) sin reiniciar datos acumulados.

### Escenario 4: Pruebas de Fricción y Recuperación de Errores (Foto Borrosa / Red)
- **Instrucción:** Intentar tomar la foto del DNI con la luz apagada, con reflejo del plástico o en movimiento.
- **Validación del flujo:**
  - La app detecta `confianza: "baja"` y muestra el banner amarillo/rojo de advertencia: *"Foto borrosa u oscura - sugerimos repetir la foto"*.
  - Probar presionar **"Reintentar Foto"** (para volver a encuadrar) y **"Reintentar Extracción"** (para volver a invocar a Gemini con la foto existente sin volver a fotografiar).
  - Comprobar que los campos sigan siendo 100% editables a mano.

---

## 4. Plantilla de Feedback del Piloto

Utilizar esta plantilla para recopilar las impresiones y métricas directas de cada presidenta tras usar la app en terreno:

```markdown
### Ficha de Feedback — Piloto Comedores Populares

**Nombre del Comedor Popular:** _____________________________________
**Nombre de la Presidenta:** _________________________________________
**Modelo de Celular y Versión Android:** _____________________________
**Fecha y Hora del Piloto:** ________________________________________

#### 1. Evaluación de Usabilidad
- ¿Qué parte del flujo resultó más sencilla de realizar?
  [ ] Tomar foto del DNI
  [ ] Confirmar los campos editables
  [ ] Preguntas de Gestante / Discapacidad
  [ ] Revisión final y envío

- ¿Qué parte del flujo causó confusión o dudas?
  ___________________________________________________________________
  ___________________________________________________________________

#### 2. Rendimiento y Extracción
- Número de DNI registrados en la prueba: ____
- Número de veces que la foto salió borrosa y tuvo que repetirse: ____
- Tiempo estimado de registro completo por familia (minutos): ____

#### 3. Observaciones / Sugerencias de la Presidenta:
___________________________________________________________________
___________________________________________________________________
```

---

## 5. Verificación de Esquema, Seguridad y Privacidad

Se realizaron las siguientes comprobaciones técnicas y manuales en la arquitectura de datos:

### A. Verificación del Esquema en Google Sheets (Producción Real)
Se constató que las pestañas en la hoja de cálculo real contienen las columnas exactas del PRD (Sección 4) en el orden estricto:

1. **Pestaña `Registro_Oficial` (15 Columnas):**
   - `A`: N° (asignado por backend con `LockService`)
   - `B`: DNI
   - `C`: Apellido Paterno
   - `D`: Apellido Materno
   - `E`: Nombres
   - `F`: Gestante (Sí/No/—)
   - `G`: Discapacidad (Sí/No)
   - `H`: Dirección completa del beneficiario
   - `I`: Centro Poblado
   - `J`: Distrito
   - `K`: Tipo de Beneficiario (1 o 2)
   - `L`: DNI del Apoderado (solo hijos)
   - `M`: Apellido Paterno del Apoderado
   - `N`: Apellido Materno del Apoderado
   - `O`: Nombres del Apoderado

2. **Pestaña `Interno_Control` (7 Columnas - Privada):**
   - `A`: ID_Familia (UUID de agrupación familiar)
   - `B`: N° de fila en Registro_Oficial
   - `C`: Rol (Socia / Pareja / Hijo)
   - `D`: Estado civil declarado
   - `E`: ID_Presidenta / dispositivo
   - `F`: Fecha y hora de registro
   - `G`: DNI (para deduplicar)

### B. Control de Acceso y Privacidad de la Hoja
- **Restricción de Acceso (PRD Sección 7):** El libro de Google Sheets no es público. Los permisos de lectura/escritura están restringidos exclusivamente a los correos electrónicos institucionales autorizados del programa de comedores populares.
- **Backend Seguro:** La app no guarda llaves OAuth de Google ni credenciales del Spreadsheet dentro del APK. El intercambio se realiza de forma anónima y segura mediante invocación HTTPS al Web App de Google Apps Script (`doPost`).

### C. Auditoría de Tratamiento de Imágenes (Ley N.° 29733)
- **Eliminación Temporal de Fotos:** La aplicación **NO almacena ni persiste** las fotografías de los DNI capturados en el almacenamiento permanente del celular ni las transmite a Google Sheets o servidores de terceros.
- **Flujo de Memoria:**
  1. La foto tomada por CameraX se escribe en un archivo temporal en la carpeta privada de caché (`context.cacheDir`).
  2. La imagen en Base64 se envía cifrada a la API de Gemini Visión para la extracción.
  3. Una vez procesada la extracción y parseado el JSON, el archivo temporal en caché es marcado para su eliminación inmediata.
  4. Los únicos datos conservados en el estado de la app y enviados al backend son los campos textuales editables (DNI, nombres, dirección).

---

## 6. Ajustes Pendientes y Hallazgos del Piloto

A continuación se registra la matriz priorizada de hallazgos, fricciones o mejoras identificadas durante la preparación y ejecución del piloto:

| ID | Categoría | Descripción del Hallazgo / Fricción | Prioridad | Estado |
|---|---|---|---|---|
| H-01 | Usabilidad | En celulares de gama baja con pantalla pequeña (menor a 5.5"), algunos botones de confirmar requieren scroll vertical adicional. | **Mejora** | Pendiente |
| H-02 | Conectividad | En zonas con cobertura 2G/E, la llamada a Gemini API puede tardar más de 10 segundos. Se requiere feedback visual de espera. | **Importante** | Mitigado con Progress Overlay |
| H-03 | Iluminación | DNI plastificados con plastificado brillante reflejan el flash de la cámara. Se recomienda desactivar el flash automático. | **Mejora** | Recomendación en guía |
| H-04 | Negocio | Alguna socia sin apoderado directo (tutora o abuela) requiere registrar parentesco. El PRD actual fija apoderado a los datos de la socia. | **Mejora Futura** | Registrado para V2 |

---

*Cierre del Backlog Inicial de Desarrollo (Sprints 0 al 8).*
