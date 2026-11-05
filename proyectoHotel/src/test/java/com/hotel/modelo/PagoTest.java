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

/** Pruebas de {@link Pago} y de la emisión de comprobantes. */
class PagoTest {

    private static final LocalDate FECHA = LocalDate.of(2026, 11, 10);
    private Reserva reserva;

    @BeforeEach
    void preparar() {
        Hotel hotel = new Hotel("Hotel Test", "Calle 1", "123");
        Habitacion habitacion = hotel.agregarHabitacion(201, TipoHabitacion.DOBLE, 2, new BigDecimal("120"));
        Cliente cliente = hotel.registrarCliente("Ana Pérez", "1", null, null, "Colombiana");
        reserva = hotel.crearReserva(cliente, habitacion, FECHA, FECHA.plusDays(3), null);
    }

    @Test
    @DisplayName("Un pago registrado queda procesado")
    void pagoRegistradoEstaProcesado() {
        Pago pago = reserva.registrarPago(new BigDecimal("100"), MetodoPago.TARJETA, FECHA);

        assertTrue(pago.isProcesado());
        assertEquals(new BigDecimal("100.00"), pago.getMonto());
        assertEquals(MetodoPago.TARJETA, pago.getMetodo());
        assertEquals(FECHA, pago.getFecha());
    }

    @Test
    @DisplayName("No se puede procesar dos veces el mismo pago")
    void procesarDosVeces() {
        Pago pago = reserva.registrarPago(new BigDecimal("100"), MetodoPago.TARJETA, FECHA);

        assertThrows(HotelException.class, pago::procesar);
    }

    @Test
    @DisplayName("El comprobante incluye número, cliente, habitación, monto y método")
    void comprobante() {
        Pago pago = reserva.registrarPago(new BigDecimal("100"), MetodoPago.TRANSFERENCIA, FECHA);
        String texto = pago.generarComprobante();

        assertEquals("RES-0001-P1", pago.getNumeroComprobante());
        assertTrue(texto.contains("RES-0001-P1"));
        assertTrue(texto.contains("Ana Pérez"));
        assertTrue(texto.contains("201"));
        assertTrue(texto.contains("100.00"));
        assertTrue(texto.contains("TRANSFERENCIA"));
    }

    @Test
    @DisplayName("No emite comprobante de un pago sin procesar")
    void comprobanteSinProcesar() {
        Pago sinProcesar = new Pago(9, reserva, BigDecimal.TEN, MetodoPago.EFECTIVO, FECHA);

        assertFalse(sinProcesar.isProcesado());
        assertThrows(HotelException.class, sinProcesar::generarComprobante);
    }

    @Test
    @DisplayName("Los pagos de una reserva se numeran en orden")
    void numeracion() {
        Pago p1 = reserva.registrarPago(new BigDecimal("50"), MetodoPago.EFECTIVO, FECHA);
        Pago p2 = reserva.registrarPago(new BigDecimal("50"), MetodoPago.EFECTIVO, FECHA);

        assertEquals(1, p1.getId());
        assertEquals(2, p2.getId());
    }
}
