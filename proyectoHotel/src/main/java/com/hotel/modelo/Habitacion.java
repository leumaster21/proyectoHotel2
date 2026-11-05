package com.hotel.modelo;

import com.hotel.excepciones.HabitacionNoDisponibleException;
import com.hotel.excepciones.HotelException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Habitación del hotel (requisito RF2). Sabe qué reservas la ocupan, lo que le permite
 * responder por sí misma si está libre en un rango de fechas.
 *
 * <p>Las habitaciones pertenecen al {@link Hotel} (composición), por eso solo
 * se crean mediante {@link Hotel#agregarHabitacion}.</p>
 */
public class Habitacion {

    private final int numero;
    private final TipoHabitacion tipo;
    private final int capacidad;
    private BigDecimal tarifaNoche;
    private EstadoHabitacion estado = EstadoHabitacion.DISPONIBLE;
    private final List<Reserva> reservas = new ArrayList<>();

    /**
     * Crea una habitación en estado {@link EstadoHabitacion#DISPONIBLE}.
     *
     * @param numero      número de la habitación (mayor que cero)
     * @param tipo        tipo de habitación
     * @param capacidad   número máximo de huéspedes (al menos 1)
     * @param tarifaNoche precio por noche, mayor que cero
     * @throws IllegalArgumentException si algún dato es inválido
     */
    Habitacion(int numero, TipoHabitacion tipo, int capacidad, BigDecimal tarifaNoche) {
        if (numero <= 0) {
            throw new IllegalArgumentException("El número de habitación debe ser mayor que cero");
        }
        if (capacidad < 1) {
            throw new IllegalArgumentException("La capacidad debe ser al menos 1");
        }
        this.numero = numero;
        this.tipo = Validaciones.noNulo(tipo, "El tipo de habitación");
        this.capacidad = capacidad;
        this.tarifaNoche = Validaciones.positivo(tarifaNoche, "La tarifa por noche");
    }

    /**
     * Indica si la habitación puede reservarse en el rango dado.
     *
     * <p>Regla: el rango es <em>semiabierto</em> {@code [entrada, salida)}. La noche de
     * salida queda libre, de modo que un huésped puede salir el mismo día en que otro entra.
     * Las reservas {@link EstadoReserva#CANCELADA} y {@link EstadoReserva#FINALIZADA} no
     * bloquean fechas. Una habitación en mantenimiento nunca está disponible.</p>
     *
     * @param inicio fecha de entrada
     * @param fin    fecha de salida (posterior a la entrada)
     * @return {@code true} si está libre en todo el rango
     * @throws IllegalArgumentException si las fechas son nulas o el rango es inválido
     */
    public boolean estaDisponible(LocalDate inicio, LocalDate fin) {
        Validaciones.rangoFechas(inicio, fin);
        if (estado == EstadoHabitacion.MANTENIMIENTO) {
            return false;
        }
        return reservas.stream().noneMatch(r -> r.estaActiva() && r.seSolapaCon(inicio, fin));
    }

    /**
     * Cambia el estado de la habitación respetando las transiciones permitidas:
     * DISPONIBLE &harr; OCUPADA y DISPONIBLE &harr; MANTENIMIENTO. No se puede pasar
     * directamente entre OCUPADA y MANTENIMIENTO ni repetir el estado actual.
     *
     * @param nuevoEstado estado al que se quiere pasar
     * @throws HotelException si la transición no está permitida
     */
    public void cambiarEstado(EstadoHabitacion nuevoEstado) {
        Validaciones.noNulo(nuevoEstado, "El nuevo estado");
        boolean permitida = (estado == EstadoHabitacion.DISPONIBLE && nuevoEstado != EstadoHabitacion.DISPONIBLE)
                || (estado != EstadoHabitacion.DISPONIBLE && nuevoEstado == EstadoHabitacion.DISPONIBLE);
        if (!permitida) {
            throw new HotelException("Transición de estado no permitida en la habitación "
                    + numero + ": " + estado + " -> " + nuevoEstado);
        }
        this.estado = nuevoEstado;
    }

    /**
     * Registra una reserva sobre esta habitación. Comprueba de nuevo la disponibilidad
     * como defensa contra la sobreventa.
     *
     * @param reserva reserva a registrar
     * @throws HabitacionNoDisponibleException si ya existe una reserva activa que se solapa
     */
    void registrarReserva(Reserva reserva) {
        if (!estaDisponible(reserva.getFechaEntrada(), reserva.getFechaSalida())) {
            throw new HabitacionNoDisponibleException(numero, reserva.getFechaEntrada(),
                    reserva.getFechaSalida());
        }
        reservas.add(reserva);
    }

    /**
     * Devuelve las reservas asociadas a la habitación (todas las del historial).
     *
     * @return lista de solo lectura
     */
    public List<Reserva> getReservas() {
        return Collections.unmodifiableList(reservas);
    }

    /**
     * Devuelve el número de la habitación.
     *
     * @return número
     */
    public int getNumero() {
        return numero;
    }

    /**
     * Devuelve el tipo de habitación.
     *
     * @return tipo
     */
    public TipoHabitacion getTipo() {
        return tipo;
    }

    /**
     * Devuelve la capacidad máxima de huéspedes.
     *
     * @return capacidad
     */
    public int getCapacidad() {
        return capacidad;
    }

    /**
     * Devuelve la tarifa vigente por noche.
     *
     * @return tarifa con dos decimales
     */
    public BigDecimal getTarifaNoche() {
        return tarifaNoche;
    }

    /**
     * Cambia la tarifa por noche (operación del administrador). Las reservas ya creadas
     * conservan la tarifa con la que se hicieron.
     *
     * @param tarifaNoche nueva tarifa, mayor que cero
     * @throws IllegalArgumentException si la tarifa no es positiva
     */
    public void setTarifaNoche(BigDecimal tarifaNoche) {
        this.tarifaNoche = Validaciones.positivo(tarifaNoche, "La tarifa por noche");
    }

    /**
     * Devuelve el estado actual.
     *
     * @return estado de la habitación
     */
    public EstadoHabitacion getEstado() {
        return estado;
    }
}
