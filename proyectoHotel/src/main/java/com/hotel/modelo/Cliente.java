package com.hotel.modelo;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Huésped o persona que reserva en el hotel (requisito RF1).
 * Mantiene el historial de sus reservas.
 */
public class Cliente extends Persona {

    /** Categoría que se asigna por defecto a un cliente nuevo. */
    public static final String CATEGORIA_INICIAL = "REGULAR";

    private final String nacionalidad;
    private String categoria = CATEGORIA_INICIAL;
    private final List<Reserva> reservas = new ArrayList<>();

    /**
     * Crea un cliente.
     *
     * @param id           identificador interno
     * @param nombre       nombre completo
     * @param documento    documento de identidad
     * @param telefono     teléfono (opcional)
     * @param email        correo (opcional)
     * @param nacionalidad nacionalidad (obligatoria)
     * @throws IllegalArgumentException si algún dato obligatorio es inválido
     */
    public Cliente(int id, String nombre, String documento, String telefono,
                   String email, String nacionalidad) {
        super(id, nombre, documento, telefono, email);
        this.nacionalidad = Validaciones.texto(nacionalidad, "La nacionalidad");
    }

    /**
     * Devuelve todas las reservas del cliente, de la más antigua a la más reciente.
     *
     * @return lista de solo lectura con el historial de reservas
     */
    public List<Reserva> consultarHistorial() {
        return Collections.unmodifiableList(reservas);
    }

    /**
     * Solicita una reserva (por recepción o en línea). La reserva queda
     * {@link EstadoReserva#PENDIENTE} y sin empleado asignado hasta que recepción
     * la confirme.
     *
     * @param hotel      hotel donde se reserva
     * @param habitacion habitación deseada
     * @param entrada    fecha de entrada
     * @param salida     fecha de salida
     * @return la reserva creada
     * @throws com.hotel.excepciones.HabitacionNoDisponibleException si la habitación
     *         está ocupada en ese rango
     */
    public Reserva solicitarReserva(Hotel hotel, Habitacion habitacion,
                                    LocalDate entrada, LocalDate salida) {
        Validaciones.noNulo(hotel, "El hotel");
        return hotel.crearReserva(this, habitacion, entrada, salida, null);
    }

    /**
     * Registra una reserva en el historial. Solo la invoca {@link Hotel}.
     *
     * @param reserva reserva a agregar
     */
    void agregarReserva(Reserva reserva) {
        reservas.add(reserva);
    }

    /**
     * Devuelve la nacionalidad.
     *
     * @return nacionalidad
     */
    public String getNacionalidad() {
        return nacionalidad;
    }

    /**
     * Devuelve la categoría comercial del cliente (por ejemplo REGULAR o VIP).
     *
     * @return categoría
     */
    public String getCategoria() {
        return categoria;
    }

    /**
     * Cambia la categoría comercial del cliente.
     *
     * @param categoria nueva categoría (obligatoria)
     * @throws IllegalArgumentException si está vacía
     */
    public void setCategoria(String categoria) {
        this.categoria = Validaciones.texto(categoria, "La categoría");
    }
}
