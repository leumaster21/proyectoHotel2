package com.hotel.modelo;

/**
 * Ciclo de vida de una {@link Reserva}.
 *
 * <pre>
 * PENDIENTE -> CONFIRMADA -> EN_CURSO -> FINALIZADA
 *      \            \
 *       +------------+--> CANCELADA
 * </pre>
 */
public enum EstadoReserva {
    /** Creada pero aún no confirmada. Ya bloquea la habitación. */
    PENDIENTE,
    /** Confirmada; lista para el check-in. */
    CONFIRMADA,
    /** El huésped hizo check-in y se encuentra alojado. */
    EN_CURSO,
    /** El huésped hizo check-out y la cuenta quedó saldada. */
    FINALIZADA,
    /** Anulada; libera la habitación. */
    CANCELADA
}
