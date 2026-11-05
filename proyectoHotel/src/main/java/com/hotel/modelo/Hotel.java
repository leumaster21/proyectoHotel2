package com.hotel.modelo;

import com.hotel.excepciones.HabitacionNoDisponibleException;
import com.hotel.excepciones.HotelException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Punto de entrada del sistema: centraliza habitaciones, clientes, empleados y reservas.
 *
 * <ul>
 *   <li>Las habitaciones son parte del hotel (composición).</li>
 *   <li>Los empleados existen aparte y se registran en él (agregación).</li>
 *   <li>Es quien garantiza que no haya sobreventa al crear reservas.</li>
 * </ul>
 */
public class Hotel {

    private final String nombre;
    private final String direccion;
    private final String telefono;
    private final Map<Integer, Habitacion> habitaciones = new LinkedHashMap<>();
    private final List<Empleado> empleados = new ArrayList<>();
    private final Map<String, Cliente> clientes = new LinkedHashMap<>();
    private final Map<String, Reserva> reservas = new LinkedHashMap<>();
    private int siguienteIdCliente = 1;
    private int siguienteNumeroReserva = 1;

    /**
     * Crea un hotel vacío.
     *
     * @param nombre    nombre del hotel (obligatorio)
     * @param direccion dirección (obligatoria)
     * @param telefono  teléfono (obligatorio)
     * @throws IllegalArgumentException si algún dato está vacío
     */
    public Hotel(String nombre, String direccion, String telefono) {
        this.nombre = Validaciones.texto(nombre, "El nombre del hotel");
        this.direccion = Validaciones.texto(direccion, "La dirección");
        this.telefono = Validaciones.texto(telefono, "El teléfono");
    }

    // ------------------------------------------------------------------ habitaciones

    /**
     * Crea una habitación y la incorpora al hotel (RF2).
     *
     * @param numero      número único de la habitación
     * @param tipo        tipo de habitación
     * @param capacidad   capacidad máxima de huéspedes
     * @param tarifaNoche precio por noche
     * @return la habitación creada
     * @throws HotelException           si ya existe una habitación con ese número
     * @throws IllegalArgumentException si algún dato es inválido
     */
    public Habitacion agregarHabitacion(int numero, TipoHabitacion tipo, int capacidad,
                                        BigDecimal tarifaNoche) {
        if (habitaciones.containsKey(numero)) {
            throw new HotelException("Ya existe la habitación " + numero);
        }
        Habitacion habitacion = new Habitacion(numero, tipo, capacidad, tarifaNoche);
        habitaciones.put(numero, habitacion);
        return habitacion;
    }

    /**
     * Busca una habitación por su número.
     *
     * @param numero número de habitación
     * @return la habitación, o vacío si no existe
     */
    public Optional<Habitacion> buscarHabitacion(int numero) {
        return Optional.ofNullable(habitaciones.get(numero));
    }

    /**
     * Lista las habitaciones libres en un rango de fechas (disponibilidad en tiempo real).
     *
     * @param inicio fecha de entrada
     * @param fin    fecha de salida
     * @return habitaciones disponibles, en el orden en que se crearon
     * @throws IllegalArgumentException si el rango es inválido
     */
    public List<Habitacion> buscarDisponibles(LocalDate inicio, LocalDate fin) {
        Validaciones.rangoFechas(inicio, fin);
        List<Habitacion> libres = new ArrayList<>();
        for (Habitacion habitacion : habitaciones.values()) {
            if (habitacion.estaDisponible(inicio, fin)) {
                libres.add(habitacion);
            }
        }
        return libres;
    }

    /**
     * Devuelve todas las habitaciones del hotel.
     *
     * @return colección de solo lectura
     */
    public Collection<Habitacion> getHabitaciones() {
        return Collections.unmodifiableCollection(habitaciones.values());
    }

    // ------------------------------------------------------------------ empleados

    /**
     * Registra un empleado existente en el hotel.
     *
     * @param empleado empleado a registrar
     * @throws HotelException si ya hay un empleado con el mismo usuario
     */
    public void registrarEmpleado(Empleado empleado) {
        Validaciones.noNulo(empleado, "El empleado");
        boolean repetido = empleados.stream()
                .anyMatch(e -> e.getUsuario().equalsIgnoreCase(empleado.getUsuario()));
        if (repetido) {
            throw new HotelException("Ya existe un empleado con el usuario " + empleado.getUsuario());
        }
        empleados.add(empleado);
    }

    /**
     * Devuelve los empleados registrados.
     *
     * @return lista de solo lectura
     */
    public List<Empleado> getEmpleados() {
        return Collections.unmodifiableList(empleados);
    }

    // ------------------------------------------------------------------ clientes

    /**
     * Registra un cliente nuevo asignándole un id (RF1).
     *
     * @param nombre       nombre completo
     * @param documento    documento de identidad, único en el hotel
     * @param telefono     teléfono (opcional)
     * @param email        correo (opcional)
     * @param nacionalidad nacionalidad
     * @return el cliente creado
     * @throws HotelException           si ya existe un cliente con ese documento
     * @throws IllegalArgumentException si algún dato es inválido
     */
    public Cliente registrarCliente(String nombre, String documento, String telefono,
                                    String email, String nacionalidad) {
        String clave = Validaciones.texto(documento, "El documento");
        if (clientes.containsKey(clave)) {
            throw new HotelException("Ya existe un cliente con el documento " + clave);
        }
        Cliente cliente = new Cliente(siguienteIdCliente, nombre, clave, telefono, email, nacionalidad);
        siguienteIdCliente++;
        clientes.put(clave, cliente);
        return cliente;
    }

    /**
     * Busca un cliente por su documento.
     *
     * @param documento documento de identidad
     * @return el cliente, o vacío si no está registrado
     */
    public Optional<Cliente> buscarCliente(String documento) {
        return documento == null ? Optional.empty() : Optional.ofNullable(clientes.get(documento.trim()));
    }

    // ------------------------------------------------------------------ reservas

    /**
     * Crea una reserva {@link EstadoReserva#PENDIENTE} verificando antes que la habitación
     * esté libre en el rango (RF3). Es la regla que evita la sobreventa.
     *
     * @param cliente    cliente registrado en este hotel
     * @param habitacion habitación de este hotel
     * @param entrada    fecha de entrada
     * @param salida     fecha de salida (posterior a la entrada)
     * @param empleado   empleado que gestiona la reserva, o {@code null} si es una reserva en línea
     * @return la reserva creada
     * @throws HabitacionNoDisponibleException si la habitación está ocupada en ese rango
     * @throws HotelException                  si el cliente, la habitación o el empleado no pertenecen al hotel
     * @throws IllegalArgumentException        si las fechas son inválidas
     */
    public Reserva crearReserva(Cliente cliente, Habitacion habitacion,
                                LocalDate entrada, LocalDate salida, Empleado empleado) {
        Validaciones.noNulo(cliente, "El cliente");
        Validaciones.noNulo(habitacion, "La habitación");
        Validaciones.rangoFechas(entrada, salida);
        if (clientes.get(cliente.getDocumento()) != cliente) {
            throw new HotelException("El cliente no está registrado en este hotel");
        }
        if (habitaciones.get(habitacion.getNumero()) != habitacion) {
            throw new HotelException("La habitación no pertenece a este hotel");
        }
        if (empleado != null && !empleados.contains(empleado)) {
            throw new HotelException("El empleado no está registrado en este hotel");
        }
        if (!habitacion.estaDisponible(entrada, salida)) {
            throw new HabitacionNoDisponibleException(habitacion.getNumero(), entrada, salida);
        }
        String codigo = String.format("RES-%04d", siguienteNumeroReserva);
        Reserva reserva = new Reserva(codigo, cliente, habitacion, entrada, salida, empleado);
        habitacion.registrarReserva(reserva);
        cliente.agregarReserva(reserva);
        reservas.put(codigo, reserva);
        siguienteNumeroReserva++;
        return reserva;
    }

    /**
     * Cancela una reserva a partir de su código (RF3).
     *
     * @param codigo código de la reserva
     * @throws HotelException si la reserva no existe o ya no se puede cancelar
     */
    public void cancelarReserva(String codigo) {
        Reserva reserva = buscarReserva(codigo)
                .orElseThrow(() -> new HotelException("No existe la reserva " + codigo));
        reserva.cancelar();
    }

    /**
     * Busca una reserva por su código.
     *
     * @param codigo código de la reserva
     * @return la reserva, o vacío si no existe
     */
    public Optional<Reserva> buscarReserva(String codigo) {
        return codigo == null ? Optional.empty() : Optional.ofNullable(reservas.get(codigo.trim()));
    }

    /**
     * Devuelve todas las reservas del hotel.
     *
     * @return colección de solo lectura, en orden de creación
     */
    public Collection<Reserva> getReservas() {
        return Collections.unmodifiableCollection(reservas.values());
    }

    // ------------------------------------------------------------------ reportes

    /**
     * Genera el reporte de ocupación e ingresos de un periodo (RF7).
     *
     * <p>Reglas: el periodo incluye ambos extremos; la ocupación cuenta las noches de
     * reservas no canceladas que caen dentro del periodo; los ingresos suman los pagos
     * procesados cuya fecha está en el periodo, sin importar el estado de la reserva.</p>
     *
     * @param inicio primer día del periodo (inclusive)
     * @param fin    último día del periodo (inclusive)
     * @return el reporte calculado
     * @throws IllegalArgumentException si las fechas son nulas o {@code fin} es anterior a {@code inicio}
     */
    public ReporteOcupacion generarReporte(LocalDate inicio, LocalDate fin) {
        Validaciones.noNulo(inicio, "La fecha de inicio");
        Validaciones.noNulo(fin, "La fecha de fin");
        if (fin.isBefore(inicio)) {
            throw new IllegalArgumentException("La fecha final no puede ser anterior a la inicial");
        }
        LocalDate finExclusivo = fin.plusDays(1);
        long dias = ChronoUnit.DAYS.between(inicio, finExclusivo);
        long nochesOcupadas = 0;
        int consideradas = 0;
        BigDecimal ingresos = Montos.CERO;

        for (Reserva reserva : reservas.values()) {
            if (reserva.getEstado() != EstadoReserva.CANCELADA) {
                long noches = reserva.nochesEnPeriodo(inicio, finExclusivo);
                if (noches > 0) {
                    nochesOcupadas += noches;
                    consideradas++;
                }
            }
            for (Pago pago : reserva.getPagos()) {
                boolean enPeriodo = !pago.getFecha().isBefore(inicio) && !pago.getFecha().isAfter(fin);
                if (pago.isProcesado() && enPeriodo) {
                    ingresos = ingresos.add(pago.getMonto());
                }
            }
        }
        return new ReporteOcupacion(inicio, fin, habitaciones.size(),
                dias * habitaciones.size(), nochesOcupadas, consideradas, ingresos);
    }

    /**
     * Devuelve el nombre del hotel.
     *
     * @return nombre
     */
    public String getNombre() {
        return nombre;
    }

    /**
     * Devuelve la dirección del hotel.
     *
     * @return dirección
     */
    public String getDireccion() {
        return direccion;
    }

    /**
     * Devuelve el teléfono del hotel.
     *
     * @return teléfono
     */
    public String getTelefono() {
        return telefono;
    }
}
