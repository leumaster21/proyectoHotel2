package com.hotel.persistencia;

import com.hotel.excepciones.PersistenciaException;
import com.hotel.modelo.Cargo;
import com.hotel.modelo.Cliente;
import com.hotel.modelo.Empleado;
import com.hotel.modelo.EstadoHabitacion;
import com.hotel.modelo.EstadoReserva;
import com.hotel.modelo.Habitacion;
import com.hotel.modelo.MetodoPago;
import com.hotel.modelo.Pago;
import com.hotel.modelo.Reserva;
import com.hotel.modelo.ServicioAdicional;
import com.hotel.modelo.TipoHabitacion;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Guarda y carga el hotel en una base de datos H2 usando JDBC.
 *
 * <p>Forma de trabajo (a propósito sencilla):</p>
 * <ul>
 *   <li>{@link #guardar(HotelPersistente)} borra el contenido de las tablas y vuelve a insertar
 *       todo el hotel, dentro de una sola transacción. Si algo falla se hace
 *       <em>rollback</em> y la base de datos queda como estaba.</li>
 *   <li>{@link #cargar()} lee las tablas y vuelve a armar el hotel <strong>repitiendo las
 *       operaciones normales</strong> (agregar habitaciones, registrar clientes, crear reservas,
 *       agregar servicios, registrar pagos, confirmar, hacer check-in...). Así no hace falta
 *       modificar las clases del modelo.</li>
 * </ul>
 *
 * <p>Las tablas se crean solas la primera vez (CREATE TABLE IF NOT EXISTS).</p>
 */
public class RepositorioH2 implements RepositorioHotel {

    /** URL para guardar los datos en el archivo {@code datos/hotel.mv.db} junto al proyecto. */
    public static final String URL_ARCHIVO = "jdbc:h2:./datos/hotel";

    private static final String USUARIO_BD = "sa";
    private static final String CLAVE_BD = "";

    // ---------------------------------------------------------------- crear tablas

    private static final String[] CREAR_TABLAS = {
        "CREATE TABLE IF NOT EXISTS hotel ("
            + "id INT PRIMARY KEY, "
            + "nombre VARCHAR(255) NOT NULL, "
            + "direccion VARCHAR(255) NOT NULL, "
            + "telefono VARCHAR(255) NOT NULL)",

        "CREATE TABLE IF NOT EXISTS empleado ("
            + "id INT PRIMARY KEY, "
            + "nombre VARCHAR(255) NOT NULL, "
            + "documento VARCHAR(255) NOT NULL UNIQUE, "
            + "telefono VARCHAR(255), "
            + "email VARCHAR(255), "
            + "cargo VARCHAR(30) NOT NULL, "
            + "usuario VARCHAR(255) NOT NULL UNIQUE, "
            + "sal VARCHAR(255) NOT NULL, "
            + "hash_contrasena VARCHAR(255) NOT NULL)",

        "CREATE TABLE IF NOT EXISTS cliente ("
            + "id INT PRIMARY KEY, "
            + "nombre VARCHAR(255) NOT NULL, "
            + "documento VARCHAR(255) NOT NULL UNIQUE, "
            + "telefono VARCHAR(255), "
            + "email VARCHAR(255), "
            + "nacionalidad VARCHAR(255) NOT NULL, "
            + "categoria VARCHAR(255) NOT NULL)",

        "CREATE TABLE IF NOT EXISTS habitacion ("
            + "numero INT PRIMARY KEY, "
            + "tipo VARCHAR(30) NOT NULL, "
            + "capacidad INT NOT NULL, "
            + "tarifa_noche DECIMAL(12,2) NOT NULL, "
            + "estado VARCHAR(30) NOT NULL)",

        "CREATE TABLE IF NOT EXISTS reserva ("
            + "codigo VARCHAR(30) PRIMARY KEY, "
            + "cliente_id INT NOT NULL, "
            + "numero_habitacion INT NOT NULL, "
            + "empleado_id INT, "
            + "fecha_entrada DATE NOT NULL, "
            + "fecha_salida DATE NOT NULL, "
            + "tarifa_noche DECIMAL(12,2) NOT NULL, "
            + "estado VARCHAR(30) NOT NULL, "
            + "FOREIGN KEY (cliente_id) REFERENCES cliente(id), "
            + "FOREIGN KEY (numero_habitacion) REFERENCES habitacion(numero), "
            + "FOREIGN KEY (empleado_id) REFERENCES empleado(id))",

        "CREATE TABLE IF NOT EXISTS servicio_adicional ("
            + "reserva_codigo VARCHAR(30) NOT NULL, "
            + "orden INT NOT NULL, "
            + "servicio_id INT NOT NULL, "
            + "nombre VARCHAR(255) NOT NULL, "
            + "precio DECIMAL(12,2) NOT NULL, "
            + "cantidad INT NOT NULL, "
            + "PRIMARY KEY (reserva_codigo, orden), "
            + "FOREIGN KEY (reserva_codigo) REFERENCES reserva(codigo))",

        "CREATE TABLE IF NOT EXISTS pago ("
            + "reserva_codigo VARCHAR(30) NOT NULL, "
            + "id INT NOT NULL, "
            + "monto DECIMAL(12,2) NOT NULL, "
            + "metodo VARCHAR(30) NOT NULL, "
            + "fecha DATE NOT NULL, "
            + "PRIMARY KEY (reserva_codigo, id), "
            + "FOREIGN KEY (reserva_codigo) REFERENCES reserva(codigo))"
    };

    // ---------------------------------------------------------------- borrar (en orden: primero las tablas hijas)

    private static final String[] BORRAR_TABLAS = {
        "DELETE FROM pago",
        "DELETE FROM servicio_adicional",
        "DELETE FROM reserva",
        "DELETE FROM habitacion",
        "DELETE FROM cliente",
        "DELETE FROM empleado",
        "DELETE FROM hotel"
    };

    // ---------------------------------------------------------------- insertar

    private static final String INSERTAR_HOTEL =
        "INSERT INTO hotel (id, nombre, direccion, telefono) VALUES (?, ?, ?, ?)";

    private static final String INSERTAR_EMPLEADO =
        "INSERT INTO empleado (id, nombre, documento, telefono, email, cargo, usuario, sal, hash_contrasena) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String INSERTAR_CLIENTE =
        "INSERT INTO cliente (id, nombre, documento, telefono, email, nacionalidad, categoria) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?)";

    private static final String INSERTAR_HABITACION =
        "INSERT INTO habitacion (numero, tipo, capacidad, tarifa_noche, estado) VALUES (?, ?, ?, ?, ?)";

    private static final String INSERTAR_RESERVA =
        "INSERT INTO reserva (codigo, cliente_id, numero_habitacion, empleado_id, "
            + "fecha_entrada, fecha_salida, tarifa_noche, estado) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String INSERTAR_SERVICIO =
        "INSERT INTO servicio_adicional (reserva_codigo, orden, servicio_id, nombre, precio, cantidad) "
            + "VALUES (?, ?, ?, ?, ?, ?)";

    private static final String INSERTAR_PAGO =
        "INSERT INTO pago (reserva_codigo, id, monto, metodo, fecha) VALUES (?, ?, ?, ?, ?)";

    // ---------------------------------------------------------------- consultar

    private static final String LEER_HOTEL =
        "SELECT nombre, direccion, telefono FROM hotel";

    private static final String LEER_EMPLEADOS =
        "SELECT id, nombre, documento, telefono, email, cargo, usuario, sal, hash_contrasena "
            + "FROM empleado ORDER BY id";

    private static final String LEER_CLIENTES =
        "SELECT id, nombre, documento, telefono, email, nacionalidad, categoria "
            + "FROM cliente ORDER BY id";

    private static final String LEER_HABITACIONES =
        "SELECT numero, tipo, capacidad, tarifa_noche, estado FROM habitacion ORDER BY numero";

    private static final String LEER_SERVICIOS =
        "SELECT reserva_codigo, servicio_id, nombre, precio, cantidad "
            + "FROM servicio_adicional ORDER BY reserva_codigo, orden";

    private static final String LEER_PAGOS =
        "SELECT reserva_codigo, monto, metodo, fecha FROM pago ORDER BY reserva_codigo, id";

    // Se ordena por la longitud del código y luego por el código para que RES-10000 quede después de RES-9999.
    private static final String LEER_RESERVAS =
        "SELECT codigo, cliente_id, numero_habitacion, empleado_id, fecha_entrada, fecha_salida, "
            + "tarifa_noche, estado FROM reserva ORDER BY LENGTH(codigo), codigo";

    private final String url;

    /**
     * Crea el repositorio y se asegura de que las tablas existan.
     *
     * @param url URL JDBC de H2; por ejemplo {@link #URL_ARCHIVO} o, para pruebas,
     *            {@code jdbc:h2:mem:prueba;DB_CLOSE_DELAY=-1}
     * @throws PersistenciaException si no se puede conectar o crear las tablas
     */
    public RepositorioH2(String url) {
        this.url = url;
        crearTablas();
    }

    // ================================================================ guardar

    @Override
    public void guardar(HotelPersistente hotel) {
        Connection con = null;
        try {
            con = abrirConexion();
            con.setAutoCommit(false);
            borrarTodo(con);
            guardarHotel(con, hotel);
            guardarEmpleados(con, hotel);
            guardarClientes(con, hotel);
            guardarHabitaciones(con, hotel);
            guardarReservas(con, hotel);
            con.commit();
        } catch (SQLException e) {
            deshacer(con);
            throw new PersistenciaException("No se pudo guardar en la base de datos: " + e.getMessage(), e);
        } catch (RuntimeException e) {
            deshacer(con);
            throw e;
        } finally {
            cerrar(con);
        }
    }

    private void borrarTodo(Connection con) throws SQLException {
        try (Statement st = con.createStatement()) {
            for (String sql : BORRAR_TABLAS) {
                st.executeUpdate(sql);
            }
        }
    }

    private void guardarHotel(Connection con, HotelPersistente hotel) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(INSERTAR_HOTEL)) {
            ps.setInt(1, 1);
            ps.setString(2, hotel.getNombre());
            ps.setString(3, hotel.getDireccion());
            ps.setString(4, hotel.getTelefono());
            ps.executeUpdate();
        }
    }

    private void guardarEmpleados(Connection con, HotelPersistente hotel) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(INSERTAR_EMPLEADO)) {
            for (Empleado empleado : hotel.getEmpleados()) {
                if (!(empleado instanceof EmpleadoConClave)) {
                    throw new PersistenciaException("El empleado " + empleado.getUsuario()
                            + " no es un EmpleadoConClave, no se puede guardar su contraseña", null);
                }
                EmpleadoConClave e = (EmpleadoConClave) empleado;
                ps.setInt(1, e.getId());
                ps.setString(2, e.getNombre());
                ps.setString(3, e.getDocumento());
                ps.setString(4, e.getTelefono());
                ps.setString(5, e.getEmail());
                ps.setString(6, e.getCargo().name());
                ps.setString(7, e.getUsuario());
                ps.setString(8, e.getSalBase64());
                ps.setString(9, e.getHashBase64());
                ps.executeUpdate();
            }
        }
    }

    private void guardarClientes(Connection con, HotelPersistente hotel) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(INSERTAR_CLIENTE)) {
            for (Cliente c : hotel.getClientes()) {
                ps.setInt(1, c.getId());
                ps.setString(2, c.getNombre());
                ps.setString(3, c.getDocumento());
                ps.setString(4, c.getTelefono());
                ps.setString(5, c.getEmail());
                ps.setString(6, c.getNacionalidad());
                ps.setString(7, c.getCategoria());
                ps.executeUpdate();
            }
        }
    }

    private void guardarHabitaciones(Connection con, HotelPersistente hotel) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(INSERTAR_HABITACION)) {
            for (Habitacion h : hotel.getHabitaciones()) {
                ps.setInt(1, h.getNumero());
                ps.setString(2, h.getTipo().name());
                ps.setInt(3, h.getCapacidad());
                ps.setBigDecimal(4, h.getTarifaNoche());
                ps.setString(5, h.getEstado().name());
                ps.executeUpdate();
            }
        }
    }

    private void guardarReservas(Connection con, HotelPersistente hotel) throws SQLException {
        try (PreparedStatement psReserva = con.prepareStatement(INSERTAR_RESERVA);
             PreparedStatement psServicio = con.prepareStatement(INSERTAR_SERVICIO);
             PreparedStatement psPago = con.prepareStatement(INSERTAR_PAGO)) {

            for (Reserva r : hotel.getReservas()) {
                psReserva.setString(1, r.getCodigo());
                psReserva.setInt(2, r.getCliente().getId());
                psReserva.setInt(3, r.getHabitacion().getNumero());
                if (r.getEmpleado() == null) {
                    psReserva.setNull(4, Types.INTEGER);
                } else {
                    psReserva.setInt(4, r.getEmpleado().getId());
                }
                psReserva.setDate(5, java.sql.Date.valueOf(r.getFechaEntrada()));
                psReserva.setDate(6, java.sql.Date.valueOf(r.getFechaSalida()));
                psReserva.setBigDecimal(7, r.getTarifaNoche());
                psReserva.setString(8, r.getEstado().name());
                psReserva.executeUpdate();

                int orden = 1;
                for (ServicioAdicional s : r.getServicios()) {
                    psServicio.setString(1, r.getCodigo());
                    psServicio.setInt(2, orden);
                    psServicio.setInt(3, s.getId());
                    psServicio.setString(4, s.getNombre());
                    psServicio.setBigDecimal(5, s.getPrecio());
                    psServicio.setInt(6, s.getCantidad());
                    psServicio.executeUpdate();
                    orden++;
                }

                for (Pago p : r.getPagos()) {
                    psPago.setString(1, r.getCodigo());
                    psPago.setInt(2, p.getId());
                    psPago.setBigDecimal(3, p.getMonto());
                    psPago.setString(4, p.getMetodo().name());
                    psPago.setDate(5, java.sql.Date.valueOf(p.getFecha()));
                    psPago.executeUpdate();
                }
            }
        }
    }

    // ================================================================ cargar

    @Override
    public HotelPersistente cargar() {
        Connection con = null;
        try {
            con = abrirConexion();
            HotelPersistente hotel = leerHotel(con);
            if (hotel == null) {
                return null;
            }
            // 1) Se leen las filas de las tablas
            List<FilaHabitacion> habitaciones = leerFilasHabitaciones(con);
            List<FilaReserva> reservas = leerFilasReservas(con);
            Map<String, List<ServicioAdicional>> servicios = leerServicios(con);
            Map<String, List<FilaPago>> pagos = leerPagos(con);

            // 2) Se vuelve a armar el hotel usando las operaciones normales del modelo
            restaurarHabitaciones(hotel, habitaciones);
            Map<Integer, Empleado> empleados = restaurarEmpleados(con, hotel);
            Map<Integer, Cliente> clientes = restaurarClientes(con, hotel);
            restaurarReservas(hotel, reservas, servicios, pagos, clientes, empleados);
            ponerEstadosDeHabitaciones(hotel, habitaciones);
            return hotel;
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo leer la base de datos: " + e.getMessage(), e);
        } finally {
            cerrar(con);
        }
    }

    private HotelPersistente leerHotel(Connection con) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(LEER_HOTEL);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return new HotelPersistente(rs.getString("nombre"), rs.getString("direccion"),
                        rs.getString("telefono"));
            }
            return null;
        }
    }

    // ---------------------------------------------------------------- leer filas

    private List<FilaHabitacion> leerFilasHabitaciones(Connection con) throws SQLException {
        List<FilaHabitacion> lista = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(LEER_HABITACIONES);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                FilaHabitacion fila = new FilaHabitacion();
                fila.numero = rs.getInt("numero");
                fila.tipo = TipoHabitacion.valueOf(rs.getString("tipo"));
                fila.capacidad = rs.getInt("capacidad");
                fila.tarifaNoche = rs.getBigDecimal("tarifa_noche");
                fila.estado = EstadoHabitacion.valueOf(rs.getString("estado"));
                lista.add(fila);
            }
        }
        return lista;
    }

    private List<FilaReserva> leerFilasReservas(Connection con) throws SQLException {
        List<FilaReserva> lista = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(LEER_RESERVAS);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                FilaReserva fila = new FilaReserva();
                fila.codigo = rs.getString("codigo");
                fila.clienteId = rs.getInt("cliente_id");
                fila.numeroHabitacion = rs.getInt("numero_habitacion");
                int empleadoId = rs.getInt("empleado_id");
                if (rs.wasNull()) {
                    fila.empleadoId = null;
                } else {
                    fila.empleadoId = empleadoId;
                }
                fila.entrada = rs.getDate("fecha_entrada").toLocalDate();
                fila.salida = rs.getDate("fecha_salida").toLocalDate();
                fila.tarifaNoche = rs.getBigDecimal("tarifa_noche");
                fila.estado = EstadoReserva.valueOf(rs.getString("estado"));
                lista.add(fila);
            }
        }
        return lista;
    }

    private Map<String, List<ServicioAdicional>> leerServicios(Connection con) throws SQLException {
        Map<String, List<ServicioAdicional>> porReserva = new HashMap<>();
        try (PreparedStatement ps = con.prepareStatement(LEER_SERVICIOS);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String codigo = rs.getString("reserva_codigo");
                ServicioAdicional servicio = new ServicioAdicional(
                        rs.getInt("servicio_id"),
                        rs.getString("nombre"),
                        rs.getBigDecimal("precio"),
                        rs.getInt("cantidad"));
                if (!porReserva.containsKey(codigo)) {
                    porReserva.put(codigo, new ArrayList<>());
                }
                porReserva.get(codigo).add(servicio);
            }
        }
        return porReserva;
    }

    private Map<String, List<FilaPago>> leerPagos(Connection con) throws SQLException {
        Map<String, List<FilaPago>> porReserva = new HashMap<>();
        try (PreparedStatement ps = con.prepareStatement(LEER_PAGOS);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String codigo = rs.getString("reserva_codigo");
                FilaPago pago = new FilaPago();
                pago.monto = rs.getBigDecimal("monto");
                pago.metodo = MetodoPago.valueOf(rs.getString("metodo"));
                pago.fecha = rs.getDate("fecha").toLocalDate();
                if (!porReserva.containsKey(codigo)) {
                    porReserva.put(codigo, new ArrayList<>());
                }
                porReserva.get(codigo).add(pago);
            }
        }
        return porReserva;
    }

    // ---------------------------------------------------------------- volver a armar el hotel

    /** Crea las habitaciones con su tarifa actual. Los estados se ponen al final. */
    private void restaurarHabitaciones(HotelPersistente hotel, List<FilaHabitacion> filas) {
        for (FilaHabitacion f : filas) {
            hotel.agregarHabitacion(f.numero, f.tipo, f.capacidad, f.tarifaNoche);
        }
    }

    private Map<Integer, Empleado> restaurarEmpleados(Connection con, HotelPersistente hotel)
            throws SQLException {
        Map<Integer, Empleado> porId = new HashMap<>();
        try (PreparedStatement ps = con.prepareStatement(LEER_EMPLEADOS);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                EmpleadoConClave e = EmpleadoConClave.restaurar(
                        rs.getInt("id"),
                        rs.getString("nombre"),
                        rs.getString("documento"),
                        rs.getString("telefono"),
                        rs.getString("email"),
                        Cargo.valueOf(rs.getString("cargo")),
                        rs.getString("usuario"),
                        rs.getString("sal"),
                        rs.getString("hash_contrasena"));
                hotel.registrarEmpleado(e);
                porId.put(e.getId(), e);
            }
        }
        return porId;
    }

    /**
     * Registra los clientes en orden de id. El hotel les asigna los ids 1, 2, 3... en orden, que
     * son los mismos con los que se guardaron (el sistema no borra clientes, así que no hay huecos).
     */
    private Map<Integer, Cliente> restaurarClientes(Connection con, HotelPersistente hotel)
            throws SQLException {
        Map<Integer, Cliente> porId = new HashMap<>();
        try (PreparedStatement ps = con.prepareStatement(LEER_CLIENTES);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int idGuardado = rs.getInt("id");
                Cliente c = hotel.registrarCliente(
                        rs.getString("nombre"),
                        rs.getString("documento"),
                        rs.getString("telefono"),
                        rs.getString("email"),
                        rs.getString("nacionalidad"));
                c.setCategoria(rs.getString("categoria"));
                if (c.getId() != idGuardado) {
                    throw new PersistenciaException("El cliente " + c.getDocumento()
                            + " tenía el id " + idGuardado + " pero ahora tiene el " + c.getId()
                            + ". Los datos guardados no son consistentes.", null);
                }
                porId.put(idGuardado, c);
            }
        }
        return porId;
    }

    /**
     * Vuelve a crear las reservas en orden de código, como se crearon originalmente.
     * Para cada una: se crea, se le agregan servicios y pagos y al final se lleva a su estado.
     */
    private void restaurarReservas(HotelPersistente hotel, List<FilaReserva> filas,
                                   Map<String, List<ServicioAdicional>> servicios,
                                   Map<String, List<FilaPago>> pagos,
                                   Map<Integer, Cliente> clientes, Map<Integer, Empleado> empleados) {
        // Empleado interno, solo para poder hacer check-in y check-out mientras se restaura.
        // No se registra en el hotel, así que no se guarda ni aparece en ninguna parte.
        Empleado ayudante = new Empleado(0, "Sistema", "SISTEMA", null, null,
                Cargo.RECEPCIONISTA, "sistema", "clave-interna");
        ayudante.iniciarSesion("sistema", "clave-interna");

        for (FilaReserva f : filas) {
            Cliente cliente = clientes.get(f.clienteId);
            Habitacion habitacion = hotel.buscarHabitacion(f.numeroHabitacion).get();
            Empleado empleado = null;
            if (f.empleadoId != null) {
                empleado = empleados.get(f.empleadoId);
            }

            // La reserva toma la tarifa que tenga la habitación en ese momento. Se le pone la
            // tarifa que tenía la reserva y luego se devuelve la tarifa actual.
            BigDecimal tarifaActual = habitacion.getTarifaNoche();
            habitacion.setTarifaNoche(f.tarifaNoche);
            Reserva reserva = hotel.crearReserva(cliente, habitacion, f.entrada, f.salida, empleado);
            habitacion.setTarifaNoche(tarifaActual);

            if (!reserva.getCodigo().equals(f.codigo)) {
                throw new PersistenciaException("La reserva " + f.codigo + " se recreó como "
                        + reserva.getCodigo() + ". Los datos guardados no son consistentes.", null);
            }

            List<ServicioAdicional> susServicios = servicios.get(f.codigo);
            if (susServicios != null) {
                for (ServicioAdicional s : susServicios) {
                    reserva.agregarServicio(s);
                }
            }
            List<FilaPago> susPagos = pagos.get(f.codigo);
            if (susPagos != null) {
                for (FilaPago p : susPagos) {
                    reserva.registrarPago(p.monto, p.metodo, p.fecha);
                }
            }
            llevarAlEstado(reserva, f.estado, ayudante);
        }
    }

    /** Lleva una reserva recién creada (PENDIENTE) al estado con el que estaba guardada. */
    private void llevarAlEstado(Reserva reserva, EstadoReserva estado, Empleado ayudante) {
        if (estado == EstadoReserva.CONFIRMADA) {
            reserva.confirmar();
        } else if (estado == EstadoReserva.CANCELADA) {
            reserva.cancelar();
        } else if (estado == EstadoReserva.EN_CURSO || estado == EstadoReserva.FINALIZADA) {
            if (reserva.getEmpleado() == null) {
                throw new PersistenciaException("La reserva " + reserva.getCodigo()
                        + " está " + estado + " pero no tiene empleado.", null);
            }
            reserva.confirmar();
            liberarHabitacion(reserva.getHabitacion());
            ayudante.registrarCheckIn(reserva);
            if (estado == EstadoReserva.FINALIZADA) {
                ayudante.registrarCheckOut(reserva);
            }
        }
        // PENDIENTE: no hay nada que hacer
    }

    /** Una habitación ocupada no puede volver a ocuparse; se deja disponible antes de un nuevo check-in. */
    private void liberarHabitacion(Habitacion habitacion) {
        if (habitacion.getEstado() == EstadoHabitacion.OCUPADA) {
            habitacion.cambiarEstado(EstadoHabitacion.DISPONIBLE);
        }
    }

    /** Al final se deja cada habitación en el estado con el que estaba guardada. */
    private void ponerEstadosDeHabitaciones(HotelPersistente hotel, List<FilaHabitacion> filas) {
        for (FilaHabitacion f : filas) {
            Habitacion habitacion = hotel.buscarHabitacion(f.numero).get();
            liberarHabitacion(habitacion);
            if (f.estado != EstadoHabitacion.DISPONIBLE) {
                habitacion.cambiarEstado(f.estado);
            }
        }
    }

    // ---------------------------------------------------------------- datos leídos de las tablas

    /** Fila de la tabla habitacion. */
    private static class FilaHabitacion {
        private int numero;
        private TipoHabitacion tipo;
        private int capacidad;
        private BigDecimal tarifaNoche;
        private EstadoHabitacion estado;
    }

    /** Fila de la tabla reserva. */
    private static class FilaReserva {
        private String codigo;
        private int clienteId;
        private int numeroHabitacion;
        private Integer empleadoId;
        private LocalDate entrada;
        private LocalDate salida;
        private BigDecimal tarifaNoche;
        private EstadoReserva estado;
    }

    /** Fila de la tabla pago. */
    private static class FilaPago {
        private BigDecimal monto;
        private MetodoPago metodo;
        private LocalDate fecha;
    }

    // ================================================================ conexión

    private void crearTablas() {
        Connection con = null;
        try {
            con = abrirConexion();
            try (Statement st = con.createStatement()) {
                for (String sql : CREAR_TABLAS) {
                    st.executeUpdate(sql);
                }
            }
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudieron crear las tablas: " + e.getMessage(), e);
        } finally {
            cerrar(con);
        }
    }

    private Connection abrirConexion() throws SQLException {
        return DriverManager.getConnection(url, USUARIO_BD, CLAVE_BD);
    }

    private void deshacer(Connection con) {
        if (con != null) {
            try {
                con.rollback();
            } catch (SQLException e) {
                // si el rollback también falla no hay más que hacer
            }
        }
    }

    private void cerrar(Connection con) {
        if (con != null) {
            try {
                con.close();
            } catch (SQLException e) {
                // al cerrar no hay nada más que hacer
            }
        }
    }
}
