package com.hotel.excepciones;

/**
 * Se lanza cuando un empleado intenta una operación sin sesión iniciada o sin el
 * cargo necesario (requisito no funcional RNF2: acceso con roles).
 */
public class AccesoDenegadoException extends HotelException {

    private static final long serialVersionUID = 1L;

    /**
     * Crea la excepción con un mensaje descriptivo.
     *
     * @param mensaje motivo por el cual se deniega el acceso
     */
    public AccesoDenegadoException(String mensaje) {
        super(mensaje);
    }
}
