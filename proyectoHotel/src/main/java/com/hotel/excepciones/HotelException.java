package com.hotel.excepciones;

/**
 * Excepción base para las violaciones de reglas de negocio del sistema de hotel
 * (por ejemplo, cancelar una reserva que ya está en curso).
 *
 * <p>Los errores de <em>argumentos inválidos</em> (valores nulos, montos negativos, etc.)
 * se reportan con {@link IllegalArgumentException}; esta clase queda reservada para
 * situaciones en las que los datos son válidos pero la operación no está permitida
 * por el estado actual del sistema.</p>
 */
public class HotelException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Crea una excepción con un mensaje descriptivo.
     *
     * @param mensaje descripción de la regla de negocio violada
     */
    public HotelException(String mensaje) {
        super(mensaje);
    }
}
