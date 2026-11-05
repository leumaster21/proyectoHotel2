package com.hotel.modelo;

import com.hotel.excepciones.AccesoDenegadoException;
import com.hotel.excepciones.HotelException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pruebas de {@link Empleado}: sesión, permisos por cargo, check-in y check-out. */
class EmpleadoTest {

    private static final LocalDate ENTRADA = LocalDate.of(2026, 11, 10);
    private static final LocalDate SALIDA = LocalDate.of(2026, 11, 13);

    private Hotel hotel;
    private Habitacion habitacion;
    private Empleado recepcionista;
    private Empleado administrador;
    private Reserva reserva;

    @BeforeEach
    void preparar() {
        hotel = new Hotel("Hotel Test", "Calle 1", "123");
        habitacion = hotel.agregarHabitacion(201, TipoHabitacion.DOBLE, 2, new BigDecimal("100"));
        recepcionista = new Empleado(1, "Laura", "10", null, null, Cargo.RECEPCIONISTA, "laura", "clave123");
        administrador = new Empleado(2, "Pedro", "20", null, null, Cargo.ADMINISTRADOR, "pedro", "admin123");
        hotel.registrarEmpleado(recepcionista);
        hotel.registrarEmpleado(administrador);
        Cliente cliente = hotel.registrarCliente("Ana", "1", null, null, "Colombiana");
        reserva = hotel.crearReserva(cliente, habitacion, ENTRADA, SALIDA, null);
        reserva.confirmar();
    }

    @Test
    @DisplayName("Inicia sesión con credenciales correctas")
    void loginCorrecto() {
        assertTrue(recepcionista.iniciarSesion("laura", "clave123"));
        assertTrue(recepcionista.isSesionActiva());
    }

    @Test
    @DisplayName("Rechaza usuario o contraseña incorrectos y cierra la sesión")
    void loginIncorrecto() {
        assertFalse(recepcionista.iniciarSesion("laura", "otra-clave"));
        assertFalse(recepcionista.iniciarSesion("otro", "clave123"));
        assertFalse(recepcionista.iniciarSesion(null, null));

        recepcionista.iniciarSesion("laura", "clave123");
        recepcionista.iniciarSesion("laura", "mala");
        assertFalse(recepcionista.isSesionActiva(), "un intento fallido cierra la sesión");
    }

    @Test
    @DisplayName("Cerrar sesión desactiva la sesión")
    void cerrarSesion() {
        recepcionista.iniciarSesion("laura", "clave123");
        recepcionista.cerrarSesion();

        assertFalse(recepcionista.isSesionActiva());
    }

    @Test
    @DisplayName("Exige contraseña de al menos 6 caracteres")
    void contrasenaCorta() {
        assertThrows(IllegalArgumentException.class,
                () -> new Empleado(3, "X", "30", null, null, Cargo.RECEPCIONISTA, "x", "12345"));
    }

    @Test
    @DisplayName("Sin sesión no se puede hacer check-in ni check-out")
    void sinSesion() {
        assertThrows(AccesoDenegadoException.class, () -> recepcionista.registrarCheckIn(reserva));
        assertThrows(AccesoDenegadoException.class, () -> recepcionista.registrarCheckOut(reserva));
    }

    @Test
    @DisplayName("Un administrador no puede hacer check-in")
    void administradorSinPermiso() {
        administrador.iniciarSesion("pedro", "admin123");

        assertThrows(AccesoDenegadoException.class, () -> administrador.registrarCheckIn(reserva));
        assertEquals(EstadoReserva.CONFIRMADA, reserva.getEstado());
    }

    @Test
    @DisplayName("El check-in ocupa la habitación, pone la reserva en curso y asigna empleado")
    void checkIn() {
        recepcionista.iniciarSesion("laura", "clave123");

        recepcionista.registrarCheckIn(reserva);

        assertEquals(EstadoReserva.EN_CURSO, reserva.getEstado());
        assertEquals(EstadoHabitacion.OCUPADA, habitacion.getEstado());
        assertSame(recepcionista, reserva.getEmpleado());
    }

    @Test
    @DisplayName("No se hace check-in de una reserva pendiente ni dos veces")
    void checkInEstadoInvalido() {
        recepcionista.iniciarSesion("laura", "clave123");
        Cliente otro = hotel.registrarCliente("Luis", "2", null, null, "Peruana");
        Habitacion otra = hotel.agregarHabitacion(202, TipoHabitacion.SENCILLA, 1, BigDecimal.TEN);
        Reserva pendiente = hotel.crearReserva(otro, otra, ENTRADA, SALIDA, null);

        assertThrows(HotelException.class, () -> recepcionista.registrarCheckIn(pendiente));

        recepcionista.registrarCheckIn(reserva);
        assertThrows(HotelException.class, () -> recepcionista.registrarCheckIn(reserva));
    }

    @Test
    @DisplayName("No se hace check-in si la habitación está en mantenimiento")
    void checkInConMantenimiento() {
        recepcionista.iniciarSesion("laura", "clave123");
        habitacion.cambiarEstado(EstadoHabitacion.MANTENIMIENTO);

        assertThrows(HotelException.class, () -> recepcionista.registrarCheckIn(reserva));
        assertEquals(EstadoReserva.CONFIRMADA, reserva.getEstado(), "la reserva no debe cambiar si falla");
    }

    @Test
    @DisplayName("No se hace check-out con saldo pendiente")
    void checkOutConSaldo() {
        recepcionista.iniciarSesion("laura", "clave123");
        recepcionista.registrarCheckIn(reserva);

        assertThrows(HotelException.class, () -> recepcionista.registrarCheckOut(reserva));
        assertEquals(EstadoReserva.EN_CURSO, reserva.getEstado());
        assertEquals(EstadoHabitacion.OCUPADA, habitacion.getEstado());
    }

    @Test
    @DisplayName("Con la cuenta saldada el check-out libera la habitación")
    void checkOut() {
        recepcionista.iniciarSesion("laura", "clave123");
        recepcionista.registrarCheckIn(reserva);
        reserva.registrarPago(reserva.getSaldoPendiente(), MetodoPago.EFECTIVO, SALIDA);

        recepcionista.registrarCheckOut(reserva);

        assertEquals(EstadoReserva.FINALIZADA, reserva.getEstado());
        assertEquals(EstadoHabitacion.DISPONIBLE, habitacion.getEstado());
    }

    @Test
    @DisplayName("No se hace check-out de una reserva que no está en curso")
    void checkOutSinCheckIn() {
        recepcionista.iniciarSesion("laura", "clave123");

        assertThrows(HotelException.class, () -> recepcionista.registrarCheckOut(reserva));
    }
}
