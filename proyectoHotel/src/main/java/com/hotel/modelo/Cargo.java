package com.hotel.modelo;

/** Cargo (rol) de un {@link Empleado}. Distingue al recepcionista del administrador. */
public enum Cargo {
    /** Crea reservas, hace check-in y check-out y cobra. */
    RECEPCIONISTA,
    /** Gestiona habitaciones, tarifas, empleados y reportes. */
    ADMINISTRADOR
}
