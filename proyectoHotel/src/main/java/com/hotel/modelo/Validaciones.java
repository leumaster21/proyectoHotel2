package com.hotel.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Validaciones de argumentos compartidas por las clases del modelo.
 * Lanzan {@link IllegalArgumentException} cuando el dato es inválido.
 */
final class Validaciones {

    private Validaciones() {
    }

    /**
     * Verifica que un texto no sea nulo ni vacío.
     *
     * @param valor texto a validar
     * @param campo nombre del campo, usado en el mensaje de error
     * @return el texto sin espacios sobrantes
     */
    static String texto(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(campo + " es obligatorio");
        }
        return valor.trim();
    }

    /**
     * Verifica que un objeto no sea nulo.
     *
     * @param <T>   tipo del objeto
     * @param valor objeto a validar
     * @param campo nombre del campo, usado en el mensaje de error
     * @return el mismo objeto
     */
    static <T> T noNulo(T valor, String campo) {
        if (valor == null) {
            throw new IllegalArgumentException(campo + " es obligatorio");
        }
        return valor;
    }

    /**
     * Verifica que un monto sea estrictamente positivo y lo normaliza a dos decimales.
     *
     * @param valor monto a validar
     * @param campo nombre del campo, usado en el mensaje de error
     * @return el monto con escala 2
     */
    static BigDecimal positivo(BigDecimal valor, String campo) {
        noNulo(valor, campo);
        if (valor.signum() <= 0) {
            throw new IllegalArgumentException(campo + " debe ser mayor que cero");
        }
        return Montos.normalizar(valor);
    }

    /**
     * Verifica que ambas fechas existan y que la salida sea posterior a la entrada.
     *
     * @param entrada fecha de entrada
     * @param salida  fecha de salida
     */
    static void rangoFechas(LocalDate entrada, LocalDate salida) {
        noNulo(entrada, "La fecha de entrada");
        noNulo(salida, "La fecha de salida");
        if (!salida.isAfter(entrada)) {
            throw new IllegalArgumentException("La fecha de salida debe ser posterior a la de entrada");
        }
    }
}
