package com.hotel.modelo;

import com.hotel.excepciones.HotelException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pruebas de {@link Reserva}: total, estados, servicios y pagos. */
class ReservaTest {

    private static final LocalDate ENTRADA = LocalDate.of(2026, 11, 10);
    private static final LocalDate SALIDA = LocalDate.of(2026, 11, 13); // 3 noches

    private Hotel hotel;
    private Habitacion habitacion;
    private Reserva reserva;

    @BeforeEach
    void preparar() {
        hotel = new Hotel("Hotel Test", "Calle 1", "123");
        habitacion = hotel.agregarHabitacion(201, TipoHabitacion.DOBLE, 2, new BigDecimal("120"));
        Cliente cliente = hotel.registrarCliente("Ana", "1", null, null, "Colombiana");
        reserva = hotel.crearReserva(cliente, habitacion, ENTRADA, SALIDA, null);
    }

    @Test
    @DisplayName("El total inicial es noches x tarifa")
    void totalInicial() {
        assertEquals(3, reserva.getNoches());
        assertEquals(new BigDecimal("360.00"), reserva.calcularTotal());
        assertEquals(EstadoReserva.PENDIENTE, reserva.getEstado());
    }

    @Test
    @DisplayName("Agregar servicios incrementa el total")
    void totalConServicios() {
        reserva.agregarServicio(new ServicioAdicional(1, "Desayuno", new BigDecimal("15"), 3)); // 45
        reserva.agregarServicio(new ServicioAdicional(2, "Spa", new BigDecimal("80.50"), 1));   // 80.50

        assertEquals(new BigDecimal("485.50"), reserva.getTotal());
        assertEquals(2, reserva.getServicios().size());
    }

    @Test
    @DisplayName("Cambiar la tarifa de la habitación no altera reservas existentes")
    void tarifaCongelada() {
        habitacion.setTarifaNoche(new BigDecimal("200"));

        assertEquals(new BigDecimal("360.00"), reserva.calcularTotal());
        assertEquals(new BigDecimal("120.00"), reserva.getTarifaNoche());
    }

    @Test
    @DisplayName("Una reserva pendiente se confirma una sola vez")
    void confirmar() {
        reserva.confirmar();

        assertEquals(EstadoReserva.CONFIRMADA, reserva.getEstado());
        assertThrows(HotelException.class, reserva::confirmar);
    }

    @Test
    @DisplayName("Se puede cancelar pendiente o confirmada, pero no dos veces")
    void cancelar() {
        reserva.confirmar();
        reserva.cancelar();

        assertEquals(EstadoReserva.CANCELADA, reserva.getEstado());
        assertThrows(HotelException.class, reserva::cancelar);
        assertTrue(habitacion.estaDisponible(ENTRADA, SALIDA));
    }

    @Test
    @DisplayName("No se pueden agregar servicios ni pagos a una reserva cancelada")
    void canceladaNoAceptaMovimientos() {
        reserva.cancelar();

        assertThrows(HotelException.class,
                () -> reserva.agregarServicio(new ServicioAdicional(1, "Spa", BigDecimal.TEN, 1)));
        assertThrows(HotelException.class,
                () -> reserva.registrarPago(BigDecimal.TEN, MetodoPago.EFECTIVO, ENTRADA));
    }

    @Test
    @DisplayName("Los pagos reducen el saldo pendiente")
    void pagosReducenSaldo() {
        reserva.registrarPago(new BigDecimal("100"), MetodoPago.TARJETA, ENTRADA);
        reserva.registrarPago(new BigDecimal("60.25"), MetodoPago.EFECTIVO, ENTRADA);

        assertEquals(new BigDecimal("160.25"), reserva.getTotalPagado());
        assertEquals(new BigDecimal("199.75"), reserva.getSaldoPendiente());
        assertEquals(2, reserva.getPagos().size());
    }

    @Test
    @DisplayName("Un pago que excede el saldo es rechazado y no se registra")
    void pagoExcedido() {
        assertThrows(HotelException.class,
                () -> reserva.registrarPago(new BigDecimal("360.01"), MetodoPago.TARJETA, ENTRADA));

        assertTrue(reserva.getPagos().isEmpty());
        assertEquals(new BigDecimal("360.00"), reserva.getSaldoPendiente());
    }

    @Test
    @DisplayName("Rechaza pagos con monto cero, negativo o nulo")
    void pagoInvalido() {
        assertThrows(IllegalArgumentException.class,
                () -> reserva.registrarPago(BigDecimal.ZERO, MetodoPago.EFECTIVO, ENTRADA));
        assertThrows(IllegalArgumentException.class,
                () -> reserva.registrarPago(new BigDecimal("-5"), MetodoPago.EFECTIVO, ENTRADA));
        assertThrows(IllegalArgumentException.class,
                () -> reserva.registrarPago(null, MetodoPago.EFECTIVO, ENTRADA));
    }

    @Test
    @DisplayName("Los servicios agregados después de un pago aumentan el saldo")
    void servicioDespuesDePago() {
        reserva.registrarPago(new BigDecimal("360"), MetodoPago.TARJETA, ENTRADA);
        assertEquals(new BigDecimal("0.00"), reserva.getSaldoPendiente());

        reserva.agregarServicio(new ServicioAdicional(1, "Lavandería", new BigDecimal("20"), 1));

        assertEquals(new BigDecimal("20.00"), reserva.getSaldoPendiente());
    }

    @Test
    @DisplayName("Cuenta las noches que caen dentro de un periodo")
    void nochesEnPeriodo() {
        assertEquals(3, reserva.nochesEnPeriodo(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 12, 1)));
        assertEquals(2, reserva.nochesEnPeriodo(LocalDate.of(2026, 11, 11), LocalDate.of(2026, 12, 1)));
        assertEquals(1, reserva.nochesEnPeriodo(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 11)));
        assertEquals(0, reserva.nochesEnPeriodo(LocalDate.of(2026, 11, 13), LocalDate.of(2026, 11, 20)));
        assertEquals(0, reserva.nochesEnPeriodo(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 11, 10)));
    }
}
