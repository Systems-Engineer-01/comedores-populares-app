package com.comedorespopulares.registro.data.model

import com.google.gson.annotations.SerializedName

/**
 * JSON que se envía al backend (Google Apps Script Web App).
 * Corresponde al body esperado por doPost() en /backend/Code.gs
 */
data class RegistroRequest(
    @SerializedName("id_familia")
    val idFamilia: String,

    val rol: String,                          // "Socia", "Pareja", "Hijo"

    @SerializedName("presidenta_id")
    val presidentaId: String,

    @SerializedName("estado_civil")
    val estadoCivil: String = "",

    val dni: String,

    @SerializedName("apellido_paterno")
    val apellidoPaterno: String,

    @SerializedName("apellido_materno")
    val apellidoMaterno: String,

    val nombres: String,
    val gestante: String = "",
    val discapacidad: String,
    val direccion: String,
    val distrito: String,

    @SerializedName("tipo_beneficiario")
    val tipoBeneficiario: String,

    @SerializedName("apoderado_dni")
    val apoderadoDni: String = "",

    @SerializedName("apoderado_apellido_paterno")
    val apoderadoApellidoPaterno: String = "",

    @SerializedName("apoderado_apellido_materno")
    val apoderadoApellidoMaterno: String = "",

    @SerializedName("apoderado_nombres")
    val apoderadoNombres: String = ""
)
