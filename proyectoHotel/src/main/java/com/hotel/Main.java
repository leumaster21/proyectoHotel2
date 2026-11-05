package com.hotel;

import com.hotel.excepciones.HabitacionNoDisponibleException;
import com.hotel.modelo.Cargo;
import com.hotel.modelo.Cliente;
import com.hotel.modelo.Empleado;
import com.hotel.modelo.Habitacion;
import com.hotel.modelo.Hotel;
import com.hotel.modelo.MetodoPago;
import com.hotel.modelo.Pago;
import com.hotel.modelo.Reserva;
import com.hotel.modelo.ServicioAdicional;
import com.hotel.modelo.TipoHabitacion;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Demostración de consola del flujo completo: crear hotel, reservar, evitar la
 * sobreventa, check-in, servicios, pago, check-out y reporte.
 */
public final class Main {

    private Main() {
    }

    /**
     * Ejecuta el escenario de demostración.
     *
     * @param args no se usan
     */
    public static void main(String[] args) {
        Hotel hotel = new Hotel("Hotel Central", "Calle 10 # 20-30", "604 555 0100");
        Habitacion h101 = hotel.agregarHabitacion(101, TipoHabitacion.SENCILLA, 1, new BigDecimal("80"));
        Habitacion h201 = hotel.agregarHabitacion(201, TipoHabitacion.DOBLE, 2, new BigDecimal("120"));
        hotel.agregarHabitacion(301, TipoHabitacion.SUITE, 4, new BigDecimal("250"));

        Empleado recepcionista = new Empleado(1, "Laura Gómez", "1020304050", "3001112233",
                "laura@hotelcentral.com", Cargo.RECEPCIONISTA, "lgomez", "secreto123");
        hotel.registrarEmpleado(recepcionista);
        recepcionista.iniciarSesion("lgomez", "secreto123");

        Cliente ana = hotel.registrarCliente("Ana Pérez", "5566778899", "3105554433",
                "ana@correo.com", "Colombiana");
        Cliente luis = hotel.registrarCliente("Luis Torres", "1122334455", null, null, "Peruana");

        LocalDate entrada = LocalDate.of(2026, 11, 10);
        LocalDate salida = LocalDate.of(2026, 11, 13);

        System.out.println("== 1. Disponibilidad " + entrada + " a " + salida + " ==");
        hotel.buscarDisponibles(entrada, salida)
                .forEach(h -> System.out.println("  Habitación " + h.getNumero() + " (" + h.getTipo() + ")"));

        System.out.println("\n== 2. Reserva de Ana en la 201 ==");
        Reserva reserva = hotel.crearReserva(ana, h201, entrada, salida, recepcionista);
        reserva.confirmar();
        System.out.println("  " + reserva.getCodigo() + " " + reserva.getEstado()
                + " | noches: " + reserva.getNoches() + " | total: " + reserva.getTotal());

        System.out.println("\n== 3. Intento de sobreventa (Luis, misma habitación, fechas cruzadas) ==");
        try {
            hotel.crearReserva(luis, h201, LocalDate.of(2026, 11, 12), LocalDate.of(2026, 11, 14), recepcionista);
        } catch (HabitacionNoDisponibleException e) {
            System.out.println("  Rechazada: " + e.getMessage());
        }

        System.out.println("\n== 4. Check-in, servicios y pago ==");
        recepcionista.registrarCheckIn(reserva);
        System.out.println("  Habitación 201: " + h201.getEstado() + " | reserva: " + reserva.getEstado());
        reserva.agregarServicio(new ServicioAdicional(1, "Desayuno", new BigDecimal("15"), 3));
        reserva.agregarServicio(new ServicioAdicional(2, "Lavandería", new BigDecimal("20"), 1));
        System.out.println("  Total con servicios: " + reserva.getTotal());
        Pago anticipo = reserva.registrarPago(new BigDecimal("200"), MetodoPago.TARJETA, entrada);
        System.out.println("  Saldo tras anticipo: " + reserva.getSaldoPendiente());
        Pago saldo = reserva.registrarPago(reserva.getSaldoPendiente(), MetodoPago.EFECTIVO, salida);
        System.out.println("\n" + anticipo.generarComprobante());
        System.out.println("\n" + saldo.generarComprobante());

        System.out.println("\n== 5. Check-out ==");
        recepcionista.registrarCheckOut(reserva);
        System.out.println("  Habitación 201: " + h201.getEstado() + " | reserva: " + reserva.getEstado());

        System.out.println("\n== 6. Reserva en línea de Luis (sin empleado) y cancelación ==");
        Reserva enLinea = luis.solicitarReserva(hotel, h101, entrada, salida);
        System.out.println("  " + enLinea.getCodigo() + " " + enLinea.getEstado());
        hotel.cancelarReserva(enLinea.getCodigo());
        System.out.println("  Tras cancelar: " + enLinea.getEstado()
                + " | 101 libre: " + h101.estaDisponible(entrada, salida));

        System.out.println("\n== 7. Reporte ==");
        System.out.println(hotel.generarReporte(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30)).resumen());
    }
}
