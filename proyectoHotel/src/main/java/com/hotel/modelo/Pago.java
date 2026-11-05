package com.hotel.modelo;

import com.hotel.excepciones.HotelException;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Pago realizado sobre una {@link Reserva} (requisito RF6). Existe solo dentro de
 * su reserva (composición), por lo que únicamente {@link Reserva} puede crearlo.
 *
 * <p>La autorización con la pasarela o terminal de tarjetas (actor externo del
 * análisis) queda fuera de este modelo: {@link #procesar()} representa el momento
 * en que el pago se da por aceptado. Para integrarla bastaría con invocar la
 * pasarela dentro de ese método.</p>
 */
public class Pago {

    private final int id;
    private final Reserva reserva;
    private final BigDecimal monto;
    private final MetodoPago metodo;
    private final LocalDate fecha;
    private boolean procesado;

    /**
     * Crea un pago pendiente de procesar.
     *
     * @param id      número de pago dentro de la reserva
     * @param reserva reserva a la que pertenece
     * @param monto   monto pagado, mayor que cero
     * @param metodo  medio de pago
     * @param fecha   fecha del pago
     */
    Pago(int id, Reserva reserva, BigDecimal monto, MetodoPago metodo, LocalDate fecha) {
        this.id = id;
        this.reserva = Validaciones.noNulo(reserva, "La reserva");
        this.monto = Validaciones.positivo(monto, "El monto");
        this.metodo = Validaciones.noNulo(metodo, "El método de pago");
        this.fecha = Validaciones.noNulo(fecha, "La fecha del pago");
    }

    /**
     * Marca el pago como procesado (aceptado).
     *
     * @throws HotelException si el pago ya había sido procesado
     */
    public void procesar() {
        if (procesado) {
            throw new HotelException("El pago " + id + " ya fue procesado");
        }
        procesado = true;
    }

    /**
     * Genera el comprobante de pago en texto.
     *
     * @return comprobante con número, cliente, habitación, monto, método y fecha
     * @throws HotelException si el pago aún no ha sido procesado
     */
    public String generarComprobante() {
        if (!procesado) {
            throw new HotelException("No se puede emitir comprobante de un pago sin procesar");
        }
        return String.format(
                "COMPROBANTE DE PAGO %s%n"
                        + "Cliente: %s%n"
                        + "Habitación: %d%n"
                        + "Monto: %s%n"
                        + "Método: %s%n"
                        + "Fecha: %s",
                getNumeroComprobante(), reserva.getCliente().getNombre(),
                reserva.getHabitacion().getNumero(), monto.toPlainString(), metodo, fecha);
    }

    /**
     * Devuelve el número de comprobante, formado por el código de la reserva y el id del pago.
     *
     * @return número de comprobante, por ejemplo {@code RES-0001-P1}
     */
    public String getNumeroComprobante() {
        return reserva.getCodigo() + "-P" + id;
    }

    /**
     * Devuelve el número de pago dentro de la reserva.
     *
     * @return id del pago
     */
    public int getId() {
        return id;
    }

    /**
     * Devuelve el monto pagado.
     *
     * @return monto con dos decimales
     */
    public BigDecimal getMonto() {
        return monto;
    }

    /**
     * Devuelve el método de pago.
     *
     * @return método de pago
     */
    public MetodoPago getMetodo() {
        return metodo;
    }

    /**
     * Devuelve la fecha del pago.
     *
     * @return fecha
     */
    public LocalDate getFecha() {
        return fecha;
    }

    /**
     * Indica si el pago ya fue procesado.
     *
     * @return {@code true} si fue procesado
     */
    public boolean isProcesado() {
        return procesado;
    }
}
