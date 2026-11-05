package com.hotel.modelo;

import com.hotel.excepciones.HotelException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pruebas de {@link Habitacion}: disponibilidad por fechas y cambios de estado. */
class HabitacionTest {

    private static final LocalDate D10 = LocalDate.of(2026, 11, 10);
    private static final LocalDate D13 = LocalDate.of(2026, 11, 13);

    private Hotel hotel;
    private Habitacion habitacion;
    private Cliente cliente;

    @BeforeEach
    void preparar() {
        hotel = new Hotel("Hotel Test", "Calle 1", "123");
        habitacion = hotel.agregarHabitacion(101, TipoHabitacion.DOBLE, 2, new BigDecimal("100"));
        cliente = hotel.registrarCliente("Ana", "1", null, null, "Colombiana");
    }

    @Test
    @DisplayName("Una habitación sin reservas está disponible")
    void sinReservasEstaDisponible() {
        assertTrue(habitacion.estaDisponible(D10, D13));
    }

    @Test
    @DisplayName("No está disponible si una reserva activa se cruza con el rango")
    void reservaCruzadaBloquea() {
        hotel.crearReserva(cliente, habitacion, D10, D13, null);

        assertFalse(habitacion.estaDisponible(D10, D13), "mismo rango");
        assertFalse(habitacion.estaDisponible(LocalDate.of(2026, 11, 12), LocalDate.of(2026, 11, 15)),
                "cruce al final");
        assertFalse(habitacion.estaDisponible(LocalDate.of(2026, 11, 8), LocalDate.of(2026, 11, 11)),
                "cruce al inicio");
        assertFalse(habitacion.estaDisponible(LocalDate.of(2026, 11, 11), LocalDate.of(2026, 11, 12)),
                "rango contenido");
    }

    @Test
    @DisplayName("El día de salida queda libre para otra entrada (rango semiabierto)")
    void reservasConsecutivasSonValidas() {
        hotel.crearReserva(cliente, habitacion, D10, D13, null);

        assertTrue(habitacion.estaDisponible(D13, LocalDate.of(2026, 11, 15)), "entra el día que otro sale");
        assertTrue(habitacion.estaDisponible(LocalDate.of(2026, 11, 7), D10), "sale el día que otro entra");
    }

    @Test
    @DisplayName("Una reserva cancelada libera la habitación")
    void reservaCanceladaNoBloquea() {
        Reserva reserva = hotel.crearReserva(cliente, habitacion, D10, D13, null);
        reserva.cancelar();

        assertTrue(habitacion.estaDisponible(D10, D13));
    }

    @Test
    @DisplayName("En mantenimiento nunca está disponible")
    void mantenimientoNoEstaDisponible() {
        habitacion.cambiarEstado(EstadoHabitacion.MANTENIMIENTO);

        assertFalse(habitacion.estaDisponible(D10, D13));
    }

    @Test
    @DisplayName("Rechaza rangos de fechas inválidos o nulos")
    void rangoInvalido() {
        assertThrows(IllegalArgumentException.class, () -> habitacion.estaDisponible(D13, D10));
        assertThrows(IllegalArgumentException.class, () -> habitacion.estaDisponible(D10, D10));
        assertThrows(IllegalArgumentException.class, () -> habitacion.estaDisponible(null, D13));
    }

    @Test
    @DisplayName("Permite DISPONIBLE <-> OCUPADA y DISPONIBLE <-> MANTENIMIENTO")
    void transicionesValidas() {
        habitacion.cambiarEstado(EstadoHabitacion.OCUPADA);
        assertEquals(EstadoHabitacion.OCUPADA, habitacion.getEstado());
        habitacion.cambiarEstado(EstadoHabitacion.DISPONIBLE);
        habitacion.cambiarEstado(EstadoHabitacion.MANTENIMIENTO);
        assertEquals(EstadoHabitacion.MANTENIMIENTO, habitacion.getEstado());
        habitacion.cambiarEstado(EstadoHabitacion.DISPONIBLE);
        assertEquals(EstadoHabitacion.DISPONIBLE, habitacion.getEstado());
    }

    @Test
    @DisplayName("Rechaza transiciones no permitidas")
    void transicionesInvalidas() {
        habitacion.cambiarEstado(EstadoHabitacion.OCUPADA);
        assertThrows(HotelException.class, () -> habitacion.cambiarEstado(EstadoHabitacion.MANTENIMIENTO));
        assertThrows(HotelException.class, () -> habitacion.cambiarEstado(EstadoHabitacion.OCUPADA));
    }

    @Test
    @DisplayName("Valida tarifa, capacidad y número al crear")
    void validacionesDeCreacion() {
        assertThrows(IllegalArgumentException.class,
                () -> hotel.agregarHabitacion(102, TipoHabitacion.SENCILLA, 1, BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class,
                () -> hotel.agregarHabitacion(103, TipoHabitacion.SENCILLA, 0, BigDecimal.TEN));
        assertThrows(IllegalArgumentException.class,
                () -> hotel.agregarHabitacion(-1, TipoHabitacion.SENCILLA, 1, BigDecimal.TEN));
        assertThrows(IllegalArgumentException.class,
                () -> hotel.agregarHabitacion(104, null, 1, BigDecimal.TEN));
    }

    @Test
    @DisplayName("Normaliza la tarifa a dos decimales")
    void tarifaConDosDecimales() {
        assertEquals(new BigDecimal("100.00"), habitacion.getTarifaNoche());
    }
}
