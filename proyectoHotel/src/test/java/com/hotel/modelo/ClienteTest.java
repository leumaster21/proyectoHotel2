package com.hotel.modelo;

import com.hotel.excepciones.HabitacionNoDisponibleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pruebas de {@link Cliente} y de los datos comunes de {@link Persona}. */
class ClienteTest {

    private static final LocalDate ENTRADA = LocalDate.of(2026, 11, 10);
    private static final LocalDate SALIDA = LocalDate.of(2026, 11, 13);

    private Hotel hotel;
    private Habitacion habitacion;
    private Cliente ana;

    @BeforeEach
    void preparar() {
        hotel = new Hotel("Hotel Test", "Calle 1", "123");
        habitacion = hotel.agregarHabitacion(101, TipoHabitacion.SENCILLA, 1, new BigDecimal("80"));
        ana = hotel.registrarCliente("Ana Pérez", "5566", "300111", "ana@correo.com", "Colombiana");
    }

    @Test
    @DisplayName("Un cliente nuevo tiene categoría REGULAR y sin reservas")
    void estadoInicial() {
        assertEquals(Cliente.CATEGORIA_INICIAL, ana.getCategoria());
        assertTrue(ana.consultarHistorial().isEmpty());
        assertEquals("Colombiana", ana.getNacionalidad());
    }

    @Test
    @DisplayName("solicitarReserva crea una reserva pendiente sin empleado y la guarda en el historial")
    void solicitarReserva() {
        Reserva reserva = ana.solicitarReserva(hotel, habitacion, ENTRADA, SALIDA);

        assertEquals(EstadoReserva.PENDIENTE, reserva.getEstado());
        assertNull(reserva.getEmpleado());
        assertEquals(1, ana.consultarHistorial().size());
        assertEquals(reserva, ana.consultarHistorial().get(0));
    }

    @Test
    @DisplayName("solicitarReserva falla si la habitación ya está tomada")
    void solicitarReservaOcupada() {
        ana.solicitarReserva(hotel, habitacion, ENTRADA, SALIDA);
        Cliente luis = hotel.registrarCliente("Luis", "7788", null, null, "Peruana");

        assertThrows(HabitacionNoDisponibleException.class,
                () -> luis.solicitarReserva(hotel, habitacion, ENTRADA, SALIDA));
        assertTrue(luis.consultarHistorial().isEmpty());
    }

    @Test
    @DisplayName("El historial devuelto no se puede modificar")
    void historialInmutable() {
        assertThrows(UnsupportedOperationException.class, () -> ana.consultarHistorial().clear());
    }

    @Test
    @DisplayName("actualizarContacto cambia teléfono y correo")
    void actualizarContacto() {
        ana.actualizarContacto("310999", "nuevo@correo.com");

        assertEquals("310999", ana.getTelefono());
        assertEquals("nuevo@correo.com", ana.getEmail());
    }

    @Test
    @DisplayName("actualizarContacto rechaza correos sin '@' y no modifica los datos")
    void correoInvalido() {
        assertThrows(IllegalArgumentException.class, () -> ana.actualizarContacto("310999", "correo-malo"));

        assertEquals("300111", ana.getTelefono());
        assertEquals("ana@correo.com", ana.getEmail());
    }

    @Test
    @DisplayName("obtenerDatos incluye los datos y muestra N/D en los vacíos")
    void obtenerDatos() {
        Cliente sinContacto = hotel.registrarCliente("Luis", "7788", null, null, "Peruana");

        assertTrue(ana.obtenerDatos().contains("Ana Pérez"));
        assertTrue(ana.obtenerDatos().contains("5566"));
        assertTrue(sinContacto.obtenerDatos().contains("N/D"));
    }

    @Test
    @DisplayName("Rechaza nombre, documento y nacionalidad vacíos")
    void datosObligatorios() {
        assertThrows(IllegalArgumentException.class,
                () -> new Cliente(9, " ", "1", null, null, "Colombiana"));
        assertThrows(IllegalArgumentException.class,
                () -> new Cliente(9, "X", "", null, null, "Colombiana"));
        assertThrows(IllegalArgumentException.class,
                () -> new Cliente(9, "X", "1", null, null, null));
    }

    @Test
    @DisplayName("Cambiar la categoría exige un valor no vacío")
    void categoria() {
        ana.setCategoria("VIP");
        assertEquals("VIP", ana.getCategoria());
        assertThrows(IllegalArgumentException.class, () -> ana.setCategoria(" "));
    }
}
