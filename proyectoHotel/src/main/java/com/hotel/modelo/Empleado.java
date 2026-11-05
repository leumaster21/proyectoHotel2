package com.hotel.modelo;

import com.hotel.excepciones.AccesoDenegadoException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * Empleado del hotel. El {@link Cargo} distingue al recepcionista del administrador
 * (en lugar de crear una subclase por cada rol).
 *
 * <p>La contraseña nunca se guarda en texto plano: se almacena su hash SHA-256 con
 * una sal aleatoria (requisito RNF2). En un sistema real se recomienda un algoritmo
 * de derivación de claves como bcrypt o Argon2.</p>
 *
 * <p><strong>Nota:</strong> el diagrama nombra el atributo {@code contraseña};
 * en el código se escribe {@code contrasena} para evitar caracteres no ASCII
 * en los identificadores.</p>
 */
public class Empleado extends Persona {

    private static final int LONGITUD_MINIMA_CONTRASENA = 6;
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private final Cargo cargo;
    private final String usuario;
    private final byte[] sal;
    private final byte[] hashContrasena;
    private boolean sesionActiva;

    /**
     * Crea un empleado con sus credenciales.
     *
     * @param id         identificador interno
     * @param nombre     nombre completo
     * @param documento  documento de identidad
     * @param telefono   teléfono (opcional)
     * @param email      correo (opcional)
     * @param cargo      cargo o rol del empleado
     * @param usuario    nombre de usuario para iniciar sesión
     * @param contrasena contraseña en texto plano (mínimo 6 caracteres)
     * @throws IllegalArgumentException si algún dato es inválido o la contraseña es muy corta
     */
    public Empleado(int id, String nombre, String documento, String telefono, String email,
                    Cargo cargo, String usuario, String contrasena) {
        super(id, nombre, documento, telefono, email);
        this.cargo = Validaciones.noNulo(cargo, "El cargo");
        this.usuario = Validaciones.texto(usuario, "El usuario");
        Validaciones.texto(contrasena, "La contraseña");
        if (contrasena.length() < LONGITUD_MINIMA_CONTRASENA) {
            throw new IllegalArgumentException("La contraseña debe tener al menos "
                    + LONGITUD_MINIMA_CONTRASENA + " caracteres");
        }
        this.sal = new byte[16];
        ALEATORIO.nextBytes(this.sal);
        this.hashContrasena = calcularHash(contrasena, this.sal);
    }

    /**
     * Intenta iniciar sesión con las credenciales dadas.
     *
     * @param usuario    nombre de usuario
     * @param contrasena contraseña en texto plano
     * @return {@code true} si las credenciales son correctas (la sesión queda activa);
     *         {@code false} en caso contrario (la sesión queda cerrada)
     */
    public boolean iniciarSesion(String usuario, String contrasena) {
        boolean correcto = usuario != null && contrasena != null
                && this.usuario.equals(usuario)
                && MessageDigest.isEqual(hashContrasena, calcularHash(contrasena, sal));
        this.sesionActiva = correcto;
        return correcto;
    }

    /** Cierra la sesión del empleado. */
    public void cerrarSesion() {
        this.sesionActiva = false;
    }

    /**
     * Registra el check-in de una reserva: la reserva pasa a {@link EstadoReserva#EN_CURSO}
     * y la habitación a {@link EstadoHabitacion#OCUPADA} (requisito RF4).
     *
     * @param reserva reserva confirmada del huésped que llega
     * @throws AccesoDenegadoException si no hay sesión activa o el cargo no es recepcionista
     * @throws com.hotel.excepciones.HotelException si la reserva no está confirmada
     */
    public void registrarCheckIn(Reserva reserva) {
        exigirRecepcionista();
        Validaciones.noNulo(reserva, "La reserva");
        reserva.iniciarEstadia(this);
    }

    /**
     * Registra el check-out de una reserva: exige la cuenta saldada, la reserva pasa a
     * {@link EstadoReserva#FINALIZADA} y la habitación vuelve a
     * {@link EstadoHabitacion#DISPONIBLE} (requisito RF4).
     *
     * @param reserva reserva en curso del huésped que se va
     * @throws AccesoDenegadoException si no hay sesión activa o el cargo no es recepcionista
     * @throws com.hotel.excepciones.HotelException si la reserva no está en curso o tiene saldo pendiente
     */
    public void registrarCheckOut(Reserva reserva) {
        exigirRecepcionista();
        Validaciones.noNulo(reserva, "La reserva");
        reserva.finalizarEstadia();
    }

    private void exigirRecepcionista() {
        if (!sesionActiva) {
            throw new AccesoDenegadoException("Debe iniciar sesión para realizar esta operación");
        }
        if (cargo != Cargo.RECEPCIONISTA) {
            throw new AccesoDenegadoException("Solo un recepcionista puede registrar check-in y check-out");
        }
    }

    private static byte[] calcularHash(String contrasena, byte[] sal) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(sal);
            return digest.digest(contrasena.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no está disponible en esta JVM", e);
        }
    }

    /**
     * Devuelve el cargo del empleado.
     *
     * @return cargo
     */
    public Cargo getCargo() {
        return cargo;
    }

    /**
     * Devuelve el nombre de usuario.
     *
     * @return usuario
     */
    public String getUsuario() {
        return usuario;
    }

    /**
     * Indica si el empleado tiene una sesión activa.
     *
     * @return {@code true} si inició sesión y no la ha cerrado
     */
    public boolean isSesionActiva() {
        return sesionActiva;
    }
}
