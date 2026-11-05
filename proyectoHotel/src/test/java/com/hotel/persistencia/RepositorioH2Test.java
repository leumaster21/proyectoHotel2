package com.hotel.persistencia;

import com.hotel.excepciones.HabitacionNoDisponibleException;
import com.hotel.excepciones.PersistenciaException;
import com.hotel.modelo.Cargo;
import com.hotel.modelo.Cliente;
import com.hotel.modelo.Empleado;
import com.hotel.modelo.EstadoHabitacion;
import com.hotel.modelo.EstadoReserva;
import com.hotel.modelo.Habitacion;
import com.hotel.modelo.MetodoPago;
import com.hotel.modelo.Reserva;
import com.hotel.modelo.ServicioAdicional;
import com.hotel.modelo.TipoHabitacion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas de {@link RepositorioH2}. Usan una base de datos H2 en memoria, distinta
 * para cada prueba, que desaparece al terminar.
 */
class RepositorioH2Test {

    private static int contador = 0;

    private String url;
    private RepositorioH2 repositorio;

    @BeforeEach
    void preparar() {
        contador++;
        url = "jdbc:h2:mem:repositorio" + contador + ";DB_CLOSE_DELAY=-1";
        repositorio = new RepositorioH2(url);
    }

    /**
     * Arma un hotel con los casos más difíciles de volver a armar:
     * <ul>
     *   <li>RES-0001: en curso (con servicio y pago parcial), en la 101.</li>
     *   <li>RES-0002: pendiente y sin empleado, creada antes de que subiera la tarifa de la 102.</li>
     *   <li>RES-0003: finalizada, en la misma 101 pero en fechas anteriores. Su check-in y check-out
     *       ocurrieron antes del check-in de la RES-0001, aunque tiene un código mayor.</li>
     *   <li>RES-0004: cancelada.</li>
     *   <li>RES-0005: creada en línea (sin empleado) y con check-in hecho después por un empleado.</li>
     *   <li>La 104 está en mantenimiento.</li>
     * </ul>
     */
    private HotelPersistente armarHotelCompleto() {
        HotelPersistente hotel = new HotelPersistente("Hotel Test", "Calle 1 # 2-3", "604 123");
        Habitacion h101 = hotel.agregarHabitacion(101, TipoHabitacion.SENCILLA, 1, new BigDecimal("80"));
        Habitacion h102 = hotel.agregarHabitacion(102, TipoHabitacion.DOBLE, 2, new BigDecimal("120"));
        Habitacion h103 = hotel.agregarHabitacion(103, TipoHabitacion.SUITE, 4, new BigDecimal("250"));
        Habitacion h104 = hotel.agregarHabitacion(104, TipoHabitacion.DOBLE, 2, new BigDecimal("100"));
        h104.cambiarEstado(EstadoHabitacion.MANTENIMIENTO);

        Empleado laura = new EmpleadoConClave(1, "Laura Gómez", "10", "300111", "laura@hotel.com",
                Cargo.RECEPCIONISTA, "laura", "clave123");
        Empleado pedro = new EmpleadoConClave(2, "Pedro Ruiz", "20", null, null,
                Cargo.ADMINISTRADOR, "pedro", "admin123");
        hotel.registrarEmpleado(laura);
        hotel.registrarEmpleado(pedro);
        laura.iniciarSesion("laura", "clave123");

        Cliente ana = hotel.registrarCliente("Ana Pérez", "5566", "3001112233", "ana@correo.com", "Colombiana");
        Cliente luis = hotel.registrarCliente("Luis Torres", "7788", null, null, "Peruana");
        luis.setCategoria("VIP");

        Reserva r1 = hotel.crearReserva(ana, h101, LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 13), laura);
        r1.confirmar();
        r1.agregarServicio(new ServicioAdicional(1, "Desayuno", new BigDecimal("15"), 3));
        r1.registrarPago(new BigDecimal("50"), MetodoPago.TARJETA, LocalDate.of(2026, 11, 10));

        hotel.crearReserva(luis, h102, LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 12), null);
        h102.setTarifaNoche(new BigDecimal("150"));

        Reserva r3 = hotel.crearReserva(ana, h101, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 3), laura);
        r3.confirmar();
        laura.registrarCheckIn(r3);
        r3.registrarPago(new BigDecimal("160"), MetodoPago.EFECTIVO, LocalDate.of(2026, 11, 3));
        laura.registrarCheckOut(r3);

        laura.registrarCheckIn(r1);

        Reserva r4 = hotel.crearReserva(ana, h103, LocalDate.of(2026, 11, 20), LocalDate.of(2026, 11, 22), null);
        r4.cancelar();

        Reserva r5 = hotel.crearReserva(luis, h103, LocalDate.of(2026, 11, 5), LocalDate.of(2026, 11, 6), null);
        r5.confirmar();
        laura.registrarCheckIn(r5);
        return hotel;
    }

    @Test
    @DisplayName("Con la base de datos vacía, cargar devuelve null")
    void baseVacia() {
        assertNull(repositorio.cargar());
    }

    @Test
    @DisplayName("Guarda y carga un hotel sin habitaciones ni reservas")
    void hotelVacio() {
        repositorio.guardar(new HotelPersistente("Mi Hotel", "Calle 5", "555"));

        HotelPersistente cargado = repositorio.cargar();

        assertNotNull(cargado);
        assertEquals("Mi Hotel", cargado.getNombre());
        assertEquals("Calle 5", cargado.getDireccion());
        assertEquals("555", cargado.getTelefono());
        assertTrue(cargado.getHabitaciones().isEmpty());
    }

    @Test
    @DisplayName("Guarda y recupera habitaciones, clientes y empleados")
    void datosBasicos() {
        repositorio.guardar(armarHotelCompleto());

        HotelPersistente hotel = repositorio.cargar();

        assertEquals(4, hotel.getHabitaciones().size());
        assertEquals(EstadoHabitacion.OCUPADA, hotel.buscarHabitacion(101).orElseThrow().getEstado());
        assertEquals(EstadoHabitacion.DISPONIBLE, hotel.buscarHabitacion(102).orElseThrow().getEstado());
        assertEquals(EstadoHabitacion.OCUPADA, hotel.buscarHabitacion(103).orElseThrow().getEstado());
        assertEquals(EstadoHabitacion.MANTENIMIENTO, hotel.buscarHabitacion(104).orElseThrow().getEstado());
        assertEquals(TipoHabitacion.SUITE, hotel.buscarHabitacion(103).orElseThrow().getTipo());
        assertEquals(4, hotel.buscarHabitacion(103).orElseThrow().getCapacidad());
        assertEquals(new BigDecimal("150.00"), hotel.buscarHabitacion(102).orElseThrow().getTarifaNoche());

        assertEquals(2, hotel.getClientes().size());
        Cliente ana = hotel.buscarCliente("5566").orElseThrow();
        assertEquals("Ana Pérez", ana.getNombre());
        assertEquals("ana@correo.com", ana.getEmail());
        assertEquals("Colombiana", ana.getNacionalidad());
        Cliente luis = hotel.buscarCliente("7788").orElseThrow();
        assertEquals("VIP", luis.getCategoria());
        assertNull(luis.getTelefono());
        assertNull(luis.getEmail());

        assertEquals(2, hotel.getEmpleados().size(), "el empleado interno de la carga no debe aparecer");
        assertEquals(Cargo.ADMINISTRADOR, hotel.getEmpleados().get(1).getCargo());
    }

    @Test
    @DisplayName("Guarda y recupera las reservas con su estado, servicios y pagos")
    void reservas() {
        repositorio.guardar(armarHotelCompleto());

        HotelPersistente hotel = repositorio.cargar();

        assertEquals(5, hotel.getReservas().size());

        Reserva r1 = hotel.buscarReserva("RES-0001").orElseThrow();
        assertEquals(EstadoReserva.EN_CURSO, r1.getEstado());
        assertEquals("Ana Pérez", r1.getCliente().getNombre());
        assertEquals(101, r1.getHabitacion().getNumero());
        assertEquals("laura", r1.getEmpleado().getUsuario());
        assertEquals(LocalDate.of(2026, 11, 10), r1.getFechaEntrada());
        assertEquals(LocalDate.of(2026, 11, 13), r1.getFechaSalida());
        assertEquals(1, r1.getServicios().size());
        assertEquals("Desayuno", r1.getServicios().get(0).getNombre());
        assertEquals(new BigDecimal("285.00"), r1.getTotal());
        assertEquals(1, r1.getPagos().size());
        assertEquals(new BigDecimal("50.00"), r1.getPagos().get(0).getMonto());
        assertEquals(MetodoPago.TARJETA, r1.getPagos().get(0).getMetodo());
        assertEquals(LocalDate.of(2026, 11, 10), r1.getPagos().get(0).getFecha());
        assertEquals(new BigDecimal("235.00"), r1.getSaldoPendiente());

        Reserva r2 = hotel.buscarReserva("RES-0002").orElseThrow();
        assertEquals(EstadoReserva.PENDIENTE, r2.getEstado());
        assertNull(r2.getEmpleado());
        assertEquals(new BigDecimal("120.00"), r2.getTarifaNoche(), "conserva la tarifa con la que se reservó");
        assertEquals(new BigDecimal("240.00"), r2.getTotal());

        Reserva r3 = hotel.buscarReserva("RES-0003").orElseThrow();
        assertEquals(EstadoReserva.FINALIZADA, r3.getEstado());
        assertEquals(new BigDecimal("160.00"), r3.getTotal());
        assertEquals(new BigDecimal("0.00"), r3.getSaldoPendiente());

        assertEquals(EstadoReserva.CANCELADA, hotel.buscarReserva("RES-0004").orElseThrow().getEstado());

        Reserva r5 = hotel.buscarReserva("RES-0005").orElseThrow();
        assertEquals(EstadoReserva.EN_CURSO, r5.getEstado());
        assertEquals("laura", r5.getEmpleado().getUsuario(), "el empleado que hizo el check-in queda asignado");

        assertEquals(3, hotel.buscarCliente("5566").orElseThrow().consultarHistorial().size());
    }

    @Test
    @DisplayName("Después de cargar, el hotel sigue evitando la sobreventa y la numeración continúa")
    void sobreventaDespuesDeCargar() {
        repositorio.guardar(armarHotelCompleto());
        HotelPersistente hotel = repositorio.cargar();
        Cliente luis = hotel.buscarCliente("7788").orElseThrow();
        Habitacion h101 = hotel.buscarHabitacion(101).orElseThrow();
        Habitacion h103 = hotel.buscarHabitacion(103).orElseThrow();

        // 101 está tomada por RES-0001 del 10 al 13; 103 por RES-0005 del 5 al 6
        assertThrows(HabitacionNoDisponibleException.class, () -> hotel.crearReserva(luis, h101,
                LocalDate.of(2026, 11, 12), LocalDate.of(2026, 11, 14), null));
        assertThrows(HabitacionNoDisponibleException.class, () -> hotel.crearReserva(luis, h103,
                LocalDate.of(2026, 11, 5), LocalDate.of(2026, 11, 6), null));
        // la reserva cancelada (103 del 20 al 22) ya no bloquea
        Reserva nueva = hotel.crearReserva(luis, h103,
                LocalDate.of(2026, 11, 20), LocalDate.of(2026, 11, 22), null);
        // la 104 sigue en mantenimiento
        assertFalse(hotel.buscarHabitacion(104).orElseThrow()
                .estaDisponible(LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 3)));

        assertEquals("RES-0006", nueva.getCodigo(), "la numeración continúa donde quedó");
        assertEquals(3, hotel.registrarCliente("Nuevo", "9999", null, null, "Chilena").getId());
    }

    @Test
    @DisplayName("Los empleados cargados pueden iniciar sesión y trabajar con su contraseña de siempre")
    void loginDespuesDeCargar() {
        repositorio.guardar(armarHotelCompleto());
        HotelPersistente hotel = repositorio.cargar();
        Empleado laura = hotel.getEmpleados().get(0);

        assertFalse(laura.iniciarSesion("laura", "incorrecta"));
        assertTrue(laura.iniciarSesion("laura", "clave123"));

        Reserva r2 = hotel.buscarReserva("RES-0002").orElseThrow();
        r2.confirmar();
        laura.registrarCheckIn(r2);
        assertEquals(EstadoReserva.EN_CURSO, r2.getEstado());
    }

    @Test
    @DisplayName("Guardar varias veces no duplica los datos")
    void guardarVariasVeces() {
        HotelPersistente hotel = armarHotelCompleto();
        repositorio.guardar(hotel);
        repositorio.guardar(hotel);
        repositorio.guardar(hotel);

        HotelPersistente cargado = repositorio.cargar();

        assertEquals(4, cargado.getHabitaciones().size());
        assertEquals(2, cargado.getClientes().size());
        assertEquals(2, cargado.getEmpleados().size());
        assertEquals(5, cargado.getReservas().size());
    }

    @Test
    @DisplayName("Cargar, guardar y volver a cargar da el mismo resultado")
    void cargarYGuardarDeNuevo() {
        repositorio.guardar(armarHotelCompleto());

        repositorio.guardar(repositorio.cargar());
        HotelPersistente cargado = repositorio.cargar();

        assertEquals(5, cargado.getReservas().size());
        assertEquals(EstadoReserva.EN_CURSO, cargado.buscarReserva("RES-0001").orElseThrow().getEstado());
        assertEquals(EstadoReserva.FINALIZADA, cargado.buscarReserva("RES-0003").orElseThrow().getEstado());
        assertEquals(new BigDecimal("120.00"), cargado.buscarReserva("RES-0002").orElseThrow().getTarifaNoche());
        assertEquals(EstadoHabitacion.OCUPADA, cargado.buscarHabitacion(101).orElseThrow().getEstado());
        assertTrue(cargado.getEmpleados().get(0).iniciarSesion("laura", "clave123"));
    }

    @Test
    @DisplayName("Los datos siguen ahí al abrir otra conexión (como al reiniciar el programa)")
    void persisteEntreInstancias() {
        repositorio.guardar(armarHotelCompleto());

        RepositorioH2 otro = new RepositorioH2(url);
        HotelPersistente cargado = otro.cargar();

        assertNotNull(cargado);
        assertEquals(5, cargado.getReservas().size());
    }

    @Test
    @DisplayName("Si falla al guardar, se deshace todo y los datos anteriores quedan intactos")
    void rollbackSiFalla() {
        repositorio.guardar(armarHotelCompleto());

        // Un nombre de 300 letras no cabe en VARCHAR(255): el INSERT falla a mitad de la transacción
        HotelPersistente malo = new HotelPersistente("Otro Hotel", "Calle 9", "999");
        malo.registrarCliente("X".repeat(300), "123", null, null, "Colombiana");

        assertThrows(PersistenciaException.class, () -> repositorio.guardar(malo));

        HotelPersistente cargado = repositorio.cargar();
        assertEquals("Hotel Test", cargado.getNombre());
        assertEquals(5, cargado.getReservas().size());
        assertEquals(2, cargado.getClientes().size());
    }

    @Test
    @DisplayName("No guarda empleados que no sean EmpleadoConClave y deja los datos anteriores intactos")
    void empleadoSinClaveGuardable() {
        repositorio.guardar(armarHotelCompleto());

        HotelPersistente otro = new HotelPersistente("Otro Hotel", "Calle 9", "999");
        otro.registrarEmpleado(new Empleado(1, "Común", "11", null, null,
                Cargo.RECEPCIONISTA, "comun", "clave123"));

        assertThrows(PersistenciaException.class, () -> repositorio.guardar(otro));

        assertEquals("Hotel Test", repositorio.cargar().getNombre());
    }

    @Test
    @DisplayName("Una URL inválida produce PersistenciaException")
    void urlInvalida() {
        assertThrows(PersistenciaException.class, () -> new RepositorioH2("jdbc:noexiste:abc"));
    }
}
