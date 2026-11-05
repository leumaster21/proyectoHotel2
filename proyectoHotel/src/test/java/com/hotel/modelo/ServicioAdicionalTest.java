package com.hotel.modelo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Pruebas de {@link ServicioAdicional}. */
class ServicioAdicionalTest {

    @Test
    @DisplayName("El subtotal es precio x cantidad")
    void subtotal() {
        ServicioAdicional desayuno = new ServicioAdicional(1, "Desayuno", new BigDecimal("12.50"), 4);

        assertEquals(new BigDecimal("50.00"), desayuno.calcularSubtotal());
    }

    @Test
    @DisplayName("Rechaza datos inválidos")
    void validaciones() {
        assertThrows(IllegalArgumentException.class,
                () -> new ServicioAdicional(1, " ", BigDecimal.TEN, 1));
        assertThrows(IllegalArgumentException.class,
                () -> new ServicioAdicional(1, "Spa", BigDecimal.ZERO, 1));
        assertThrows(IllegalArgumentException.class,
                () -> new ServicioAdicional(1, "Spa", null, 1));
        assertThrows(IllegalArgumentException.class,
                () -> new ServicioAdicional(1, "Spa", BigDecimal.TEN, 0));
    }
}
