/**
 * Backend - Registro Digital Comedores Populares
 * Despliegue: Extensiones > Apps Script en la hoja de cálculo > Implementar > Aplicación Web
 *   - Ejecutar como: Yo
 *   - Quién tiene acceso: Cualquier usuario (o "Cualquiera con el enlace" si la app llama sin OAuth)
 *
 * Recibe un JSON por POST desde la app Android y lo escribe de forma segura (sin colisiones)
 * en la hoja "Registro_Oficial" y en "Interno_Control".
 *
 * Body esperado (ejemplo, un integrante):
 * {
 *   "id_familia": "F-0001",
 *   "rol": "Socia" | "Pareja" | "Hijo",
 *   "presidenta_id": "device-abc123",
 *   "estado_civil": "Casada",              // solo relevante si rol == "Socia"
 *   "dni": "60554283",
 *   "apellido_paterno": "NAZARIO",
 *   "apellido_materno": "ROA",
 *   "nombres": "EDINSON LEONARDO",
 *   "gestante": "No",                      // "" si no aplica (varón)
 *   "discapacidad": "No",
 *   "direccion": "PARCELAY-55 CAIDA ALTA",
 *   "distrito": "LAS LOMAS",
 *   "tipo_beneficiario": "1" | "2" | "3",    // 1 = Socia, 2 = Usuario, 3 = Caso Social (adulto mayor, 60+)
 *   "apoderado_dni": "02766720",
 *   "apoderado_apellido_paterno": "ROA",
 *   "apoderado_apellido_materno": "CRUZ",
 *   "apoderado_nombres": "VILMA"
 * }
 */

const HOJA_OFICIAL = 'Registro_Oficial';
const HOJA_INTERNA = 'Interno_Control';

function doPost(e) {
  const lock = LockService.getScriptLock();
  lock.waitLock(30000); // espera hasta 30s si otra presidenta está escribiendo

  try {
    const data = JSON.parse(e.postData.contents);
    const errorValidacion = validar(data);
    if (errorValidacion) {
      return respuesta({ ok: false, error: errorValidacion });
    }

    const ss = SpreadsheetApp.getActiveSpreadsheet();
    const hojaOficial = ss.getSheetByName(HOJA_OFICIAL);
    const hojaInterna = ss.getSheetByName(HOJA_INTERNA);

    // Evitar duplicar el mismo DNI dos veces en toda la hoja oficial
    if (dniYaRegistrado(hojaOficial, data.dni)) {
      return respuesta({ ok: false, error: 'El DNI ' + data.dni + ' ya está registrado.' });
    }

    const siguienteNumero = hojaOficial.getLastRow(); // fila 1 = encabezado, así que esto ya es el N correcto

    hojaOficial.appendRow([
      siguienteNumero,
      data.dni,
      data.apellido_paterno,
      data.apellido_materno,
      data.nombres,
      data.gestante || '',
      data.discapacidad,
      data.direccion,
      data.direccion,          // Centro Poblado = replica de Dirección
      data.distrito,
      data.tipo_beneficiario,
      data.apoderado_dni || '',
      data.apoderado_apellido_paterno || '',
      data.apoderado_apellido_materno || '',
      data.apoderado_nombres || ''
    ]);

    hojaInterna.appendRow([
      data.id_familia,
      siguienteNumero,
      data.rol,
      data.estado_civil || '',
      data.presidenta_id,
      new Date(),
      data.dni
    ]);

    return respuesta({ ok: true, numero: siguienteNumero });

  } catch (err) {
    return respuesta({ ok: false, error: 'Error interno: ' + err.message });
  } finally {
    lock.releaseLock();
  }
}

function validar(data) {
  if (!data.dni || !/^\d{8}$/.test(data.dni)) return 'DNI inválido (debe tener 8 dígitos).';
  if (!data.apellido_paterno) return 'Falta Apellido Paterno.';
  if (!data.nombres) return 'Faltan Nombres.';
  if (!['1', '2', '3'].includes(String(data.tipo_beneficiario))) return 'Tipo de Beneficiario inválido.';
  return null;
}

function dniYaRegistrado(hoja, dni) {
  const valores = hoja.getRange(2, 2, Math.max(hoja.getLastRow() - 1, 0), 1).getValues(); // columna B = DNI
  return valores.some(fila => String(fila[0]) === String(dni));
}

function respuesta(obj) {
  return ContentService.createTextOutput(JSON.stringify(obj))
    .setMimeType(ContentService.MimeType.JSON);
}
