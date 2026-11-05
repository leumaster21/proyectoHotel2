package com.hotel.modelo;

import com.hotel.excepciones.HabitacionNoDisponibleException;
import com.hotel.excepciones.HotelException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pruebas de {@link Hotel}: registro, disponibilidad, sobreventa, cancelación y reportes. */
class HotelTest {

    private static final LocalDate ENTRADA = LocalDate.of(2026, 11, 10);
    private static final LocalDate SALIDA = LocalDate.of(2026, 11, 13);

    private Hotel hotel;
    private Habitacion h101;
    private Habitacion h102;
    private Cliente ana;
    private Cliente luis;

    @BeforeEach
    void preparar() {
        hotel = new Hotel("Hotel Test", "Calle 1", "123");
        h101 = hotel.agregarHabitacion(101, TipoHabitacion.SENCILLA, 1, new BigDecimal("80"));
        h102 = hotel.agregarHabitacion(102, TipoHabitacion.DOBLE, 2, new BigDecimal("120"));
        ana = hotel.registrarCliente("Ana", "1", null, null, "Colombiana");
        luis = hotel.registrarCliente("Luis", "2", null, null, "Peruana");
    }

    // ------------------------------------------------------------ habitaciones y clientes

    @Test
    @DisplayName("No permite dos habitaciones con el mismo número")
    void habitacionDuplicada() {
        assertThrows(HotelException.class,
                () -> hotel.agregarHabitacion(101, TipoHabitacion.SUITE, 4, BigDecimal.TEN));
        assertEquals(2, hotel.getHabitaciones().size());
    }

    @Test
    @DisplayName("Busca habitaciones por número")
    void buscarHabitacion() {
        assertSame(h101, hotel.buscarHabitacion(101).orElseThrow());
        assertFalse(hotel.buscarHabitacion(999).isPresent());
    }

    @Test
    @DisplayName("No permite dos clientes con el mismo documento y asigna ids consecutivos")
    void clienteDuplicado() {
        assertThrows(HotelException.class,
                () -> hotel.registrarCliente("Otra Ana", "1", null, null, "Colombiana"));
        assertEquals(1, ana.getId());
        assertEquals(2, luis.getId());
        assertSame(ana, hotel.buscarCliente("1").orElseThrow());
        assertFalse(hotel.buscarCliente("999").isPresent());
        assertFalse(hotel.buscarCliente(null).isPresent());
    }

    @Test
    @DisplayName("No permite registrar dos empleados con el mismo usuario")
    void empleadoDuplicado() {
        hotel.registrarEmpleado(new Empleado(1, "Laura", "10", null, null, Cargo.RECEPCIONISTA, "laura", "clave123"));

        assertThrows(HotelException.class, () -> hotel.registrarEmpleado(
                new Empleado(2, "Otra", "11", null, null, Cargo.ADMINISTRADOR, "LAURA", "clave456")));
        assertEquals(1, hotel.getEmpleados().size());
    }

    @Test
    @DisplayName("Valida los datos del hotel")
    void datosDelHotel() {
        assertThrows(IllegalArgumentException.class, () -> new Hotel("", "Calle", "1"));
        assertThrows(IllegalArgumentException.class, () -> new Hotel("H", null, "1"));
        assertThrows(IllegalArgumentException.class, () -> new Hotel("H", "Calle", " "));
    }

    // ------------------------------------------------------------ disponibilidad y sobreventa

    @Test
    @DisplayName("buscarDisponibles excluye las habitaciones reservadas y las de mantenimiento")
    void buscarDisponibles() {
        assertEquals(2, hotel.buscarDisponibles(ENTRADA, SALIDA).size());

        hotel.crearReserva(ana, h101, ENTRADA, SALIDA, null);
        assertEquals(List.of(h102), hotel.buscarDisponibles(ENTRADA, SALIDA));

        h102.cambiarEstado(EstadoHabitacion.MANTENIMIENTO);
        assertTrue(hotel.buscarDisponibles(ENTRADA, SALIDA).isEmpty());
    }

    @Test
    @DisplayName("Evita la sobreventa: dos reservas para la misma habitación y fechas cruzadas")
    void sobreventa() {
        hotel.crearReserva(ana, h101, ENTRADA, SALIDA, null);

        HabitacionNoDisponibleException error = assertThrows(HabitacionNoDisponibleException.class,
                () -> hotel.crearReserva(luis, h101, LocalDate.of(2026, 11, 12), LocalDate.of(2026, 11, 15), null));

        assertEquals(101, error.getNumeroHabitacion());
        assertEquals(1, hotel.getReservas().size());
        assertTrue(luis.consultarHistorial().isEmpty());
    }

    @Test
    @DisplayName("Permite reservar la misma habitación en fechas que no se cruzan")
    void reservasSinCruce() {
        hotel.crearReserva(ana, h101, ENTRADA, SALIDA, null);
        Reserva siguiente = hotel.crearReserva(luis, h101, SALIDA, SALIDA.plusDays(2), null);

        assertEquals("RES-0002", siguiente.getCodigo());
        assertEquals(2, hotel.getReservas().size());
    }

    @Test
    @DisplayName("Una reserva fallida no consume el código siguiente")
    void codigoNoSeConsumeAlFallar() {
        hotel.crearReserva(ana, h101, ENTRADA, SALIDA, null);
        assertThrows(HabitacionNoDisponibleException.class,
                () -> hotel.crearReserva(luis, h101, ENTRADA, SALIDA, null));

        Reserva valida = hotel.crearReserva(luis, h102, ENTRADA, SALIDA, null);

        assertEquals("RES-0002", valida.getCodigo());
    }

    @Test
    @DisplayName("Rechaza reservas con fechas inválidas")
    void fechasInvalidas() {
        assertThrows(IllegalArgumentException.class, () -> hotel.crearReserva(ana, h101, SALIDA, ENTRADA, null));
        assertThrows(IllegalArgumentException.class, () -> hotel.crearReserva(ana, h101, ENTRADA, ENTRADA, null));
        assertThrows(IllegalArgumentException.class, () -> hotel.crearReserva(ana, h101, null, SALIDA, null));
    }

    @Test
    @DisplayName("Rechaza clientes, habitaciones o empleados ajenos al hotel")
    void entidadesAjenas() {
        Hotel otro = new Hotel("Otro", "Calle 2", "456");
        Habitacion ajena = otro.agregarHabitacion(101, TipoHabitacion.SENCILLA, 1, BigDecimal.TEN);
        Cliente clienteAjeno = otro.registrarCliente("Zoe", "1", null, null, "Chilena");
        Empleado noRegistrado = new Empleado(5, "Eve", "50", null, null, Cargo.RECEPCIONISTA, "eve", "clave123");

        assertThrows(HotelException.class, () -> hotel.crearReserva(ana, ajena, ENTRADA, SALIDA, null));
        assertThrows(HotelException.class, () -> hotel.crearReserva(clienteAjeno, h101, ENTRADA, SALIDA, null));
        assertThrows(HotelException.class, () -> hotel.crearReserva(ana, h101, ENTRADA, SALIDA, noRegistrado));
    }

    // ------------------------------------------------------------ cancelación

    @Test
    @DisplayName("cancelarReserva libera la habitación y permite reservar de nuevo")
    void cancelarReserva() {
        Reserva reserva = hotel.crearReserva(ana, h101, ENTRADA, SALIDA, null);

        hotel.cancelarReserva(reserva.getCodigo());

        assertEquals(EstadoReserva.CANCELADA, reserva.getEstado());
        hotel.crearReserva(luis, h101, ENTRADA, SALIDA, null);
    }

    @Test
    @DisplayName("cancelarReserva falla si el código no existe")
    void cancelarInexistente() {
        assertThrows(HotelException.class, () -> hotel.cancelarReserva("RES-9999"));
        assertFalse(hotel.buscarReserva("RES-9999").isPresent());
    }

    // ------------------------------------------------------------ reportes

    @Test
    @DisplayName("El reporte calcula noches ocupadas, porcentaje e ingresos del periodo")
    void reporte() {
        LocalDate inicio = LocalDate.of(2026, 11, 1);
        LocalDate fin = LocalDate.of(2026, 11, 10); // 10 días x 2 habitaciones = 20 noches

        // A: 3 noches completas dentro del periodo, pago dentro del periodo
        Reserva a = hotel.crearReserva(ana, h101, LocalDate.of(2026, 11, 3), LocalDate.of(2026, 11, 6), null);
        a.registrarPago(new BigDecimal("100"), MetodoPago.TARJETA, LocalDate.of(2026, 11, 3));

        // B: 4 noches (8 al 12) de las que solo 3 caen en el periodo; un pago dentro y otro fuera
        Reserva b = hotel.crearReserva(luis, h102, LocalDate.of(2026, 11, 8), LocalDate.of(2026, 11, 12), null);
        b.registrarPago(new BigDecimal("50"), MetodoPago.EFECTIVO, LocalDate.of(2026, 11, 9));
        b.registrarPago(new BigDecimal("10"), MetodoPago.EFECTIVO, LocalDate.of(2026, 11, 20));

        // C: cancelada, no debe contar noches
        Reserva c = hotel.crearReserva(ana, h101, LocalDate.of(2026, 11, 7), LocalDate.of(2026, 11, 9), null);
        c.cancelar();

        ReporteOcupacion reporte = hotel.generarReporte(inicio, fin);

        assertEquals(2, reporte.totalHabitaciones());
        assertEquals(20, reporte.nochesDisponibles());
        assertEquals(6, reporte.nochesOcupadas());
        assertEquals(2, reporte.reservasConsideradas());
        assertEquals(new BigDecimal("30.00"), reporte.porcentajeOcupacion());
        assertEquals(new BigDecimal("150.00"), reporte.ingresos());
        assertTrue(reporte.resumen().contains("30.00%"));
    }

    @Test
    @DisplayName("El reporte de un hotel sin reservas es cero")
    void reporteVacio() {
        ReporteOcupacion reporte = hotel.generarReporte(ENTRADA, SALIDA);

        assertEquals(0, reporte.nochesOcupadas());
        assertEquals(new BigDecimal("0.00"), reporte.porcentajeOcupacion());
        assertEquals(new BigDecimal("0.00"), reporte.ingresos());
    }

    @Test
    @DisplayName("El reporte de un hotel sin habitaciones no divide entre cero")
    void reporteSinHabitaciones() {
        Hotel vacio = new Hotel("Vacío", "Calle", "1");

        assertEquals(new BigDecimal("0.00"), vacio.generarReporte(ENTRADA, SALIDA).porcentajeOcupacion());
    }

    @Test
    @DisplayName("El reporte valida el periodo")
    void reporteFechasInvalidas() {
        assertThrows(IllegalArgumentException.class, () -> hotel.generarReporte(SALIDA, ENTRADA));
        assertThrows(IllegalArgumentException.class, () -> hotel.generarReporte(null, SALIDA));
    }

    @Test
    @DisplayName("Un periodo de un solo día es válido")
    void reporteUnDia() {
        hotel.crearReserva(ana, h101, ENTRADA, SALIDA, null);

        ReporteOcupacion reporte = hotel.generarReporte(ENTRADA, ENTRADA);

        assertEquals(2, reporte.nochesDisponibles());
        assertEquals(1, reporte.nochesOcupadas());
        assertEquals(new BigDecimal("50.00"), reporte.porcentajeOcupacion());
    }
}
