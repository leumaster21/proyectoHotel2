package com.hotel.modelo;

/** Estado físico de una {@link Habitacion} (requisito RF2). */
public enum EstadoHabitacion {
    /** Lista para recibir huéspedes. */
    DISPONIBLE,
    /** Actualmente ocupada por un huésped (hay un check-in sin check-out). */
    OCUPADA,
    /** Fuera de servicio; no puede reservarse. */
    MANTENIMIENTO
}
