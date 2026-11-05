package com.hotel.persistencia;

import com.hotel.modelo.Cargo;
import com.hotel.modelo.Empleado;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Empleado cuya contraseña se puede guardar en la base de datos.
 *
 * <p>La clase {@link Empleado} original esconde por completo el hash de la contraseña,
 * por lo que no hay forma de guardarlo ni de volver a crear al empleado sin conocer la
 * contraseña. Esta clase hereda de {@code Empleado} y se encarga ella misma de guardar
 * la sal y el hash (SHA-256) para poder entregarlos a la base de datos.</p>
 *
 * <p>Funcionamiento: el constructor de {@code Empleado} exige una contraseña, así que se le
 * pasa una clave interna fija ({@link #CLAVE_INTERNA}). Cuando alguien inicia sesión, esta
 * clase comprueba la contraseña real contra su propio hash y, si es correcta, llama a
 * {@code super.iniciarSesion} con la clave interna para que el empleado quede con la sesión
 * activa y pueda hacer check-in y check-out.</p>
 */
public class EmpleadoConClave extends Empleado {

    private static final String CLAVE_INTERNA = "clave-interna-no-usar";
    private static final int LONGITUD_MINIMA = 6;
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private final byte[] sal;
    private final byte[] hash;

    /**
     * Crea un empleado nuevo a partir de su contraseña en texto plano.
     *
     * @param id         identificador interno
     * @param nombre     nombre completo
     * @param documento  documento de identidad
     * @param telefono   teléfono (opcional)
     * @param email      correo (opcional)
     * @param cargo      cargo del empleado
     * @param usuario    nombre de usuario
     * @param contrasena contraseña (mínimo 6 caracteres)
     * @throws IllegalArgumentException si algún dato es inválido o la contraseña es muy corta
     */
    public EmpleadoConClave(int id, String nombre, String documento, String telefono, String email,
                            Cargo cargo, String usuario, String contrasena) {
        super(id, nombre, documento, telefono, email, cargo, usuario, CLAVE_INTERNA);
        if (contrasena == null || contrasena.length() < LONGITUD_MINIMA) {
            throw new IllegalArgumentException("La contraseña debe tener al menos "
                    + LONGITUD_MINIMA + " caracteres");
        }
        this.sal = new byte[16];
        ALEATORIO.nextBytes(this.sal);
        this.hash = calcularHash(contrasena, this.sal);
    }

    /** Constructor privado que usa {@link #restaurar} con la sal y el hash ya guardados. */
    private EmpleadoConClave(int id, String nombre, String documento, String telefono, String email,
                             Cargo cargo, String usuario, byte[] sal, byte[] hash) {
        super(id, nombre, documento, telefono, email, cargo, usuario, CLAVE_INTERNA);
        this.sal = sal;
        this.hash = hash;
    }

    /**
     * Reconstruye un empleado guardado en la base de datos, con su sal y su hash.
     *
     * @param id         identificador interno
     * @param nombre     nombre completo
     * @param documento  documento de identidad
     * @param telefono   teléfono (puede ser {@code null})
     * @param email      correo (puede ser {@code null})
     * @param cargo      cargo del empleado
     * @param usuario    nombre de usuario
     * @param salBase64  sal en Base64, tal como se guardó
     * @param hashBase64 hash en Base64, tal como se guardó
     * @return el empleado, con la sesión cerrada
     */
    public static EmpleadoConClave restaurar(int id, String nombre, String documento, String telefono,
                                             String email, Cargo cargo, String usuario,
                                             String salBase64, String hashBase64) {
        return new EmpleadoConClave(id, nombre, documento, telefono, email, cargo, usuario,
                Base64.getDecoder().decode(salBase64), Base64.getDecoder().decode(hashBase64));
    }

    /**
     * Inicia sesión comprobando la contraseña contra el hash guardado.
     *
     * @param usuario    nombre de usuario
     * @param contrasena contraseña en texto plano
     * @return {@code true} si las credenciales son correctas (la sesión queda activa);
     *         {@code false} en caso contrario (la sesión queda cerrada)
     */
    @Override
    public boolean iniciarSesion(String usuario, String contrasena) {
        boolean correcto = usuario != null && contrasena != null
                && getUsuario().equals(usuario)
                && MessageDigest.isEqual(hash, calcularHash(contrasena, sal));
        if (correcto) {
            return super.iniciarSesion(getUsuario(), CLAVE_INTERNA);
        }
        cerrarSesion();
        return false;
    }

    /**
     * Devuelve la sal de la contraseña en Base64, para guardarla en la base de datos.
     *
     * @return sal en Base64
     */
    public String getSalBase64() {
        return Base64.getEncoder().encodeToString(sal);
    }

    /**
     * Devuelve el hash de la contraseña en Base64, para guardarlo en la base de datos.
     * La contraseña original no se puede recuperar a partir de él.
     *
     * @return hash en Base64
     */
    public String getHashBase64() {
        return Base64.getEncoder().encodeToString(hash);
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
}
