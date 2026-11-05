package com.hotel.modelo;

/**
 * Clase abstracta que agrupa los datos comunes de las personas del sistema.
 * {@link Cliente} y {@link Empleado} heredan de ella (relación de herencia del
 * diagrama de clases).
 */
public abstract class Persona {

    private final int id;
    private final String nombre;
    private final String documento;
    private String telefono;
    private String email;

    /**
     * Crea una persona validando sus datos obligatorios.
     *
     * @param id        identificador interno
     * @param nombre    nombre completo (obligatorio)
     * @param documento documento de identidad (obligatorio)
     * @param telefono  teléfono de contacto (opcional)
     * @param email     correo electrónico (opcional; si se indica debe contener '@')
     * @throws IllegalArgumentException si el nombre o el documento están vacíos,
     *                                  o si el correo no es válido
     */
    protected Persona(int id, String nombre, String documento, String telefono, String email) {
        this.id = id;
        this.nombre = Validaciones.texto(nombre, "El nombre");
        this.documento = Validaciones.texto(documento, "El documento");
        actualizarContacto(telefono, email);
    }

    /**
     * Devuelve una ficha de texto con los datos principales de la persona.
     *
     * @return cadena con id, nombre, documento, teléfono y correo
     */
    public String obtenerDatos() {
        return String.format("[%d] %s - Documento: %s - Tel: %s - Email: %s",
                id, nombre, documento, valorOND(telefono), valorOND(email));
    }

    /**
     * Actualiza los datos de contacto.
     *
     * @param telefono nuevo teléfono (puede ser {@code null})
     * @param email    nuevo correo (puede ser {@code null}; si se indica debe contener '@')
     * @throws IllegalArgumentException si el correo no tiene formato válido
     */
    public final void actualizarContacto(String telefono, String email) {
        if (email != null && !email.isBlank() && !email.contains("@")) {
            throw new IllegalArgumentException("El correo electrónico no es válido");
        }
        this.telefono = telefono;
        this.email = email;
    }

    private static String valorOND(String valor) {
        return (valor == null || valor.isBlank()) ? "N/D" : valor;
    }

    /**
     * Devuelve el identificador interno.
     *
     * @return identificador
     */
    public int getId() {
        return id;
    }

    /**
     * Devuelve el nombre completo.
     *
     * @return nombre
     */
    public String getNombre() {
        return nombre;
    }

    /**
     * Devuelve el documento de identidad.
     *
     * @return documento
     */
    public String getDocumento() {
        return documento;
    }

    /**
     * Devuelve el teléfono de contacto.
     *
     * @return teléfono, o {@code null} si no se registró
     */
    public String getTelefono() {
        return telefono;
    }

    /**
     * Devuelve el correo electrónico.
     *
     * @return correo, o {@code null} si no se registró
     */
    public String getEmail() {
        return email;
    }
}
