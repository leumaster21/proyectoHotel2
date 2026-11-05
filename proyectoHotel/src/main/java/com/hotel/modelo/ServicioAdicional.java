package com.hotel.modelo;

import java.math.BigDecimal;

/**
 * Servicio extra que se cobra dentro de una reserva: desayuno, lavandería, spa, etc.
 * (requisito RF5). Cada instancia representa un renglón de consumo con su cantidad.
 */
public class ServicioAdicional {

    private final int id;
    private final String nombre;
    private final BigDecimal precio;
    private final int cantidad;

    /**
     * Crea un servicio adicional.
     *
     * @param id       identificador del servicio
     * @param nombre   nombre del servicio (obligatorio)
     * @param precio   precio unitario, mayor que cero
     * @param cantidad unidades consumidas, al menos 1
     * @throws IllegalArgumentException si el nombre está vacío, el precio no es positivo
     *                                  o la cantidad es menor que 1
     */
    public ServicioAdicional(int id, String nombre, BigDecimal precio, int cantidad) {
        this.id = id;
        this.nombre = Validaciones.texto(nombre, "El nombre del servicio");
        this.precio = Validaciones.positivo(precio, "El precio");
        if (cantidad < 1) {
            throw new IllegalArgumentException("La cantidad debe ser al menos 1");
        }
        this.cantidad = cantidad;
    }

    /**
     * Calcula el subtotal del renglón.
     *
     * @return precio unitario multiplicado por la cantidad, con dos decimales
     */
    public BigDecimal calcularSubtotal() {
        return Montos.normalizar(precio.multiply(BigDecimal.valueOf(cantidad)));
    }

    /**
     * Devuelve el identificador.
     *
     * @return id del servicio
     */
    public int getId() {
        return id;
    }

    /**
     * Devuelve el nombre del servicio.
     *
     * @return nombre
     */
    public String getNombre() {
        return nombre;
    }

    /**
     * Devuelve el precio unitario.
     *
     * @return precio por unidad
     */
    public BigDecimal getPrecio() {
        return precio;
    }

    /**
     * Devuelve la cantidad consumida.
     *
     * @return unidades
     */
    public int getCantidad() {
        return cantidad;
    }
}
