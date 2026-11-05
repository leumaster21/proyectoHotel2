package com.hotel.modelo;

import com.hotel.excepciones.HotelException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Reserva de una habitación por un cliente en un rango de fechas. Es el objeto central
 * del sistema: concentra las noches, los servicios adicionales, los pagos y el estado
 * del alojamiento (requisitos RF3 a RF6).
 *
 * <p>Los pagos son parte de la reserva (composición) y los servicios se agregan a ella
 * (agregación). La tarifa por noche se congela al crear la reserva.</p>
 */
public class Reserva {

    private final String codigo;
    private final Cliente cliente;
    private final Habitacion habitacion;
    private Empleado empleado;
    private final LocalDate fechaEntrada;
    private final LocalDate fechaSalida;
    private final BigDecimal tarifaNoche;
    private EstadoReserva estado = EstadoReserva.PENDIENTE;
    private BigDecimal total;
    private final List<ServicioAdicional> servicios = new ArrayList<>();
    private final List<Pago> pagos = new ArrayList<>();

    /**
     * Crea una reserva en estado {@link EstadoReserva#PENDIENTE}. No verifica la
     * disponibilidad; de eso se encarga {@link Hotel#crearReserva}.
     *
     * @param codigo       código único de la reserva
     * @param cliente      cliente que reserva
     * @param habitacion   habitación reservada
     * @param fechaEntrada fecha de entrada
     * @param fechaSalida  fecha de salida, posterior a la entrada
     * @param empleado     empleado que gestiona la reserva (puede ser {@code null}
     *                     si el cliente reservó en línea)
     * @throws IllegalArgumentException si algún dato obligatorio falta o las fechas son inválidas
     */
    Reserva(String codigo, Cliente cliente, Habitacion habitacion,
            LocalDate fechaEntrada, LocalDate fechaSalida, Empleado empleado) {
        this.codigo = Validaciones.texto(codigo, "El código");
        this.cliente = Validaciones.noNulo(cliente, "El cliente");
        this.habitacion = Validaciones.noNulo(habitacion, "La habitación");
        Validaciones.rangoFechas(fechaEntrada, fechaSalida);
        this.fechaEntrada = fechaEntrada;
        this.fechaSalida = fechaSalida;
        this.empleado = empleado;
        this.tarifaNoche = habitacion.getTarifaNoche();
        this.total = recalcularTotal();
    }

    /**
     * Calcula y actualiza el total a pagar: noches &times; tarifa congelada, más el
     * subtotal de cada servicio adicional.
     *
     * @return total de la reserva con dos decimales
     */
    public BigDecimal calcularTotal() {
        this.total = recalcularTotal();
        return total;
    }

    private BigDecimal recalcularTotal() {
        BigDecimal alojamiento = tarifaNoche.multiply(BigDecimal.valueOf(getNoches()));
        BigDecimal extras = servicios.stream()
                .map(ServicioAdicional::calcularSubtotal)
                .reduce(Montos.CERO, BigDecimal::add);
        return Montos.normalizar(alojamiento.add(extras));
    }

    /**
     * Confirma una reserva pendiente.
     *
     * @throws HotelException si la reserva no está {@link EstadoReserva#PENDIENTE}
     */
    public void confirmar() {
        exigirEstado("confirmar", EstadoReserva.PENDIENTE);
        estado = EstadoReserva.CONFIRMADA;
    }

    /**
     * Cancela la reserva y libera la habitación para esas fechas. Solo se puede cancelar
     * antes del check-in. Los pagos ya registrados se conservan: la política de
     * reembolsos queda fuera del alcance de este modelo.
     *
     * @throws HotelException si la reserva no está pendiente ni confirmada
     */
    public void cancelar() {
        exigirEstado("cancelar", EstadoReserva.PENDIENTE, EstadoReserva.CONFIRMADA);
        estado = EstadoReserva.CANCELADA;
    }

    /**
     * Agrega un servicio adicional a la cuenta y actualiza el total (requisito RF5).
     *
     * @param servicio servicio consumido
     * @throws HotelException si la reserva está cancelada o finalizada
     */
    public void agregarServicio(ServicioAdicional servicio) {
        Validaciones.noNulo(servicio, "El servicio");
        exigirEstado("agregar servicios", EstadoReserva.PENDIENTE,
                EstadoReserva.CONFIRMADA, EstadoReserva.EN_CURSO);
        servicios.add(servicio);
        calcularTotal();
    }

    /**
     * Registra un pago con la fecha de hoy.
     *
     * @param monto  monto pagado
     * @param metodo medio de pago
     * @return el pago procesado
     * @see #registrarPago(BigDecimal, MetodoPago, LocalDate)
     */
    public Pago registrarPago(BigDecimal monto, MetodoPago metodo) {
        return registrarPago(monto, metodo, LocalDate.now());
    }

    /**
     * Registra y procesa un pago contra la cuenta de la reserva (requisito RF6).
     *
     * @param monto  monto pagado, mayor que cero y sin superar el saldo pendiente
     * @param metodo medio de pago
     * @param fecha  fecha del pago
     * @return el pago procesado, del cual se puede obtener el comprobante
     * @throws IllegalArgumentException si el monto, el método o la fecha son inválidos
     * @throws HotelException           si la reserva está cancelada o el pago excede el saldo
     */
    public Pago registrarPago(BigDecimal monto, MetodoPago metodo, LocalDate fecha) {
        if (estado == EstadoReserva.CANCELADA) {
            throw new HotelException("No se pueden registrar pagos en una reserva cancelada");
        }
        Pago pago = new Pago(pagos.size() + 1, this, monto, metodo, fecha);
        BigDecimal saldo = getSaldoPendiente();
        if (pago.getMonto().compareTo(saldo) > 0) {
            throw new HotelException("El pago (" + pago.getMonto()
                    + ") excede el saldo pendiente (" + saldo + ")");
        }
        pago.procesar();
        pagos.add(pago);
        return pago;
    }

    /**
     * Suma de los pagos procesados.
     *
     * @return total pagado con dos decimales
     */
    public BigDecimal getTotalPagado() {
        return pagos.stream()
                .filter(Pago::isProcesado)
                .map(Pago::getMonto)
                .reduce(Montos.CERO, BigDecimal::add);
    }

    /**
     * Diferencia entre el total de la cuenta y lo ya pagado.
     *
     * @return saldo pendiente con dos decimales
     */
    public BigDecimal getSaldoPendiente() {
        return Montos.normalizar(total.subtract(getTotalPagado()));
    }

    /**
     * Inicia el alojamiento (check-in). Solo lo invoca {@link Empleado#registrarCheckIn}.
     * Si la reserva no tenía empleado asignado, queda a cargo de quien hace el check-in.
     *
     * @param quien empleado que registra el check-in
     * @throws HotelException si la reserva no está confirmada o la habitación no puede ocuparse
     */
    void iniciarEstadia(Empleado quien) {
        exigirEstado("hacer check-in", EstadoReserva.CONFIRMADA);
        habitacion.cambiarEstado(EstadoHabitacion.OCUPADA);
        estado = EstadoReserva.EN_CURSO;
        if (empleado == null) {
            empleado = quien;
        }
    }

    /**
     * Termina el alojamiento (check-out). Solo lo invoca {@link Empleado#registrarCheckOut}.
     *
     * @throws HotelException si la reserva no está en curso o aún tiene saldo pendiente
     */
    void finalizarEstadia() {
        exigirEstado("hacer check-out", EstadoReserva.EN_CURSO);
        BigDecimal saldo = getSaldoPendiente();
        if (saldo.signum() > 0) {
            throw new HotelException("No se puede hacer check-out: saldo pendiente de " + saldo);
        }
        habitacion.cambiarEstado(EstadoHabitacion.DISPONIBLE);
        estado = EstadoReserva.FINALIZADA;
    }

    /**
     * Indica si la reserva bloquea la habitación (pendiente, confirmada o en curso).
     *
     * @return {@code true} si la reserva está vigente
     */
    boolean estaActiva() {
        return estado == EstadoReserva.PENDIENTE
                || estado == EstadoReserva.CONFIRMADA
                || estado == EstadoReserva.EN_CURSO;
    }

    /**
     * Indica si el rango de la reserva se cruza con otro rango semiabierto
     * {@code [inicio, fin)}.
     *
     * @param inicio inicio del otro rango
     * @param fin    fin (exclusivo) del otro rango
     * @return {@code true} si comparten al menos una noche
     */
    boolean seSolapaCon(LocalDate inicio, LocalDate fin) {
        return fechaEntrada.isBefore(fin) && inicio.isBefore(fechaSalida);
    }

    /**
     * Cuenta las noches de esta reserva que caen dentro del rango {@code [inicio, fin)}.
     *
     * @param inicio inicio del rango
     * @param fin    fin (exclusivo) del rango
     * @return número de noches en común, nunca negativo
     */
    long nochesEnPeriodo(LocalDate inicio, LocalDate fin) {
        LocalDate desde = fechaEntrada.isAfter(inicio) ? fechaEntrada : inicio;
        LocalDate hasta = fechaSalida.isBefore(fin) ? fechaSalida : fin;
        return Math.max(0, ChronoUnit.DAYS.between(desde, hasta));
    }

    private void exigirEstado(String accion, EstadoReserva... permitidos) {
        for (EstadoReserva permitido : permitidos) {
            if (estado == permitido) {
                return;
            }
        }
        throw new HotelException("No se puede " + accion + " una reserva en estado " + estado);
    }

    /**
     * Devuelve el número de noches de la estadía.
     *
     * @return noches entre la entrada y la salida
     */
    public int getNoches() {
        return (int) ChronoUnit.DAYS.between(fechaEntrada, fechaSalida);
    }

    /**
     * Devuelve el código único de la reserva.
     *
     * @return código, por ejemplo {@code RES-0001}
     */
    public String getCodigo() {
        return codigo;
    }

    /**
     * Devuelve el cliente titular.
     *
     * @return cliente
     */
    public Cliente getCliente() {
        return cliente;
    }

    /**
     * Devuelve la habitación reservada.
     *
     * @return habitación
     */
    public Habitacion getHabitacion() {
        return habitacion;
    }

    /**
     * Devuelve el empleado que gestiona la reserva.
     *
     * @return empleado, o {@code null} si todavía no tiene uno asignado
     */
    public Empleado getEmpleado() {
        return empleado;
    }

    /**
     * Devuelve la fecha de entrada.
     *
     * @return fecha de entrada
     */
    public LocalDate getFechaEntrada() {
        return fechaEntrada;
    }

    /**
     * Devuelve la fecha de salida.
     *
     * @return fecha de salida
     */
    public LocalDate getFechaSalida() {
        return fechaSalida;
    }

    /**
     * Devuelve la tarifa por noche con la que se hizo la reserva.
     *
     * @return tarifa congelada
     */
    public BigDecimal getTarifaNoche() {
        return tarifaNoche;
    }

    /**
     * Devuelve el estado actual de la reserva.
     *
     * @return estado
     */
    public EstadoReserva getEstado() {
        return estado;
    }

    /**
     * Devuelve el total a pagar calculado hasta el momento.
     *
     * @return total con dos decimales
     */
    public BigDecimal getTotal() {
        return total;
    }

    /**
     * Devuelve los servicios adicionales consumidos.
     *
     * @return lista de solo lectura
     */
    public List<ServicioAdicional> getServicios() {
        return Collections.unmodifiableList(servicios);
    }

    /**
     * Devuelve los pagos registrados.
     *
     * @return lista de solo lectura
     */
    public List<Pago> getPagos() {
        return Collections.unmodifiableList(pagos);
    }
}
