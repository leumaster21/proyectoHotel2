package com.hotel.modelo;

/** Medios de pago aceptados (requisito RF6). */
public enum MetodoPago {
    /** Pago en efectivo en recepción. */
    EFECTIVO,
    /** Pago con tarjeta (terminal o pasarela externa). */
    TARJETA,
    /** Transferencia bancaria. */
    TRANSFERENCIA
}
