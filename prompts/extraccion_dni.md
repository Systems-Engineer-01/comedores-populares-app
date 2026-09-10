# Prompt para Gemini 3.1 Pro — Extracción de campos del DNI peruano

Pega este prompt en Antigravity cuando implementes la llamada a Gemini (imagen adjunta = foto del DNI).
Pide SIEMPRE salida JSON estricta para poder parsearla en Kotlin sin ambigüedad.

## Prompt (anverso del DNI)

```
Eres un extractor de datos de Documentos Nacionales de Identidad (DNI) del Perú. Se te entrega la foto
del ANVERSO de un DNI. Devuelve ÚNICAMENTE un objeto JSON válido (sin texto adicional, sin markdown),
con esta forma exacta:

{
  "dni": "string de 8 dígitos, sin espacios ni guiones",
  "apellido_paterno": "string en mayúsculas",
  "apellido_materno": "string en mayúsculas",
  "nombres": "string en mayúsculas",
  "sexo": "M" o "F",
  "confianza": "alta" | "media" | "baja"
}

Reglas:
- Si algún campo no se puede leer con certeza, usa "" (cadena vacía) en ese campo y baja "confianza".
- No inventes datos. No completes con suposiciones.
- El campo "sexo" viene del recuadro "Sexo" del DNI (M/F).
- Ignora la zona MRZ (las líneas con "<<<") si el texto impreso arriba ya es legible; úsala solo como
  respaldo si el texto impreso está borroso.
```

## Prompt (reverso del DNI)

```
Eres un extractor de datos del REVERSO de un DNI peruano. Devuelve ÚNICAMENTE este JSON:

{
  "direccion": "string, tal como aparece en el campo 'Dirección'",
  "distrito": "string, tal como aparece en el campo 'Distrito'",
  "provincia": "string",
  "departamento": "string",
  "confianza": "alta" | "media" | "baja"
}

Reglas:
- No agregues el departamento/provincia dentro de "direccion"; van en campos separados.
- Si el campo no es legible, usa "" y baja "confianza".
```

## Regla de negocio en la app (Kotlin), NO en el prompt

```kotlin
val centroPoblado = direccionExtraida   // el DNI peruano no trae centro poblado; se replica
```

## Manejo de "confianza": baja

Si `confianza == "baja"` o algún campo obligatorio viene vacío, la app debe:
1. Mostrar los campos editables (no autocompletados en modo lectura) para que la presidenta corrija a mano.
2. No auto-enviar al backend hasta que la presidenta confirme.

## Detección de sexo del hijo (para omitir/incluir pregunta "¿Gestante?")

Usar el campo `"sexo"` devuelto por Gemini (M/F) — es el mismo dato que ya viene en el DNI, no hace falta
heurística de nombres. Solo si el campo viene vacío, usar como respaldo una lista de nombres comunes
peruanos como señal débil, y en caso de duda, preguntar directamente al usuario "¿Es hombre o mujer?" antes
de decidir si se muestra la pregunta de Gestante.
