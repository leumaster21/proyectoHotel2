package com.hotel;

import com.hotel.excepciones.AccesoDenegadoException;
import com.hotel.excepciones.HotelException;
import com.hotel.modelo.Cargo;
import com.hotel.modelo.Cliente;
import com.hotel.modelo.Empleado;
import com.hotel.modelo.EstadoHabitacion;
import com.hotel.modelo.Habitacion;
import com.hotel.modelo.MetodoPago;
import com.hotel.modelo.Pago;
import com.hotel.modelo.Reserva;
import com.hotel.modelo.ServicioAdicional;
import com.hotel.modelo.TipoHabitacion;
import com.hotel.persistencia.EmpleadoConClave;
import com.hotel.persistencia.HotelPersistente;
import com.hotel.persistencia.RepositorioHotel;

import java.io.InputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Scanner;

/**
 * Menú de consola del sistema. Muestra opciones, lee lo que escribe el usuario y
 * llama a las clases del modelo. Después de cada cambio guarda el hotel con el
 * {@link RepositorioHotel}.
 *
 * <p>Recibe la entrada y la salida como parámetros (en lugar de usar siempre el teclado
 * y la pantalla) para poder probarla con textos escritos de antemano.</p>
 */
public class Consola {

    private static final int INTENTOS_DE_LOGIN = 3;

    private final Scanner teclado;
    private final PrintStream pantalla;
    private final RepositorioHotel repositorio;
    private HotelPersistente hotel;
    private Empleado usuarioActual;

    /**
     * Crea la consola.
     *
     * @param repositorio donde se guardan y cargan los datos
     * @param entrada     de dónde se lee lo que escribe el usuario (normalmente {@code System.in})
     * @param salida      donde se muestran los mensajes (normalmente {@code System.out})
     */
    public Consola(RepositorioHotel repositorio, InputStream entrada, PrintStream salida) {
        this.repositorio = repositorio;
        this.teclado = new Scanner(entrada, StandardCharsets.UTF_8);
        this.pantalla = salida;
    }

    /**
     * Arranca el programa: carga el hotel, pide usuario y contraseña y muestra el menú
     * hasta que el usuario elija salir (o se acabe la entrada de datos).
     */
    public void iniciar() {
        try {
            cargarHotel();
            pantalla.println("=== " + hotel.getNombre() + " - Sistema de gestión ===");
            if (!iniciarSesion()) {
                pantalla.println("Demasiados intentos fallidos. Se cierra el programa.");
                return;
            }
            pantalla.println("Bienvenido(a), " + usuarioActual.getNombre()
                    + " (" + usuarioActual.getCargo() + ")");
            repetirMenu();
        } catch (NoSuchElementException e) {
            pantalla.println();
            pantalla.println("Se terminó la entrada de datos. Se cierra el programa.");
        }
    }

    // ============================================================ inicio

    private void cargarHotel() {
        hotel = repositorio.cargar();
        if (hotel == null) {
            // Primera vez que se ejecuta: no hay nada guardado todavía
            hotel = new HotelPersistente("Hotel Central", "Calle 10 # 20-30", "604 555 0100");
            hotel.registrarEmpleado(new EmpleadoConClave(1, "Administrador", "0001", null, null,
                    Cargo.ADMINISTRADOR, "admin", "admin123"));
            hotel.registrarEmpleado(new EmpleadoConClave(2, "Recepcionista", "0002", null, null,
                    Cargo.RECEPCIONISTA, "recepcion", "recep123"));
            repositorio.guardar(hotel);
            pantalla.println("Primera ejecución: se crearon los usuarios admin / admin123"
                    + " y recepcion / recep123.");
        }
    }

    private boolean iniciarSesion() {
        for (int intento = 1; intento <= INTENTOS_DE_LOGIN; intento++) {
            String usuario = leerTexto("Usuario: ");
            String clave = leerTexto("Contraseña: ");
            for (Empleado empleado : hotel.getEmpleados()) {
                if (empleado.iniciarSesion(usuario, clave)) {
                    usuarioActual = empleado;
                    return true;
                }
            }
            pantalla.println("Usuario o contraseña incorrectos.");
        }
        return false;
    }

    // ============================================================ menú

    private void repetirMenu() {
        boolean seguir = true;
        while (seguir) {
            mostrarMenu();
            String opcion = leerTexto("Elija una opción: ");
            try {
                seguir = ejecutarOpcion(opcion);
            } catch (HotelException e) {
                pantalla.println("Error: " + e.getMessage());
            } catch (IllegalArgumentException e) {
                pantalla.println("Dato inválido: " + e.getMessage());
            }
        }
        usuarioActual.cerrarSesion();
        pantalla.println("Hasta luego.");
    }

    private void mostrarMenu() {
        pantalla.println();
        pantalla.println("----------- MENÚ -----------");
        pantalla.println(" 1. Registrar cliente");
        pantalla.println(" 2. Listar clientes");
        pantalla.println(" 3. Ver habitaciones");
        pantalla.println(" 4. Buscar habitaciones disponibles");
        pantalla.println(" 5. Crear reserva");
        pantalla.println(" 6. Confirmar reserva");
        pantalla.println(" 7. Cancelar reserva");
        pantalla.println(" 8. Check-in");
        pantalla.println(" 9. Check-out");
        pantalla.println("10. Agregar servicio a una reserva");
        pantalla.println("11. Registrar pago");
        pantalla.println("12. Ver una reserva");
        pantalla.println("13. Listar reservas");
        pantalla.println("--- Solo administrador ---");
        pantalla.println("14. Agregar habitación");
        pantalla.println("15. Cambiar tarifa de una habitación");
        pantalla.println("16. Poner/quitar habitación de mantenimiento");
        pantalla.println("17. Registrar empleado");
        pantalla.println("18. Reporte de ocupación e ingresos");
        pantalla.println(" 0. Salir");
    }

    /** Ejecuta la opción elegida. Devuelve false cuando el usuario quiere salir. */
    private boolean ejecutarOpcion(String opcion) {
        switch (opcion) {
            case "1":
                registrarCliente();
                break;
            case "2":
                listarClientes();
                break;
            case "3":
                verHabitaciones();
                break;
            case "4":
                buscarDisponibles();
                break;
            case "5":
                crearReserva();
                break;
            case "6":
                confirmarReserva();
                break;
            case "7":
                cancelarReserva();
                break;
            case "8":
                hacerCheckIn();
                break;
            case "9":
                hacerCheckOut();
                break;
            case "10":
                agregarServicio();
                break;
            case "11":
                registrarPago();
                break;
            case "12":
                verReserva();
                break;
            case "13":
                listarReservas();
                break;
            case "14":
                agregarHabitacion();
                break;
            case "15":
                cambiarTarifa();
                break;
            case "16":
                cambiarEstadoHabitacion();
                break;
            case "17":
                registrarEmpleado();
                break;
            case "18":
                verReporte();
                break;
            case "0":
                return false;
            default:
                pantalla.println("Opción no válida. Escriba un número del menú.");
        }
        return true;
    }

    // ============================================================ clientes

    private void registrarCliente() {
        String nombre = leerTexto("Nombre completo: ");
        String documento = leerTexto("Documento: ");
        String telefono = leerTextoOpcional("Teléfono (Enter para omitir): ");
        String email = leerTextoOpcional("Correo (Enter para omitir): ");
        String nacionalidad = leerTexto("Nacionalidad: ");

        Cliente cliente = hotel.registrarCliente(nombre, documento, telefono, email, nacionalidad);
        guardar();
        pantalla.println("Cliente registrado con id " + cliente.getId() + ".");
    }

    private void listarClientes() {
        if (hotel.getClientes().isEmpty()) {
            pantalla.println("No hay clientes registrados.");
            return;
        }
        for (Cliente c : hotel.getClientes()) {
            pantalla.println(c.obtenerDatos() + " - " + c.getNacionalidad() + " - " + c.getCategoria());
        }
    }

    // ============================================================ habitaciones

    private void verHabitaciones() {
        if (hotel.getHabitaciones().isEmpty()) {
            pantalla.println("Todavía no hay habitaciones. El administrador debe agregarlas (opción 14).");
            return;
        }
        for (Habitacion h : hotel.getHabitaciones()) {
            pantalla.println("Habitación " + h.getNumero() + " | " + h.getTipo()
                    + " | capacidad " + h.getCapacidad()
                    + " | tarifa " + h.getTarifaNoche().toPlainString()
                    + " | " + h.getEstado());
        }
    }

    private void buscarDisponibles() {
        LocalDate entrada = leerFecha("Fecha de entrada (AAAA-MM-DD): ");
        LocalDate salida = leerFecha("Fecha de salida (AAAA-MM-DD): ");
        boolean hayAlguna = false;
        for (Habitacion h : hotel.buscarDisponibles(entrada, salida)) {
            pantalla.println("Habitación " + h.getNumero() + " | " + h.getTipo()
                    + " | capacidad " + h.getCapacidad()
                    + " | tarifa " + h.getTarifaNoche().toPlainString());
            hayAlguna = true;
        }
        if (!hayAlguna) {
            pantalla.println("No hay habitaciones disponibles en esas fechas.");
        }
    }

    private void agregarHabitacion() {
        exigirAdministrador();
        int numero = leerEntero("Número de habitación: ");
        TipoHabitacion tipo = TipoHabitacion.values()[
                elegirOpcion("Tipo de habitación", TipoHabitacion.values())];
        int capacidad = leerEntero("Capacidad (huéspedes): ");
        BigDecimal tarifa = leerDecimal("Tarifa por noche: ");

        hotel.agregarHabitacion(numero, tipo, capacidad, tarifa);
        guardar();
        pantalla.println("Habitación " + numero + " agregada.");
    }

    private void cambiarTarifa() {
        exigirAdministrador();
        Habitacion habitacion = pedirHabitacion();
        BigDecimal tarifa = leerDecimal("Nueva tarifa por noche: ");
        habitacion.setTarifaNoche(tarifa);
        guardar();
        pantalla.println("Tarifa actualizada. Las reservas ya hechas conservan su tarifa anterior.");
    }

    private void cambiarEstadoHabitacion() {
        exigirAdministrador();
        Habitacion habitacion = pedirHabitacion();
        EstadoHabitacion[] permitidos = {EstadoHabitacion.DISPONIBLE, EstadoHabitacion.MANTENIMIENTO};
        EstadoHabitacion nuevo = permitidos[elegirOpcion("Nuevo estado", permitidos)];
        habitacion.cambiarEstado(nuevo);
        guardar();
        pantalla.println("La habitación " + habitacion.getNumero() + " ahora está " + nuevo + ".");
    }

    // ============================================================ reservas

    private void crearReserva() {
        Cliente cliente = pedirCliente();
        Habitacion habitacion = pedirHabitacion();
        LocalDate entrada = leerFecha("Fecha de entrada (AAAA-MM-DD): ");
        LocalDate salida = leerFecha("Fecha de salida (AAAA-MM-DD): ");

        Reserva reserva = hotel.crearReserva(cliente, habitacion, entrada, salida, usuarioActual);
        guardar();
        pantalla.println("Reserva creada: " + reserva.getCodigo() + " (" + reserva.getNoches()
                + " noches, total " + reserva.getTotal().toPlainString() + "). Estado: "
                + reserva.getEstado());
    }

    private void confirmarReserva() {
        Reserva reserva = pedirReserva();
        reserva.confirmar();
        guardar();
        pantalla.println("Reserva " + reserva.getCodigo() + " confirmada.");
    }

    private void cancelarReserva() {
        Reserva reserva = pedirReserva();
        reserva.cancelar();
        guardar();
        pantalla.println("Reserva " + reserva.getCodigo() + " cancelada.");
    }

    private void hacerCheckIn() {
        Reserva reserva = pedirReserva();
        usuarioActual.registrarCheckIn(reserva);
        guardar();
        pantalla.println("Check-in realizado. Habitación " + reserva.getHabitacion().getNumero()
                + " ocupada.");
    }

    private void hacerCheckOut() {
        Reserva reserva = pedirReserva();
        usuarioActual.registrarCheckOut(reserva);
        guardar();
        pantalla.println("Check-out realizado. Habitación " + reserva.getHabitacion().getNumero()
                + " disponible.");
    }

    private void agregarServicio() {
        Reserva reserva = pedirReserva();
        String nombre = leerTexto("Nombre del servicio (ej. Desayuno): ");
        BigDecimal precio = leerDecimal("Precio por unidad: ");
        int cantidad = leerEntero("Cantidad: ");

        int idServicio = reserva.getServicios().size() + 1;
        reserva.agregarServicio(new ServicioAdicional(idServicio, nombre, precio, cantidad));
        guardar();
        pantalla.println("Servicio agregado. Nuevo total: " + reserva.getTotal().toPlainString());
    }

    private void registrarPago() {
        Reserva reserva = pedirReserva();
        pantalla.println("Total: " + reserva.getTotal().toPlainString()
                + " | Pagado: " + reserva.getTotalPagado().toPlainString()
                + " | Saldo: " + reserva.getSaldoPendiente().toPlainString());
        BigDecimal monto = leerDecimal("Monto a pagar: ");
        MetodoPago metodo = MetodoPago.values()[elegirOpcion("Método de pago", MetodoPago.values())];

        Pago pago = reserva.registrarPago(monto, metodo);
        guardar();
        pantalla.println();
        pantalla.println(pago.generarComprobante());
        pantalla.println("Saldo pendiente: " + reserva.getSaldoPendiente().toPlainString());
    }

    private void verReserva() {
        Reserva r = pedirReserva();
        pantalla.println("Reserva " + r.getCodigo() + " - " + r.getEstado());
        pantalla.println("Cliente: " + r.getCliente().getNombre() + " (doc. " + r.getCliente().getDocumento() + ")");
        pantalla.println("Habitación: " + r.getHabitacion().getNumero() + " " + r.getHabitacion().getTipo());
        pantalla.println("Fechas: " + r.getFechaEntrada() + " a " + r.getFechaSalida()
                + " (" + r.getNoches() + " noches)");
        pantalla.println("Tarifa por noche: " + r.getTarifaNoche().toPlainString());
        for (ServicioAdicional s : r.getServicios()) {
            pantalla.println("  Servicio: " + s.getNombre() + " x" + s.getCantidad()
                    + " = " + s.calcularSubtotal().toPlainString());
        }
        for (Pago p : r.getPagos()) {
            pantalla.println("  Pago " + p.getId() + ": " + p.getMonto().toPlainString()
                    + " " + p.getMetodo() + " (" + p.getFecha() + ")");
        }
        pantalla.println("Total: " + r.getTotal().toPlainString()
                + " | Pagado: " + r.getTotalPagado().toPlainString()
                + " | Saldo: " + r.getSaldoPendiente().toPlainString());
    }

    private void listarReservas() {
        if (hotel.getReservas().isEmpty()) {
            pantalla.println("No hay reservas.");
            return;
        }
        for (Reserva r : hotel.getReservas()) {
            pantalla.println(r.getCodigo() + " | " + r.getCliente().getNombre()
                    + " | hab. " + r.getHabitacion().getNumero()
                    + " | " + r.getFechaEntrada() + " a " + r.getFechaSalida()
                    + " | " + r.getEstado());
        }
    }

    // ============================================================ empleados y reportes

    private void registrarEmpleado() {
        exigirAdministrador();
        String nombre = leerTexto("Nombre completo: ");
        String documento = leerTexto("Documento: ");
        Cargo cargo = Cargo.values()[elegirOpcion("Cargo", Cargo.values())];
        String usuario = leerTexto("Usuario: ");
        String clave = leerTexto("Contraseña (mínimo 6 caracteres): ");

        int nuevoId = 1;
        for (Empleado e : hotel.getEmpleados()) {
            if (e.getId() >= nuevoId) {
                nuevoId = e.getId() + 1;
            }
        }
        hotel.registrarEmpleado(new EmpleadoConClave(nuevoId, nombre, documento, null, null, cargo, usuario, clave));
        guardar();
        pantalla.println("Empleado registrado.");
    }

    private void verReporte() {
        exigirAdministrador();
        LocalDate inicio = leerFecha("Fecha inicial (AAAA-MM-DD): ");
        LocalDate fin = leerFecha("Fecha final (AAAA-MM-DD): ");
        pantalla.println(hotel.generarReporte(inicio, fin).resumen());
    }

    // ============================================================ ayudas para pedir datos

    private void guardar() {
        repositorio.guardar(hotel);
    }

    private void exigirAdministrador() {
        if (usuarioActual.getCargo() != Cargo.ADMINISTRADOR) {
            throw new AccesoDenegadoException("Esta opción es solo para el administrador");
        }
    }

    private Cliente pedirCliente() {
        String documento = leerTexto("Documento del cliente: ");
        Optional<Cliente> cliente = hotel.buscarCliente(documento);
        if (!cliente.isPresent()) {
            throw new HotelException("No existe un cliente con el documento " + documento);
        }
        return cliente.get();
    }

    private Habitacion pedirHabitacion() {
        int numero = leerEntero("Número de habitación: ");
        Optional<Habitacion> habitacion = hotel.buscarHabitacion(numero);
        if (!habitacion.isPresent()) {
            throw new HotelException("No existe la habitación " + numero);
        }
        return habitacion.get();
    }

    private Reserva pedirReserva() {
        String codigo = leerTexto("Código de la reserva (ej. RES-0001): ").toUpperCase();
        Optional<Reserva> reserva = hotel.buscarReserva(codigo);
        if (!reserva.isPresent()) {
            throw new HotelException("No existe la reserva " + codigo);
        }
        return reserva.get();
    }

    /** Muestra una lista numerada y devuelve la posición elegida (empezando en 0). */
    private int elegirOpcion(String titulo, Object[] opciones) {
        pantalla.println(titulo + ":");
        for (int i = 0; i < opciones.length; i++) {
            pantalla.println("  " + (i + 1) + ". " + opciones[i]);
        }
        while (true) {
            int numero = leerEntero("Elija un número: ");
            if (numero >= 1 && numero <= opciones.length) {
                return numero - 1;
            }
            pantalla.println("Elija un número entre 1 y " + opciones.length + ".");
        }
    }

    private String leerTexto(String mensaje) {
        pantalla.print(mensaje);
        return teclado.nextLine().trim();
    }

    /** Igual que leerTexto, pero devuelve null si el usuario no escribe nada. */
    private String leerTextoOpcional(String mensaje) {
        String texto = leerTexto(mensaje);
        if (texto.isEmpty()) {
            return null;
        }
        return texto;
    }

    private int leerEntero(String mensaje) {
        while (true) {
            String texto = leerTexto(mensaje);
            try {
                return Integer.parseInt(texto);
            } catch (NumberFormatException e) {
                pantalla.println("Debe escribir un número entero.");
            }
        }
    }

    private BigDecimal leerDecimal(String mensaje) {
        while (true) {
            String texto = leerTexto(mensaje);
            try {
                return new BigDecimal(texto);
            } catch (NumberFormatException e) {
                pantalla.println("Debe escribir un número (use punto para los decimales, ej. 80.50).");
            }
        }
    }

    private LocalDate leerFecha(String mensaje) {
        while (true) {
            String texto = leerTexto(mensaje);
            try {
                return LocalDate.parse(texto);
            } catch (DateTimeParseException e) {
                pantalla.println("Fecha no válida. Use el formato AAAA-MM-DD, por ejemplo 2026-11-10.");
            }
        }
    }
}
