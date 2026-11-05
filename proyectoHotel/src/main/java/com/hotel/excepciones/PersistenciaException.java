package com.hotel.excepciones;

/**
 * Se lanza cuando falla una operación con la base de datos (por ejemplo, no se
 * puede abrir la conexión o una consulta SQL da error).
 */
public class PersistenciaException extends HotelException {

    private static final long serialVersionUID = 1L;

    /**
     * Crea la excepción con un mensaje y la causa original.
     *
     * @param mensaje descripción del problema
     * @param causa   excepción SQL que lo originó (puede ser {@code null})
     */
    @SuppressWarnings("this-escape")
    public PersistenciaException(String mensaje, Throwable causa) {
        super(mensaje);
        if (causa != null) {
            initCause(causa);
        }
    }
}
