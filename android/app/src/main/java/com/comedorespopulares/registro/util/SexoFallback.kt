package com.comedorespopulares.registro.util

/**
 * Fallback para detección de sexo cuando el campo "sexo" del DNI viene vacío.
 * Solo se usa como señal débil — si no hay match, se pregunta al usuario.
 *
 * Referencia: extraccion_dni.md, sección "Detección de sexo del hijo"
 */
object SexoFallback {

    // Lista no exhaustiva de nombres comunes peruanos femeninos
    private val nombresFemeninos = setOf(
        "MARIA", "ANA", "ROSA", "CARMEN", "JULIA", "LUISA", "ELENA",
        "PATRICIA", "MARTHA", "GLORIA", "TERESA", "BETTY", "SILVIA",
        "VILMA", "NORMA", "GLADYS", "ESTHER", "FLOR", "MILAGROS",
        "ALEJANDRA", "DIANA", "SOFIA", "VALENTINA", "CAMILA", "LUCIA",
        "GABRIELA", "ANDREA", "DANIELA", "FERNANDA", "MARIANA", "VALERIA",
        "XIMENA", "NATALIA", "ISABEL", "CATALINA", "ANGELICA", "LUZ",
        "PILAR", "JUANA", "CECILIA", "YOLANDA", "SUSANA", "GRACIELA",
        "NELLY", "OLGA", "IRMA", "DORA", "HILDA", "RUTH"
    )

    // Lista no exhaustiva de nombres comunes peruanos masculinos
    private val nombresMasculinos = setOf(
        "JOSE", "JUAN", "CARLOS", "LUIS", "JORGE", "PEDRO", "MIGUEL",
        "RICARDO", "ALBERTO", "OSCAR", "MARCO", "DAVID", "VICTOR",
        "MARIO", "RAUL", "EDUARDO", "FERNANDO", "ROBERTO", "MANUEL",
        "SANTIAGO", "MATEO", "SEBASTIAN", "DIEGO", "ALEJANDRO",
        "LEONARDO", "DANIEL", "SAMUEL", "NICOLAS", "GABRIEL", "EMILIO",
        "ANDRES", "PABLO", "ENRIQUE", "FRANCISCO", "HECTOR", "ARTURO",
        "CESAR", "EDINSON", "RENZO", "FRANCO", "BRYAN", "KEVIN"
    )

    /**
     * Intenta inferir el sexo a partir del primer nombre.
     * @return "F", "M", o null si no se puede determinar.
     */
    fun inferirSexo(nombres: String): String? {
        val primerNombre = nombres.trim().split("\\s+".toRegex()).firstOrNull()?.uppercase() ?: return null

        return when {
            nombresFemeninos.contains(primerNombre) -> "F"
            nombresMasculinos.contains(primerNombre) -> "M"
            else -> null // No se puede determinar → la app debe preguntar al usuario
        }
    }
}
