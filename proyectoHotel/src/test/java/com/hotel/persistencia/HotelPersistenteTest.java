package com.hotel.persistencia;

import com.hotel.excepciones.HotelException;
import com.hotel.modelo.Cliente;
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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pruebas de {@link HotelPersistente}: la lista de clientes que agrega al hotel original. */
class HotelPersistenteTest {

    private HotelPersistente hotel;

    @BeforeEach
    void preparar() {
        hotel = new HotelPersistente("Hotel Test", "Calle 1", "123");
    }

    @Test
    @DisplayName("Empieza sin clientes")
    void vacio() {
        assertTrue(hotel.getClientes().isEmpty());
    }

    @Test
    @DisplayName("Lista los clientes en el orden en que se registraron")
    void listaClientes() {
        Cliente ana = hotel.registrarCliente("Ana", "1", null, null, "Colombiana");
        Cliente luis = hotel.registrarCliente("Luis", "2", null, null, "Peruana");

        assertEquals(2, hotel.getClientes().size());
        assertSame(ana, hotel.getClientes().get(0));
        assertSame(luis, hotel.getClientes().get(1));
        assertEquals(1, ana.getId());
        assertEquals(2, luis.getId());
    }

    @Test
    @DisplayName("Un cliente repetido o inválido no queda en la lista")
    void clienteRechazado() {
        hotel.registrarCliente("Ana", "1", null, null, "Colombiana");

        assertThrows(HotelException.class,
                () -> hotel.registrarCliente("Otra Ana", "1", null, null, "Chilena"));
        assertThrows(IllegalArgumentException.class,
                () -> hotel.registrarCliente("Luis", "2", null, "correo-malo", "Peruana"));

        assertEquals(1, hotel.getClientes().size());
    }

    @Test
    @DisplayName("La lista devuelta no se puede modificar")
    void listaInmutable() {
        hotel.registrarCliente("Ana", "1", null, null, "Colombiana");

        assertThrows(UnsupportedOperationException.class, () -> hotel.getClientes().clear());
    }

    @Test
    @DisplayName("Sigue funcionando como un Hotel normal (búsquedas y reservas)")
    void funcionaComoHotel() {
        Habitacion habitacion = hotel.agregarHabitacion(101, TipoHabitacion.DOBLE, 2, new BigDecimal("100"));
        Cliente ana = hotel.registrarCliente("Ana", "1", null, null, "Colombiana");

        Reserva reserva = hotel.crearReserva(ana, habitacion,
                LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 13), null);

        assertSame(ana, hotel.buscarCliente("1").orElseThrow());
        assertFalse(hotel.buscarDisponibles(LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 13)).contains(habitacion));
        assertEquals("RES-0001", reserva.getCodigo());
    }
}
