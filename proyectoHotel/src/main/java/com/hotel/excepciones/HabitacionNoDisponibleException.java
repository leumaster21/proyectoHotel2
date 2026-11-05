package com.hotel.excepciones;

import java.time.LocalDate;

/**
 * Se lanza cuando se intenta reservar una habitación que no está libre en el
 * rango de fechas solicitado. Es la excepción que evita la <strong>sobreventa</strong>
 * de habitaciones descrita en el problema.
 */
public class HabitacionNoDisponibleException extends HotelException {

    private static final long serialVersionUID = 1L;

    /** Número de la habitación que causó el conflicto. */
    private final int numeroHabitacion;

    /**
     * Crea la excepción indicando la habitación y el rango que generó el conflicto.
     *
     * @param numeroHabitacion número de la habitación solicitada
     * @param entrada          fecha de entrada solicitada
     * @param salida           fecha de salida solicitada
     */
    public HabitacionNoDisponibleException(int numeroHabitacion, LocalDate entrada, LocalDate salida) {
        super("La habitación " + numeroHabitacion + " no está disponible entre "
                + entrada + " y " + salida);
        this.numeroHabitacion = numeroHabitacion;
    }

    /**
     * Devuelve el número de la habitación que causó el conflicto.
     *
     * @return número de habitación
     */
    public int getNumeroHabitacion() {
        return numeroHabitacion;
    }
}
