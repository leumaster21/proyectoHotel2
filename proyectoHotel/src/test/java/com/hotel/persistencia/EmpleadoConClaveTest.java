package com.hotel.persistencia;

import com.hotel.excepciones.AccesoDenegadoException;
import com.hotel.modelo.Cargo;
import com.hotel.modelo.Cliente;
import com.hotel.modelo.EstadoHabitacion;
import com.hotel.modelo.EstadoReserva;
import com.hotel.modelo.Habitacion;
import com.hotel.modelo.Reserva;
import com.hotel.modelo.TipoHabitacion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pruebas de {@link EmpleadoConClave}: contraseña, sesión y permisos. */
class EmpleadoConClaveTest {

    private HotelPersistente hotel;
    private Habitacion habitacion;
    private Reserva reserva;
    private EmpleadoConClave recepcionista;
    private EmpleadoConClave administrador;

    @BeforeEach
    void preparar() {
        hotel = new HotelPersistente("Hotel Test", "Calle 1", "123");
        habitacion = hotel.agregarHabitacion(101, TipoHabitacion.DOBLE, 2, new BigDecimal("100"));
        recepcionista = new EmpleadoConClave(1, "Laura", "10", null, null,
                Cargo.RECEPCIONISTA, "laura", "clave123");
        administrador = new EmpleadoConClave(2, "Pedro", "20", null, null,
                Cargo.ADMINISTRADOR, "pedro", "admin123");
        hotel.registrarEmpleado(recepcionista);
        hotel.registrarEmpleado(administrador);
        Cliente cliente = hotel.registrarCliente("Ana", "1", null, null, "Colombiana");
        reserva = hotel.crearReserva(cliente, habitacion,
                LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 13), null);
        reserva.confirmar();
    }

    @Test
    @DisplayName("Inicia sesión con la contraseña correcta")
    void loginCorrecto() {
        assertTrue(recepcionista.iniciarSesion("laura", "clave123"));
        assertTrue(recepcionista.isSesionActiva());
    }

    @Test
    @DisplayName("Rechaza contraseñas y usuarios incorrectos y deja la sesión cerrada")
    void loginIncorrecto() {
        assertFalse(recepcionista.iniciarSesion("laura", "otra-clave"));
        assertFalse(recepcionista.iniciarSesion("otro", "clave123"));
        assertFalse(recepcionista.iniciarSesion(null, null));

        recepcionista.iniciarSesion("laura", "clave123");
        recepcionista.iniciarSesion("laura", "mala");
        assertFalse(recepcionista.isSesionActiva());
    }

    @Test
    @DisplayName("La clave interna que usa la clase por dentro no sirve para iniciar sesión")
    void claveInternaNoSirve() {
        assertFalse(recepcionista.iniciarSesion("laura", "clave-interna-no-usar"));
        assertFalse(recepcionista.isSesionActiva());
    }

    @Test
    @DisplayName("Un recepcionista con sesión puede hacer check-in")
    void recepcionistaHaceCheckIn() {
        recepcionista.iniciarSesion("laura", "clave123");

        recepcionista.registrarCheckIn(reserva);

        assertEquals(EstadoReserva.EN_CURSO, reserva.getEstado());
        assertEquals(EstadoHabitacion.OCUPADA, habitacion.getEstado());
    }

    @Test
    @DisplayName("Sin sesión o siendo administrador no se puede hacer check-in")
    void sinPermiso() {
        assertThrows(AccesoDenegadoException.class, () -> recepcionista.registrarCheckIn(reserva));

        administrador.iniciarSesion("pedro", "admin123");
        assertThrows(AccesoDenegadoException.class, () -> administrador.registrarCheckIn(reserva));
    }

    @Test
    @DisplayName("Un empleado restaurado con su sal y su hash acepta la misma contraseña")
    void restaurado() {
        EmpleadoConClave copia = EmpleadoConClave.restaurar(1, "Laura", "10", null, null,
                Cargo.RECEPCIONISTA, "laura", recepcionista.getSalBase64(), recepcionista.getHashBase64());

        assertTrue(copia.iniciarSesion("laura", "clave123"));
        assertFalse(copia.iniciarSesion("laura", "otra-clave"));
        assertEquals(Cargo.RECEPCIONISTA, copia.getCargo());
    }

    @Test
    @DisplayName("El hash guardado no contiene la contraseña y cada empleado tiene su propia sal")
    void hashSeguro() {
        EmpleadoConClave igual = new EmpleadoConClave(3, "Otra", "30", null, null,
                Cargo.RECEPCIONISTA, "otra", "clave123");

        assertFalse(recepcionista.getHashBase64().contains("clave123"));
        assertFalse(recepcionista.getSalBase64().equals(igual.getSalBase64()));
        assertFalse(recepcionista.getHashBase64().equals(igual.getHashBase64()),
                "con la misma contraseña, el hash es distinto porque la sal es distinta");
    }

    @Test
    @DisplayName("Exige contraseña de al menos 6 caracteres")
    void contrasenaCorta() {
        assertThrows(IllegalArgumentException.class, () -> new EmpleadoConClave(9, "X", "90", null, null,
                Cargo.RECEPCIONISTA, "x", "12345"));
        assertThrows(IllegalArgumentException.class, () -> new EmpleadoConClave(9, "X", "90", null, null,
                Cargo.RECEPCIONISTA, "x", null));
    }
}
